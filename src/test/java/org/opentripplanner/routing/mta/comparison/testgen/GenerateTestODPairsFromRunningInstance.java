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
//import flexjson.JSONDeserializer;
//import org.apache.commons.lang.math.RandomUtils;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.utils.URIBuilder;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//import org.joda.time.DateTime;
//import org.junit.jupiter.api.Test;
//import org.onebusaway.gtfs.model.AgencyAndId;
//import org.opentripplanner.routing.graph.Graph;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Map;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//public class GenerateTestODPairsFromRunningInstance {
//
//    private String PAIRS_TXT = "src/test/resources/mta/comparison/baseline_ods.txt";
//
//    private double ACCESSIBILITY_PCT = 0;
//
//    private boolean MTA_ONLY = true;
//
//    private String OTP_STOPS_URL = "http://otp-mta-qa.camsys-apps.com/otp/routers/default/index/stops?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L";
//
//    private int PAIRS_TO_GENERATE = 200;
//
//    private static final String[] optimizations = new String[] { "W", "X", "T" };
//
//    private Map<String, Integer> agencyMax = (Map<String, Integer>) Stream.of(new Object[][] {
//        { "MTA", 50 },
//        { "MTASBWY", 50 },
//        { "MNR", 50 },
//        { "LI", 50 }}
//    ).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));
//
//    private ArrayList<String> agenciesIndex = new ArrayList<>(Arrays.asList("MTA", "MTASBWY", "MNR", "LI" ));
//
//    private int pointsByAgency[];
//
//    protected static Graph graph;
//
//    public void setOTPURL(String u) {
//    	this.OTP_STOPS_URL = u;
//    }
//
//    public void setAccessibilityPercent(Double p) {
//    	this.ACCESSIBILITY_PCT = p;
//    }
//
//    public void setPairsToGenerate(int v) {
//    	this.PAIRS_TO_GENERATE = v;
//    }
//
//    public void setOutputFile(String f) {
//    	this.PAIRS_TXT = f;
//    }
//
//    public void setMTAOnly(boolean v) {
//    	this.MTA_ONLY = v;
//    }
//
//    public void setMax(String agency, int max) {
//    	agencyMax.put(agency,  max);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void run() throws IOException, URISyntaxException {
//
//    	CloseableHttpClient httpClient = HttpClients.createDefault();
//    	URIBuilder builder = new URIBuilder(OTP_STOPS_URL);
//    	HttpGet get = new HttpGet(builder.build());
//
//    	CloseableHttpResponse response = httpClient.execute(get);
//    	String responseString = EntityUtils.toString(response.getEntity());
//
//    	ArrayList<Map> stops = null;
//    	try {
//    		stops = (ArrayList<Map>) new JSONDeserializer().deserialize(responseString);
//    	} catch (Exception e) {
//    		System.out.println("Invalid server response when requesting all stops: " + responseString);
//    		return;
//    	}
//
//
//		if(!agencyMax.isEmpty()) {
//		    agenciesIndex = new ArrayList<String>();
//			agenciesIndex.addAll(agencyMax.keySet());
//
//			pointsByAgency = new int[agencyMax.keySet().size()];
//   		}
//
//
//    	System.out.print("Generating stop pairs ...");
//
//    	DateTime startTime = new DateTime();
//		Long searchPeriod = (long) (2 * 30 * 24 * 60 * 60 * 1000); // 3 months
//
//    	FileWriter testOdPairs = new FileWriter(PAIRS_TXT);
//
//    	int i = PAIRS_TO_GENERATE;
//    	while(true) {
//        	int p1 = (int) (Math.random() * stops.size());
//    		int p2 = (int) (Math.random() * stops.size());
//
//    		Map s1 = stops.get(p1);
//    		Map s2 = stops.get(p2);
//
//    		AgencyAndId stop1id = AgencyAndId.convertFromString((String)s1.get("id"), ':');
//       		AgencyAndId stop2id = AgencyAndId.convertFromString((String)s2.get("id"), ':');
//
//
//       		if(MTA_ONLY) {
//	       		if(!stop1id.getAgencyId().equals("MTASBWY") && !stop1id.getAgencyId().equals("MTA")
//	       				&& !stop1id.getAgencyId().equals("MNR") && !stop1id.getAgencyId().equals("LI"))
//	       			continue;
//
//	       		if(!stop2id.getAgencyId().equals("MTASBWY") && !stop2id.getAgencyId().equals("MTA")
//	       				&& !stop2id.getAgencyId().equals("MNR") && !stop2id.getAgencyId().equals("LI"))
//	       			continue;
//       		}
//
//
//       		// track how many points per agency we have, and if the count exceeds the target percentage
//       		// then skip this plan. Just evaluate the starting point because if we eval both points, the
//       		// probability of finding a trip that works goes /way/ down
//       		if(!agencyMax.isEmpty()) {
//       			String agency1 = stop1id.getAgencyId();
//
//       			if(agenciesIndex.indexOf(agency1) < 0) {
//       				System.out.println("Unknown agency: " + agency1);
//       				continue;
//       			}
//
//       			if(pointsByAgency[agenciesIndex.indexOf(agency1)] > agencyMax.get(agency1)) {
//       				continue;
//       			} else {
//       				pointsByAgency[agenciesIndex.indexOf(agency1)]++;
//       			}
//       		}
//
//
//    		DateTime randomTime = new DateTime(startTime.getMillis() + (long)(searchPeriod * Math.random()));
//
//    		//System.out.println("Adding " + stop1id + " -> " + stop2id);
//
//    		testOdPairs.write("Q " + ((Math.random() < ACCESSIBILITY_PCT) ? "Y " : "N ") +
//    				randomTime.getMillis() + " " +
//    				s1.get("lat") + "," + s1.get("lon") + " " +
//    				s2.get("lat") + "," + s2.get("lon") + " " +
//    				optimizations[RandomUtils.nextInt(optimizations.length)] +
//    				"\n");
//
//    		System.out.print(".");
//
//        	if(i-- <= 0)
//        		break;
//    	}
//
//    	System.out.println("done.");
//
//    	testOdPairs.close();
//    }
//}
