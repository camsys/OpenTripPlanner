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

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;
import org.opentripplanner.routing.mta.comparison.test_file_format.Query;
import org.opentripplanner.routing.mta.comparison.test_file_format.Result;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.*;

public class ScoreAgainstIdealComparison {
	
    private String TEST_RESULTS_TXT = "src/test/resources/mta/tripeval_dev_results.txt"; 

    private String BASELINE_RESULTS_TXT = "src/test/resources/mta/tripeval_output_results.txt";
		
    private final String[] metricsDimLabels = new String[] { "APPROVED RESULTS PRESENT", "DISAPPROVED RESULTS PRESENT", "RESULT NOT TAGGED PRESENT" };

	// dimensions: metric
	private int[] resultSummary = new int[3];
	
	private int[] matchCDF = new int[10]; // assumes no query with have more than 10 itins	

	private Map<Integer, Integer> matchPercentCDF = new HashMap<Integer, Integer>();
	
	public void setBaselineResultsFile(String f) {
		this.BASELINE_RESULTS_TXT = f;
	}
	
	public void setTestResultsFile(String f) {
		this.TEST_RESULTS_TXT = f;
	}
	
	private List<Result> loadResults(File resultsFile) throws Exception {
    	List<Result> results = new ArrayList<Result>();

    	Scanner resultsReader = new Scanner(resultsFile);

		Query q = null;
		Result r = null;
    	while (resultsReader.hasNextLine()) {
    		String line = resultsReader.nextLine();

    		if(line.startsWith("Q")) {
    	    	if(r != null) {
    				results.add(r);
    	    		r = null;
    	    	}
    			
    			q = new Query(line);
    			r = new Result();    			
    			r.query = q;
    		}

    		if(line.startsWith("S")) {
    			ItinerarySummary s = new ItinerarySummary(line);
    			r.itineraries.add(s);
    		}
    	}
    	
    	if(r != null) {
			results.add(r);
			r = null;
    	}
    	
    	resultsReader.close();
    	
    	return results;
	}
	
    @Test
    public void run() throws IOException, Exception {    	
    	File testResultsFile = new File(TEST_RESULTS_TXT);
    	File baselineResultsFile = new File(BASELINE_RESULTS_TXT);

    	List<Result> testResults = loadResults(testResultsFile);
    	List<Result> baselineResults = loadResults(baselineResultsFile);

    	// ==========================COMPARE RESULTS=====================================
    	int total = 0;
    	for(int i = 0; i < Math.max(baselineResults.size(), testResults.size()); i++) {
    		Result testResult = testResults.get(i);
    		Result baselineResult = baselineResults.get(i);
    		Query baselineQuery = baselineResult.query;

    		int testItinCount = 0;
			for(ItinerarySummary testItin : testResult.itineraries) {
				total++;
				
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
    	

    	// disapproved results < 10%
    	assertTrue((float)(resultSummary[1]/(float)total) < .10f);

    	// queries with 0 approved results < 10%
    	assertTrue((float)(matchCDF[0]/(float)total) < .10f);

    }

}
