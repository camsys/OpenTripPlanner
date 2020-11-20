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
package org.opentripplanner.routing.mta.comparison;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.opentripplanner.common.MavenVersion;

import flexjson.JSONDeserializer;

import java.io.*;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class BlockUntilReleaseFinished {
	
//    private static final String OTP_URL = "http://localhost:8080/otp/routers/default/version?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnts";

    private String OTP_URL = "http://otp-mta-demo.camsys-apps.com/otp/routers/default/version?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnt";
    
    private static final int TIMEOUT_S = 60 * 30; // 30m

    private static final int POLL_INTERVAL_S = 60; // 1m
    
    private static final String TEMPLATE_TEXT = "${git.commit.id}";

    public void setOTPURL(String u) {
    	this.OTP_URL = u;
    }
    
//	@Test
    public void run() throws IOException, InterruptedException, URISyntaxException {

        CloseableHttpClient httpClient = HttpClients.createDefault();
		MavenVersion targetVersion = MavenVersion.VERSION;
		DateTime timeStart = new DateTime();
    	
        System.out.println("Waiting for " + TIMEOUT_S + " seconds or until commit " + targetVersion.commit + 
        		" was used to build the bundle at test server (" + OTP_URL + ")...");        	
        
        // code wasn't compiled with mvn, so just skip ahead!
        if(targetVersion.commit.equals(TEMPLATE_TEXT)) {
            System.out.println("source not built with Maven; continuing.");        	
        	return;
        }
        
        while(new DateTime().getMillis() - timeStart.getMillis() <= TIMEOUT_S * 1000) {        	
        	Thread.sleep(POLL_INTERVAL_S * 1000);        	

        	URIBuilder builder = new URIBuilder(OTP_URL);   
            HttpGet get = new HttpGet(builder.build());

            CloseableHttpResponse response = httpClient.execute(get);

            if(response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Server responded with HTTP status code " + 
                		response.getStatusLine().getStatusCode() + ", response: " + response.toString());        	
            	continue;
            }
            
            String responseString = EntityUtils.toString(response.getEntity());            
            HashMap<String, Map> versionResponse = (HashMap<String, Map>)new JSONDeserializer().deserialize(responseString);             

            response.close();
            
            HashMap<String, Object> builderVersion = (HashMap<String, Object>) versionResponse.get("builderVersion");
            if(builderVersion == null) {
            	builderVersion = (HashMap<String, Object>) versionResponse.get("serverVersion");
            	
                if(builderVersion == null) {
                	System.out.print("A version was not included in response; continuing since there's nothing to compare!");        	
                	break;
                }
            }
            
        	String commitIdDeployed = (String) builderVersion.get("commit");
       
        	if(commitIdDeployed.equals(targetVersion.commit)) {
                System.out.println("Found it. Continuing...");        	
                break;
        	} else {
                System.out.println("Not there yet (version deployed = " + commitIdDeployed + "), waiting " + POLL_INTERVAL_S + " seconds...");        	        		
        	}
        }    	
    }
}
