/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.mta.comparison.testgen;

import flexjson.JSONDeserializer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class RunODPairsWithOTP {
	
    private String PAIRS_TXT = "src/test/resources/mta/comparison/baseline_ods.txt";

    private String OTP_RESULTS_TXT = "src/test/resources/mta/comparison/baseline.txt";

    private String OTP_URL = "http://otp-mta-dev.camsys-apps.com/otp/routers/default/plan?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp";

    private boolean USE_CURRENT_TIME = false;
    
    public void setOTPURL(String u) {
    	this.OTP_URL = u;
    }

    public void setInputFile(String f) {
    	this.PAIRS_TXT = f;
    }

    public void setOutputFile(String f) {
    	this.OTP_RESULTS_TXT = f;
    }
    
    public void setUseCurrentTime(boolean f) {
    	this.USE_CURRENT_TIME = f;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void run() throws IOException, InterruptedException, URISyntaxException {

    	FileWriter otpResults = new FileWriter(OTP_RESULTS_TXT);

    	File odPairs = new File(PAIRS_TXT);
    	Scanner reader = new Scanner(odPairs);
            	
        CloseableHttpClient httpClient = HttpClients.createDefault();
        
    	System.out.print("Running pairs through OTP ...");

    	while (reader.hasNextLine()) {
    		
    		// get test params from OD pairs file
    		String line = reader.nextLine();
    	
    		boolean accessible = line.split(" ")[1].trim().equals("Y");
    		
    		long epoch = Long.parseLong(line.split(" ")[2].trim());

    		// this is a misnomer--it's actually more this week, adjusting by week so we try to match the 
    		// same service period
    		if(USE_CURRENT_TIME) {
    			Calendar c = Calendar.getInstance();
				c.setTime(new Date(epoch));
    			
    			while(epoch < DateTime.now().getMillis()) {
    				c.add(Calendar.DAY_OF_MONTH, 7);    				
        			epoch = c.getTimeInMillis();
    			}
    		}

    		
    		String stop1 = line.split(" ")[3].trim();
    		String stop2 = line.split(" ")[4].trim();
    		
    		String optimizeFlag = line.split(" ")[5].trim();

    		String originLat = stop1.split(",")[0].trim();
    		String originLon = stop1.split(",")[1].trim();
    	
    		String destLat = stop2.split(",")[0].trim();
    		String destLon = stop2.split(",")[1].trim();
    		    		
    		// Make request of OTP
    		URIBuilder builder = new URIBuilder(OTP_URL);
    		builder.setParameter("fromPlace", originLat + "," + originLon);
    		builder.setParameter("toPlace", destLat + "," + destLon);
    		builder.setParameter("wheelchair", accessible + "");
    		
    		DateTimeFormatter dateF = DateTimeFormat.forPattern("MM-dd-YYYY");
    		DateTimeFormatter timeF = DateTimeFormat.forPattern("hh:mm aa");
    		builder.setParameter("date", new DateTime(epoch).toString(dateF));
    		builder.setParameter("time", new DateTime(epoch).toString(timeF));

    		builder.setParameter("mode", "TRANSIT,WALK");
    		builder.setParameter("maxWalkDistance", "500");
    		builder.setParameter("ignoreRealtimeUpdates", "true");
    		switch(optimizeFlag) {
    			case "W":
    				builder.setParameter("optimize",  "WALKING");
    				break;
    			case "X":
    				builder.setParameter("optimize",  "TRANSFERS");
    				break;
    			case "T":
    				builder.setParameter("optimize",  "QUICK");
    				break;
    		}
    		

    		HttpGet get = new HttpGet(builder.build());

            // Read response from OTP
            CloseableHttpResponse response = httpClient.execute(get);
            String responseString = EntityUtils.toString(response.getEntity());

        	// write the OD input line back to the results so we can compare
        	otpResults.write(line + "\n");

        	HashMap<String, Object> root = null;
            try {
            	HashMap<String, Map> planResponse = (HashMap<String, Map>)new JSONDeserializer().deserialize(responseString);             
            	root = (HashMap<String, Object>)planResponse.get("plan");            
            } catch (Exception e) {
            	root = null;
            }

        	if(root == null) {
        		otpResults.write("\n***** FAILED *****\n");
        		otpResults.write("***** FAILED *****\n");
        		otpResults.write("***** FAILED *****\n\n");
        	
        		otpResults.write("D " + responseString.replace("\n",  "").replace("\r", "").replace("\t",  "") + "\n\n");

        		continue;
        	}

            ArrayList<Map> itineraries = (ArrayList<Map>) root.get("itineraries");
            
            for(int itin_i = 0; itin_i < itineraries.size(); itin_i++) {
            	System.out.print(".");

                HashMap<String, Object> itinerary = (HashMap<String, Object>) itineraries.get(itin_i);
                ArrayList<Map>  legs = (ArrayList<Map>) itinerary.get("legs");

                otpResults.write(
               		 (itin_i + 1) + " WALK DISTANCE=" + String.format("%.2f",(Double)itinerary.get("walkDistance")/1000) + " km "
               		 		+ "TRANIST TIME=" + ((Integer)itinerary.get("transitTime")/60) + " min \n");

                otpResults.write(
                  		 (itin_i + 1) + " ---------------------------\n");
           	 
                String summaryString = new String();
                for(int leg_i = 0; leg_i < legs.size(); leg_i++) {
                	 HashMap<String, Object> leg = (HashMap<String, Object>) legs.get(leg_i);
                	 HashMap<String, Object> onStop = (HashMap<String, Object>) leg.get("from");
                	 HashMap<String, Object> offStop = (HashMap<String, Object>) leg.get("to");
                	 String mode = (String) leg.get("mode");

                	 System.out.print(".");

                	 if(mode.equals("WALK"))
                		 continue;                	 
                	 
                	 if(summaryString.length() > 0)
                		 summaryString += ">";                	 
                	 summaryString += ((String) leg.get("route"));
                	 
                     otpResults.write(
                    		 (itin_i + 1) + " " + ((String)onStop.get("name")).replace("[", "(").replace("]", ")") + " -> " + 
                    		 ((String) leg.get("route")) + " to " + leg.get("tripHeadsign") + " -> " + 
                    		 ((String)offStop.get("name")).replace("[", "(").replace("]", ")") + "\n");
                }
                
                otpResults.write(
                 		 "S " + (itin_i + 1) + " " + String.format("%.2f",(Double)itinerary.get("walkDistance")/1000)
                 		 		+ " " + ((Integer)itinerary.get("transitTime")/60) + " " + summaryString + "\n");

                otpResults.write("\n");
            }            
           
//            otpResults.write("D " + responseString.replace("\n",  "").replace("\r", "").replace("\t",  "") + "\n\n");
       	}
    	
    	System.out.println("done.");

    	reader.close();
    	otpResults.close();
    }
}
