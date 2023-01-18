///* This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation, either version 3 of
// the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>. */
//package org.opentripplanner.routing.mta.comparison.testgen;
//
//import com.google.common.net.HttpHeaders;
//import com.thoughtworks.xstream.XStream;
//import com.thoughtworks.xstream.converters.Converter;
//import com.thoughtworks.xstream.converters.MarshallingContext;
//import com.thoughtworks.xstream.converters.UnmarshallingContext;
//import com.thoughtworks.xstream.io.HierarchicalStreamReader;
//import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.AbstractMap;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Scanner;
//
//public class RunODPairsWithATIS {
//
//    private static final String PAIRS_TXT = "src/test/resources/mta/comparison/baseline_ods.txt";
//
//    private String ATIS_RESULTS_TXT = "src/test/resources/mta/comparison/baseline.txt";
//
//    private static final String ATIS_URL = "https://dataservice.mta.info/cgi-bin-soap-2.10.1/soap.cgi";
//
//    private static final String ATIS_KEY = "<<YOUR KEY HERE>>";
//
//    public class MapEntryConverter implements Converter{
//
//    	private HashMap<String, Integer> keyCounter = new HashMap<String, Integer>();
//
//        public boolean canConvert(Class clazz) {
//            return AbstractMap.class.isAssignableFrom(clazz);
//        }
//
//        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
//            @SuppressWarnings("unchecked")
//			AbstractMap<String, Object> map = (AbstractMap<String, Object>) value;
//            for(String k : map.keySet()) {
//            	Object v = map.get(k);
//
//                writer.startNode(k);
//
//                if(v instanceof String) {
//                    writer.setValue(v.toString());
//            	} else if(v instanceof Map) {
//            		marshal(v, writer, context);
//            	}
//
//                writer.endNode();
//            }
//        }
//
//        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
//        	AbstractMap<String, Object> r = new HashMap<String, Object>();
//
//        	while(reader.hasMoreChildren()) {
//                reader.moveDown();
//
//                String k = reader.getNodeName();
//
//                if(reader.hasMoreChildren()) {
//                	if(r.containsKey(k)) {
//                		r.put(k + "0",  r.get(k));
//                		r.remove(k);
//
//            			String newKey = k + (keyCounter.get(k) + 1);
//            			r.put(newKey, unmarshal(reader, context));
//                		keyCounter.put(k, keyCounter.get(k) + 1);
//                	} else {
//                		keyCounter.put(k, new Integer(0));
//                		r.put(k, unmarshal(reader, context));
//                	}
//                } else {
//                    Object v = reader.getValue();
//                    r.put(k, v);
//                }
//
//                reader.moveUp();
//            }
//
//            return r;
//        }
//    }
//
//    public void setOutputFile(String f) {
//    	this.ATIS_RESULTS_TXT = f;
//    }
//
//    @SuppressWarnings("unchecked")
//    public void run() throws IOException, InterruptedException {
//
//    	FileWriter atisResults = new FileWriter(ATIS_RESULTS_TXT);
//
//    	File odPairs = new File(PAIRS_TXT);
//    	Scanner reader = new Scanner(odPairs);
//
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//
//    	System.out.print("Running pairs through ATIS ...");
//
//    	while (reader.hasNextLine()) {
//
//    		// get test params from OD pairs file
//    		String line = reader.nextLine();
//
//    		boolean accessible = line.split(" ")[1].trim().equals("Y");
//
//    		long epoch = Long.parseLong(line.split(" ")[2].trim());
//
//    		String stop1 = line.split(" ")[3].trim();
//    		String stop2 = line.split(" ")[4].trim();
//
//    		String optimizeFlag = line.split(" ")[5].trim();
//
//    		String originLat = stop1.split(",")[0].trim();
//    		String originLon = stop1.split(",")[1].trim();
//
//    		String destLat = stop2.split(",")[0].trim();
//    		String destLon = stop2.split(",")[1].trim();
//
//    		// Make request of ATIS
//            Map<String,String> planTrip = new HashMap<String,String>();
//
//            planTrip.put("Originlat", originLat);
//            planTrip.put("Originlong", originLon);
//            planTrip.put("Destinationlat", destLat);
//            planTrip.put("Destinationlong", destLon);
//            planTrip.put("Accessible", ((accessible) ? "Y" : "N"));
//            planTrip.put("Appid", ATIS_KEY);
//            planTrip.put("Maxanswers", "3"); // labelled max answers, but actually is the number you want back
//            planTrip.put("Date", new SimpleDateFormat("MM/dd/YY").format(epoch));
//            planTrip.put("Time", new SimpleDateFormat("HHmm").format(epoch));
//            planTrip.put("Walkdist", ".31"); // in miles, = .5 KM
//            planTrip.put("Minimize", optimizeFlag); // T = Time, X = Transfers, W = Walking. The most important factor when ranking the trips returned.
//
//
//            // Trapeze doesn't give you WSDL to do this programmatically like it should, so hacking this a bit here (FIXME)
//    		XStream magicApi = new XStream();
//            XStream.setupDefaultSecurity(magicApi);
//    		magicApi.registerConverter(new MapEntryConverter());
//    		magicApi.alias("namesp1:Plantrip", Map.class);
//
//    		String requestXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
//            		"<SOAP-ENV:Envelope xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
//            		+ "<SOAP-ENV:Body>\n";
//            requestXML += magicApi.toXML(planTrip).replace("<namesp1:Plantrip", "<namesp1:Plantrip xmlns:namesp1=\"NY_SOAP2\"");
//            requestXML += "</SOAP-ENV:Body></SOAP-ENV:Envelope>";
//
//            HttpPost post = new HttpPost(ATIS_URL);
//            post.setHeader(HttpHeaders.CONTENT_TYPE, "text/xml");
//            post.setEntity(new StringEntity(requestXML));
//
//
//            // Read response from ATIS
//            CloseableHttpResponse response = httpClient.execute(post);
//            String responseString = EntityUtils.toString(response.getEntity());
//
//            magicApi = new XStream();
//            XStream.setupDefaultSecurity(magicApi);
//    		magicApi.registerConverter(new MapEntryConverter());
//    		magicApi.alias("namesp1:Plantrip", Map.class);
//    		magicApi.alias("soap:Envelope", Map.class);
//
//            Map<String, Object>extractedMap = (Map<String, Object>) magicApi.fromXML(responseString);
//            HashMap<String, Object> root = (HashMap<String, Object>)extractedMap.get("soap:Body");
//            HashMap<String, Object> tripResponse = (HashMap<String, Object>) root.get("PlantripResponse");
//
//            // write the OD input line back to the results so we can compare
//            atisResults.write(line + "\n");
//
//            if(tripResponse == null) {
//            	atisResults.write("\n***** FAILED *****\n");
//            	atisResults.write("***** FAILED *****\n");
//            	atisResults.write("***** FAILED *****\n\n");
//
//            	atisResults.write("D " + responseString.replace("\n",  "").replace("\r", "").replace("\t",  "") + "\n\n");
//
//            	continue;
//            }
//
//            int itin_i = 1;
//            for(String k : tripResponse.keySet()) {
//            	System.out.print(".");
//
//            	if(!k.startsWith("Itin"))
//            		continue;
//
//                HashMap<String, Object> itinerary = (HashMap<String, Object>) tripResponse.get(k);
//                HashMap<String, Object> legs = (HashMap<String, Object>) itinerary.get("Legs");
//
//                atisResults.write(
//                  		 (itin_i) + " WALK DISTANCE=" + String.format("%.2f",Double.parseDouble((String)itinerary.get("Totalwalk")) * 1.609) + " km "
//                  		 		+ "TRANSIT TIME=" + Integer.parseInt((String)itinerary.get("Transittime")) + " min \n");
//
//                atisResults.write(
//                     		 (itin_i) + " ---------------------------\n");
//
//                String summaryString = new String();
//                for(String k2 : legs.keySet()) {
//                	 HashMap<String, Object> leg = (HashMap<String, Object>) legs.get(k2);
//                	 HashMap<String, Object> onStop = (HashMap<String, Object>) leg.get("Onstopdata");
//                	 HashMap<String, Object> offStop = (HashMap<String, Object>) leg.get("Offstopdata");
//                	 HashMap<String, Object> service = (HashMap<String, Object>) leg.get("Service");
//
//                	 System.out.print(".");
//
//                	 if(summaryString.length() > 0)
//                		 summaryString += ">";
//                	 summaryString += service.get("Route");
//
//                     atisResults.write(
//                    		 itin_i + " " + ((String)onStop.get("Description")).replace("[", "(").replace("]", ")") + " -> " +
//                    		 service.get("Route") + " to " + service.get("Sign") + " -> " +
//                    		 ((String)offStop.get("Description")).replace("[", "(").replace("]", ")") + "\n");
//                }
//
//                atisResults.write(
//                		 "S " + (itin_i) + " " + String.format("%.2f",Double.parseDouble((String)itinerary.get("Totalwalk")) * 1.609)
//                		 		+ " " + itinerary.get("Transittime") + " " + summaryString + "\n");
//
//
//                atisResults.write("\n");
//                itin_i++;
//            }
//
////            atisResults.write("D " + responseString.replace("\n",  "").replace("\r", "").replace("\t",  "") + "\n\n");
//
//            Thread.sleep(3 * 1000);
//    	}
//
//    	System.out.println("done.");
//
//    	httpClient.close();
//
//    	reader.close();
//    	atisResults.close();
//    }
//}
