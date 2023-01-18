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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;
import org.opentripplanner.routing.mta.comparison.test_file_format.Result;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScoreAgainstIdealComparison {
	
    private String TEST_RESULTS_TXT = null; 
    
    private String BASELINE_RESULTS_TXT = null;
	
	public void setIdealFile(String f) {
		this.BASELINE_RESULTS_TXT = f;
	}
	
	public void setTestResultsFile(String f) {
		this.TEST_RESULTS_TXT = f;
	}
    
    private final String[] metricsDimLabels = new String[] { "APPROVED RESULTS PRESENT", "DISAPPROVED RESULTS PRESENT", "RESULT NOT TAGGED PRESENT" };

	private int[] resultSummary = new int[3]; // dimensions: metric
	
	private int[] matchCDF = new int[10]; // assumes no query with have more than 10 itins	

	private int total = 0;
	
	private Map<Integer, Integer> matchPercentCDF = new HashMap<Integer, Integer>();
	
	private boolean isSetup = false;
    public void setup() throws IOException, Exception {    	
    	if(isSetup)
    		return;
    	isSetup = true;

    	File testResultsFile = new File(TEST_RESULTS_TXT);
    	File baselineResultsFile = new File(BASELINE_RESULTS_TXT);

    	List<Result> testResults = Result.loadResults(testResultsFile);
    	List<Result> baselineResults = Result.loadResults(baselineResultsFile);

    	// ==========================COMPARE RESULTS=====================================
    	for(int i = 0; i < Math.max(baselineResults.size(), testResults.size()); i++) {
    		Result testResult = testResults.get(i);
    		Result baselineResult = baselineResults.get(i);

    		int testItinCount = 0;
			for(ItinerarySummary testItin : testResult.itineraries) {
				this.total++;
				
    			boolean foundInRef = false;
        		for(ItinerarySummary refItin : baselineResult.itineraries) {
    				if(ItinerarySummary.RANKER_EQUAL.compare(refItin,  testItin) == 0) {
    	    			if(refItin.approveOfResult == true) {
    	        			resultSummary[0]++;
    	        			testItinCount++;
    					} else if(refItin.approveOfResult == false) {
    		    			resultSummary[1]++;    	    			
    					}
    	    			
    	    			foundInRef = true;
    	    			break;
    				}
        		}
        		
        		// we have a result that isn't in the reference set
        		if(! foundInRef) {
        			resultSummary[2]++;
        		}

        		matchCDF[testItinCount]++;

        		
        		int matchPercent = (int)(((float)testItinCount / (float)testResult.itineraries.size())*100f);
        		Integer count = matchPercentCDF.get(matchPercent);
        		if(count == null)
        				count = 1;
        		else 
        				count++;
        		
        		matchPercentCDF.put(matchPercent, count);
			}
    	}

    	// ==========================PRINT RESULTS=====================================

    	System.out.println("");
    	System.out.println("Aggregate statistics:");
    	System.out.println("");

    	for(int i = 0; i < metricsDimLabels.length; i++) {
    		System.out.println(String.format("%30s", metricsDimLabels[i]) + " ......................... [" + 
    			String.format("%.1f", (float)((resultSummary[i]/(float)total)*100)) 
    			+ "%] " + resultSummary[i] + "/" + total);
    	}

    	System.out.println("");
    	System.out.println("Queries by approved results (bin = # approved results)");
    	System.out.println("");
    	
    	for(int i = 0; i < matchCDF.length; i++) {
    		System.out.println(i + " (" + String.format("%-4.1f%%", (float)((matchCDF[i]/(float)total) * 100)) + ") : " + StringUtils.repeat(".", matchCDF[i]));
    	}

    	System.out.println("");
    	System.out.println("Queries by % of results approved (bin = % results that are approved)");
    	System.out.println("");
    	
    	for(Integer bin : matchPercentCDF.keySet()) {
    		System.out.println(String.format("%-3d",bin) + "% (" + String.format("%-4.1f%%", (float)((matchPercentCDF.get(bin)/(float)total) * 100)) + ") : " + StringUtils.repeat(".", matchPercentCDF.get(bin)));
    	}
    }
    
    @TestFactory
    Collection<DynamicTest> getTests() throws IOException, Exception {
    	setup();
    	
       	return Arrays.asList(
       			
       	  DynamicTest.dynamicTest("Ideal: Disapproved < 5%", new Executable() {
			@Override
			public void execute() throws Throwable {
				assertTrue((float)resultSummary[1]/(float)total < .05f);
			}
       	  }),

       	  DynamicTest.dynamicTest("Ideal: Approved > 65%", new Executable() {
			@Override
			public void execute() throws Throwable {
				assertTrue((float)resultSummary[0]/(float)total > .65f);
			}
       	  })

       	  
       	);
    }
    

}
