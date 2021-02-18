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

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;
import org.opentripplanner.routing.mta.comparison.test_file_format.Query;
import org.opentripplanner.routing.mta.comparison.test_file_format.Result;

import java.util.*;

import static org.junit.Assert.assertTrue;

import java.io.*;

public class QualitativeMultiDimInstanceComparison {
	
    private String TEST_RESULTS_TXT = "src/test/resources/mta/comparison/dev.txt"; 

    private String BASELINE_RESULTS_TXT = "src/test/resources/mta/comparison/prod.txt";

	public void setBaselineResultsFile(String f) {
		this.BASELINE_RESULTS_TXT = f;
	}
	
	public void setTestResultsFile(String f) {
		this.TEST_RESULTS_TXT = f;
	}
	
	private enum optimizationDim {ALL};
	
	private final String[] optimizationDimLabels = new String[] { "ALL" };
	
    private enum metricsDim { W, X, T, hasResults, match };

    private final String[] metricsDimLabels = new String[] { "WALKING (km)", "TRANSFERS", "TIME (min)", "PRODUCED A RESULT", "BASELINE TOP IN TEST RESULTS**" };

	public enum platformDim { BASELINE, TEST, TIE };
	
	// dimensions: optimization
	private int[] totalByOptimization = new int[3];

	// dimensions: optimization | metric | metric
	@SuppressWarnings("unchecked")
	private List<Double>[][][]resultWinMargin = new ArrayList[3][5][3];

	// dimensions: optimization | metric | platform
	private int[][][] resultSummary = new int[3][5][3];
		
	private List<Query> noResultQueries = new ArrayList<Query>();

	private List<Query> testIsWorseQueries = new ArrayList<Query>();

	private void scorePlatform(optimizationDim optimization, metricsDim metric, 
			List<ItinerarySummary> sortedResults, Comparator<ItinerarySummary> ranker, Query query) {

		sortedResults.sort(ranker);	    				
		
		if(sortedResults.size() > 0) {
			ItinerarySummary winningResult = sortedResults.get(0);
			platformDim winningPlatform = sortedResults.get(0).platform;	

			// the other platform's first result, equal or not to our top result
			ItinerarySummary otherPlatformFirstResult = null;
			
			// the first result that is less than our result
			ItinerarySummary runnerUpResult = null; 						
			
			for(int i = 1; i < sortedResults.size(); i++) {
				ItinerarySummary p_Result = sortedResults.get(i);
				platformDim p_Platform = sortedResults.get(i).platform;

				if(winningPlatform != p_Platform && otherPlatformFirstResult == null) {
					otherPlatformFirstResult = p_Result;
				}
				
				int compareResult = ranker.compare(winningResult, p_Result);
				if(compareResult < 0 && runnerUpResult == null) {
					runnerUpResult = p_Result;
				}
			} // for sortedResults, looking for runner up

			// other platform has no first result = one winner
			if(otherPlatformFirstResult == null) {
				this.resultSummary
				[optimization.ordinal()]
				[metric.ordinal()]
				[winningPlatform.ordinal()]++;	
			} else {
				// other platform produced a result--is it equal to the winner? If so, tie
				if(ranker.compare(otherPlatformFirstResult, winningResult) == 0) {
					this.resultSummary
					[optimization.ordinal()]
					[metric.ordinal()]
					[platformDim.TIE.ordinal()]++;	
				} else {
				// one winner
					this.resultSummary
					[optimization.ordinal()]
					[metric.ordinal()]
					[winningPlatform.ordinal()]++;						
				}
			}
			
			// if we won and the other platform produced a losing result
			if(otherPlatformFirstResult != null 
					&& ranker.compare(otherPlatformFirstResult, winningResult) != 0) {

				if(winningPlatform.equals(platformDim.BASELINE))
					testIsWorseQueries.add(query);

				switch(metric) {
				case T:
					this.resultWinMargin
						[optimization.ordinal()]
						[metric.ordinal()]
						[winningResult.platform.ordinal()].add((double)(winningResult.transitTime - otherPlatformFirstResult.transitTime));
					break;
				case W:
					this.resultWinMargin
						[optimization.ordinal()]
						[metric.ordinal()]
						[winningResult.platform.ordinal()].add(winningResult.walkDistance - otherPlatformFirstResult.walkDistance);
					break;
				case X:
					this.resultWinMargin
						[optimization.ordinal()]
						[metric.ordinal()]
						[winningResult.platform.ordinal()].add((double)(winningResult.routes.split(">").length - otherPlatformFirstResult.routes.split(">").length));
					break;
					
				// nothing to do here
				case hasResults:
				case match:
				default:
					break;					
				}					
			} 			
			
		}
	}
	
	private boolean isSetup = false;
	public void setup() throws IOException, Exception {    	
		if(isSetup)
			return;
		isSetup = true;

		File devResultsFile = new File(TEST_RESULTS_TXT);
		File baselineResultsFile = new File(BASELINE_RESULTS_TXT);

		List<Result> devResults = Result.loadResults(devResultsFile, platformDim.TEST);
		List<Result> baselineResults = Result.loadResults(baselineResultsFile, platformDim.BASELINE);

		// initialize stats storage array
		for(int i = 0; i < resultWinMargin.length; i++) {
			for(int z = 0; z < resultWinMargin[i].length; z++)
				for(int a = 0; a < resultWinMargin[i][z].length; a++)
					resultWinMargin[i][z][a] = new ArrayList();
		}


		// ==========================COMPARE RESULTS=====================================

		for(int i = 0; i < Math.min(baselineResults.size(), devResults.size()); i++) {
			Result testResult = devResults.get(i);
			Result baselineResult = baselineResults.get(i);

			Query query = baselineResult.query;

			int o = optimizationDim.ALL.ordinal();

			// add all system's itineraries to an array to sort based on metric and score
			List<ItinerarySummary> sortedResults = new ArrayList<ItinerarySummary>();
			sortedResults.addAll(baselineResult.itineraries);
			sortedResults.addAll(testResult.itineraries);

			// both systems produced nothing; skip?
			if(sortedResults.isEmpty()) {
				noResultQueries.add(baselineResult.query);

				continue;
			}

			totalByOptimization[o]++;

			for(int m = 0; m < metricsDim.values().length; m++) {
				switch(metricsDim.values()[m]) {
				case T:
					// time, walk, transfers
					scorePlatform(optimizationDim.values()[o], 
							metricsDim.values()[m], sortedResults, ItinerarySummary.RANKER_TIME, query);

					break;
				case W:
					// walk, transit time, transfers

					scorePlatform(optimizationDim.values()[o], 
							metricsDim.values()[m], sortedResults, ItinerarySummary.RANKER_WALKING, query);

					break;
				case X:
					// transfers, time, walk distance

					scorePlatform(optimizationDim.values()[o], 
							metricsDim.values()[m], sortedResults, ItinerarySummary.RANKER_XFERS, query);

					break;

				case match:
					if(baselineResult.itineraries.size() == 0)
						break;

					ItinerarySummary ourTopResult = baselineResult.itineraries.get(0);

					for(int z = 0; z < testResult.itineraries.size(); z++) {
						ItinerarySummary theirResult = testResult.itineraries.get(z);

						if(ItinerarySummary.RANKER_EQUAL.compare(ourTopResult, theirResult) == 0) {
							this.resultSummary
							[o]
									[m]	
											[platformDim.TIE.ordinal()]++;
							break;
						}
					}

					break;

				case hasResults:

					if(!baselineResult.itineraries.isEmpty() && !testResult.itineraries.isEmpty()) {
						this.resultSummary
						[o]
								[m]
										[platformDim.TIE.ordinal()]++;

					} else if(!baselineResult.itineraries.isEmpty()) {
						this.resultSummary
						[o]
								[m]
										[platformDim.BASELINE.ordinal()]++;
					} else if(!testResult.itineraries.isEmpty()) {
						this.resultSummary
						[o]
								[m]
										[platformDim.TEST.ordinal()]++;
					}

					break;

					// will never get here
				default:
					break; 
				} // end switch
			} // for each metric
		}
		
		// ==========================PRINT RESULTS=====================================

    	boolean overallResult = true;
    	    	
    	System.out.println("");
    	System.out.println("TOTAL RESULTS: " + baselineResults.size() + "\n");
  
    	for(int o = 0; o < optimizationDim.values().length; o++) {
    		String header = "OPTIMIZATION: " + String.format("%-10s (n=%3d)", optimizationDimLabels[o], totalByOptimization[o]) + 
    				"         BASELINE           TIE           TEST      BASELINE SUCCESS MARGIN STATS*          TEST SUCCESS MARGIN STATS*";
        	System.out.println(header);
        	System.out.println(header.replaceAll("[^\\s]", "-"));
        	
        	for(int m = 0; m < metricsDim.values().length; m++) {
        		int total = totalByOptimization[o];
        		
        		double[] baselineValuesAsPrimitive = new double[this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].size()];
        		for(int ii = 0; ii < this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].size(); ii++) {
        			baselineValuesAsPrimitive[ii] = this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].get(ii);   
        		}
        		
        		double[] devValuesAsPrimitive = new double[this.resultWinMargin[o][m][platformDim.TEST.ordinal()].size()];
        		for(int ii = 0; ii < this.resultWinMargin[o][m][platformDim.TEST.ordinal()].size(); ii++) {
        			devValuesAsPrimitive[ii] = this.resultWinMargin[o][m][platformDim.TEST.ordinal()].get(ii);   
        		}

        		Mean meanStat = new Mean();
        		Max maxStat = new Max();        		  
        		Min minStat = new Min();        		  
        		
            	System.out.print(String.format("%-30s", metricsDimLabels[m]) + 
            		"           " + 
            		String.format("%-3d (%-3.0f%%)", 
            			this.resultSummary
        				[o]
        				[m]
        				[platformDim.BASELINE.ordinal()], 
        				((float)this.resultSummary
        				[o]
        				[m]
        				[platformDim.BASELINE.ordinal()]/total)*100)
        			+ "    " + 
            		String.format("%-3d (%-3.0f%%)", 
                			this.resultSummary
            				[o]
            				[m]
            				[platformDim.TIE.ordinal()], 
            				((float)this.resultSummary
            				[o]
            				[m]
            				[platformDim.TIE.ordinal()]/total)*100)
        			+ "    " + 
            		String.format("%-3d (%-3.0f%%)", 
            			this.resultSummary
        				[o]
        				[m]
        				[platformDim.TEST.ordinal()], 
        				((float)this.resultSummary
        				[o]
        				[m]
        				[platformDim.TEST.ordinal()]/total)*100)
            		+ "   " + 
        			(m != metricsDim.match.ordinal() && m != metricsDim.hasResults.ordinal() 
        				? String.format(" M=%-6.2f n=%3d,[%6.2f,%-6.2f]", meanStat.evaluate(baselineValuesAsPrimitive), 
        						this.resultWinMargin[o][m][platformDim.BASELINE.ordinal()].size(),
        						minStat.evaluate(baselineValuesAsPrimitive), 
        						maxStat.evaluate(baselineValuesAsPrimitive)) + "    " 
        						: "                                   ")
            		+ 
        			(m != metricsDim.match.ordinal() && m != metricsDim.hasResults.ordinal() 
        				? String.format(" M=%-6.2f n=%3d,[%6.2f,%-6.2f]", meanStat.evaluate(devValuesAsPrimitive), 
        						this.resultWinMargin[o][m][platformDim.TEST.ordinal()].size(),
        						minStat.evaluate(devValuesAsPrimitive), 
        						maxStat.evaluate(devValuesAsPrimitive)) + "    " 
        						: "                                   ")

            	);

            	
            	// if this is the metric for the optimization--e.g. it's the WALKING results for the optimization WALKING, make 
            	// it one of the things that makes the whole test pass/fail
        		float ourPercentage = 
        				((float)((this.resultSummary[o][m][platformDim.TEST.ordinal()] + 
        						this.resultSummary[o][m][platformDim.TIE.ordinal()])
        				/ (float)totalByOptimization[o])) * 100;

        		// for each optimization, require our result to be the winner 80% of the time
        		if(m == metricsDim.T.ordinal() || m == metricsDim.W.ordinal() || m == metricsDim.X.ordinal()) {
//        				metricsDimLabels[m].startsWith(optimizationDimLabels[o])) {
            		if(ourPercentage < 80) {
            			overallResult = false;
                		System.out.println(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 80%+]");
            		} else {
                		System.out.println(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
            		}
            	} else {
            		// for the other two metrics (has results and matches), require 100% and 80%+ respectively
            		if(m == metricsDim.hasResults.ordinal()) {
                		if(ourPercentage < 95) {
                			overallResult = false;
                    		System.out.println(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 95%+]");
                		} else {
                    		System.out.println(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
                		}
            		} else if(m == metricsDim.match.ordinal()) {
                		if(ourPercentage < 60) {
                			overallResult = false;
                    		System.out.println(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 60%+]");
                		} else {
                    		System.out.println(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
                		}
            		} else {
            			System.out.println("");
            		}
            	}
        	}    
        	
        	System.out.println("");
       	}
    	
    	System.out.println("* success margin stats are computed against the opposing platform's top result, if it produces one. Ties are not included.\n"
    			+ "** 'no result' matches are included.\n");
    	
    	System.out.println("SYSTEM UNDER TEST WAS WORSE (n=" + testIsWorseQueries.size() + "):");
    	for(Query q : testIsWorseQueries) {
    		System.out.println("http://otp-mta-dev.camsys-apps.com/plan?" + q.toQueryString());
    	}

    	System.out.println("");
    	System.out.println("");

    	System.out.println("NO RESULT QUERIES (ACROSS BOTH SYSTEMS):");
    	for(Query q : noResultQueries) {
    		System.out.println(q);
    	}

    	System.out.println("");
    	System.out.println("");
	}
	    
    @TestFactory
    public Collection<DynamicTest> getTests() throws IOException, Exception {
    	setup();
    	
    	Collection<DynamicTest> results = new ArrayList<DynamicTest>();
    	    	
    	for(int o = 0; o < optimizationDim.values().length; o++) {
        	for(int m = 0; m < metricsDim.values().length; m++) {
                float ourPercentage = 
                		((float)((this.resultSummary[o][m][platformDim.TEST.ordinal()] + 
                				this.resultSummary[o][m][platformDim.TIE.ordinal()])
                		/ (float)totalByOptimization[o])) * 100;

                // for each optimization, require our result to be the winner 80% of the time
                if(metricsDimLabels[m].startsWith(optimizationDimLabels[o])) {
            		results.add(DynamicTest.dynamicTest("MultiDim: Optimization " + optimizationDimLabels[o] + " | Metric " + metricsDimLabels[m] + ""
            				+ " > System under test equal to/greater on optimization vs. baseline >= 95%", new Executable() {
            			@Override
            			public void execute() throws Throwable {
            				assertTrue(ourPercentage >= 95f);	
            			}
                   	}));

            	} else {
            		// for the other two metrics (has results and matches), require 100% and 80%+ respectively
            		if(m == metricsDim.hasResults.ordinal()) {
                		results.add(DynamicTest.dynamicTest("MultiDim: Optimization " + optimizationDimLabels[o] + " | Metric " + metricsDimLabels[m] + " "
                				+ "> System under test provided a result >= 98%", new Executable() {
                			@Override
                			public void execute() throws Throwable {
                				assertTrue(ourPercentage >= 98f);	
                			}
                       	}));

            		} else if(m == metricsDim.match.ordinal()) {
                		results.add(DynamicTest.dynamicTest("MultiDim: Optimization " + optimizationDimLabels[o] + " | Metric " + metricsDimLabels[m] + " "
                				+ "> System under test matches baseline >= 90%", new Executable() {
                			@Override
                			public void execute() throws Throwable {
                				assertTrue(ourPercentage >= 90);	
                			}
                       	}));
            		}
            	}
        	}
    	}
   
       	return results;
    }

}
