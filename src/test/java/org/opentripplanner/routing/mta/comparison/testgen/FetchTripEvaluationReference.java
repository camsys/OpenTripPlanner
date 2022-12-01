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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;

public class FetchTripEvaluationReference implements Runnable {
	
    private String TE_URL = "http://mta-trip-evaluation.camsys-apps.com/trip/file";
    
    private String OD_PAIRS_TXT = "src/test/resources/mta/comparison/ideal_ods.txt";;

    private String REFERENCE_RESULTS_TXT = "src/test/resources/mta/comparison/ideal.txt";
    
    public void setOutputQueryFile(String f) {
    	this.OD_PAIRS_TXT = f;
    }

    public void setOutputReferenceFile(String f) {
    	this.REFERENCE_RESULTS_TXT = f;
    }
    
    @Test
    public void run() {
    	try {
	        CloseableHttpClient httpClient = HttpClients.createDefault();
	    	
	    	URIBuilder builder = new URIBuilder(TE_URL);   
	        HttpGet get = new HttpGet(builder.build());
	
	        CloseableHttpResponse response = httpClient.execute(get);
	
	        if(response.getStatusLine().getStatusCode() != 200) {
	            System.out.println("Server responded with HTTP status code " + 
	            		response.getStatusLine().getStatusCode() + ", response: " + response.toString());        	
	
	            return;
	        }
	        
	    	FileWriter testOdPairs = new FileWriter(OD_PAIRS_TXT);
	    	FileWriter testReferenceResults = new FileWriter(REFERENCE_RESULTS_TXT);
	        	
	        String responseString = EntityUtils.toString(response.getEntity());            
	        String[] responseStringLines = responseString.split("\\n|\\r");
	        
	        for(int l = 0; l < responseStringLines.length; l++) {
	        	String line = responseStringLines[l];
	        	
	        	if(line.startsWith("Q")) {
	        		testOdPairs.write(line + "\n");
	        	}
	        	
	        	testReferenceResults.write(line + "\n");
	        }
	        
	        testOdPairs.close();
	        testReferenceResults.close();

    	} catch (Exception e) {
    		System.out.println("Error : " + e.getLocalizedMessage());
    	}
    }
}
