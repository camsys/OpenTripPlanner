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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;
import org.opentripplanner.routing.mta.comparison.test_file_format.Query;
import org.opentripplanner.routing.mta.comparison.test_file_format.Result;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class QualitativeMultiDimInstanceComparison {
	
    private String TEST_RESULTS_TXT = "src/test/resources/mta/comparison/baseline_results.txt"; 

    private String BASELINE_RESULTS_TXT = "src/test/resources/mta/comparison/baseline.txt";

	public void setBaselineResultsFile(String f) {
		this.BASELINE_RESULTS_TXT = f;
	}
	
	public void setTestResultsFile(String f) {
		this.TEST_RESULTS_TXT = f;
	}
	
    private enum metricsDim { W, X, T, hasResults, topMatch };

    private final String[] metricsDimLabels = new String[] { "WALKING (km)", "TRANSFERS", "TIME (min)", "PRODUCED A RESULT", "BASELINE TOP IN TEST RESULTS**" };

	public enum platformDim { BASELINE, TEST, TIE };
	
	// dimensions: metric | (win/loss/tie)
	@SuppressWarnings("unchecked")
	private SummaryStatistics[][] resultStats = new SummaryStatistics[5][3];
	private Frequency[][] resultDists = new Frequency[5][3];
	private ArrayList<Double>[][] resultData = new ArrayList[5][3];

	// dimensions: metric | (win/loss/tie)
	private int[][] resultSummary = new int[5][3];
		
	private List<Query> noResultQueries = new ArrayList<Query>();

	private List<Query> testIsWorseQueries = new ArrayList<Query>();

	private void scorePlatform(metricsDim metric, List<ItinerarySummary> sortedResults, 
			Comparator<ItinerarySummary> ranker, Query query) {

		sortedResults.sort(ranker);	    				
		
		if(sortedResults.size() <= 0)
			return;
		
		ItinerarySummary topResult = sortedResults.get(0);
		platformDim topPlatform = sortedResults.get(0).platform;	

		// the other platform's first result, equal or not to our top result
		ItinerarySummary otherPlatformTopResult = null;
			
		for(int i = 1; i < sortedResults.size(); i++) {
			ItinerarySummary p_Result = sortedResults.get(i);
			platformDim p_Platform = sortedResults.get(i).platform;

			if(topPlatform != p_Platform && otherPlatformTopResult == null) {
				otherPlatformTopResult = p_Result;
			}
		}

		// other platform has no first result = one winner
		if(otherPlatformTopResult == null) {
			this.resultSummary
			[metric.ordinal()]
			[topPlatform.ordinal()]++;	
		} else {
			// other platform produced a result--is it equal to the winner? If so, tie
			if(ranker.compare(otherPlatformTopResult, topResult) == 0) {
				this.resultSummary
				[metric.ordinal()]
				[platformDim.TIE.ordinal()]++;	
			} else {
				// one winner
				this.resultSummary
				[metric.ordinal()]
				[topPlatform.ordinal()]++;						
			}
		}

		if(otherPlatformTopResult == null || topResult == null)
			return;
		
		switch(metric) {
		case T:
			this.resultStats
				[metric.ordinal()]
				[topResult.platform.ordinal()].addValue((double)topResult.transitTime);
	
			this.resultStats
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].addValue((double)otherPlatformTopResult.transitTime);
	
			this.resultDists
				[metric.ordinal()]
				[topResult.platform.ordinal()].addValue((double)topResult.transitTime);

			this.resultDists
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].addValue((double)otherPlatformTopResult.transitTime);

			this.resultData
				[metric.ordinal()]
				[topResult.platform.ordinal()].add((double)topResult.transitTime);

			this.resultData
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].add((double)otherPlatformTopResult.transitTime);		
		
			break;
		case W:
			this.resultStats
				[metric.ordinal()]
				[topResult.platform.ordinal()].addValue((double)topResult.walkDistance);

			this.resultStats
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].addValue((double)otherPlatformTopResult.walkDistance);

			this.resultDists
				[metric.ordinal()]
				[topResult.platform.ordinal()].addValue((double)topResult.walkDistance);

			this.resultDists
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].addValue((double)otherPlatformTopResult.walkDistance);

			this.resultData
				[metric.ordinal()]
				[topResult.platform.ordinal()].add((double)topResult.walkDistance);

			this.resultData
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].add((double)otherPlatformTopResult.walkDistance);		

			break;
		case X:
			this.resultStats
				[metric.ordinal()]
				[topResult.platform.ordinal()].addValue((double)topResult.routes.split(">").length);

			this.resultStats
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].addValue((double)otherPlatformTopResult.routes.split(">").length);

			this.resultDists
				[metric.ordinal()]
				[topResult.platform.ordinal()].addValue((double)otherPlatformTopResult.routes.split(">").length);

			this.resultDists
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].addValue((double)otherPlatformTopResult.routes.split(">").length);
			
			this.resultData
				[metric.ordinal()]
				[topResult.platform.ordinal()].add((double)topResult.routes.split(">").length);

			this.resultData
				[metric.ordinal()]
				[otherPlatformTopResult.platform.ordinal()].add((double)otherPlatformTopResult.routes.split(">").length);		

			break;
				
			// nothing to do here
		case hasResults:
		case topMatch:
		default:
			break;					
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
		for(int z = 0; z < resultStats.length; z++)
			for(int i = 0; i < platformDim.values().length; i++) {
				resultStats[z][i] = new SummaryStatistics();
				resultDists[z][i] = new Frequency();
				resultData[z][i] = new ArrayList<Double>();
			}
			

		// ==========================COMPARE RESULTS=====================================

		for(int i = 0; i < Math.min(baselineResults.size(), devResults.size()); i++) {
			Result testResult = devResults.get(i);
			Result baselineResult = baselineResults.get(i);
			Query query = baselineResult.query;

			// add all systems' itineraries to an array to sort based on metric and score
			List<ItinerarySummary> sortedResults = new ArrayList<ItinerarySummary>();
			sortedResults.addAll(baselineResult.itineraries);
			sortedResults.addAll(testResult.itineraries);

			// both systems produced nothing; skip?
			if(sortedResults.isEmpty()) {
				noResultQueries.add(baselineResult.query);
				continue;
			}

			for(int m = 0; m < metricsDim.values().length; m++) {
				switch(metricsDim.values()[m]) {
				case T:
					// time, walk, transfers
					scorePlatform(metricsDim.values()[m], sortedResults, ItinerarySummary.RANKER_TIME, query);

					break;
				case W:
					// walk, transit time, transfers
					scorePlatform(metricsDim.values()[m], sortedResults, ItinerarySummary.RANKER_WALKING, query);

					break;
				case X:
					// transfers, time, walk distance
					scorePlatform(metricsDim.values()[m], sortedResults, ItinerarySummary.RANKER_XFERS, query);

					break;

				case topMatch:
					if(baselineResult.itineraries.size() == 0)
						break;

					ItinerarySummary baselineTopResult = baselineResult.itineraries.get(0);
					for(int z = 0; z < testResult.itineraries.size(); z++) {
						ItinerarySummary testResultItem = testResult.itineraries.get(z);

						if(ItinerarySummary.RANKER_EQUAL.compare(baselineTopResult, testResultItem) == 0) {
							this.resultSummary
									[m]	
									[platformDim.TIE.ordinal()]++;
							break;
						}
					}

					break;

				case hasResults:
					if(!baselineResult.itineraries.isEmpty() && !testResult.itineraries.isEmpty()) {
						this.resultSummary
							[m]
							[platformDim.TIE.ordinal()]++;
					} else if(!baselineResult.itineraries.isEmpty()) {
						this.resultSummary
							[m]
							[platformDim.BASELINE.ordinal()]++;
					} else if(!testResult.itineraries.isEmpty()) {
						this.resultSummary
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
    	    	
    	System.out.println("");
    	System.out.println("n=" + baselineResults.size() + "\n");
  
    	String header = "                                         BASELINE          TIE          TEST       BASELINE VALUES                              TEST VALUES";
        System.out.println(header);
        System.out.println(header.replaceAll("[^\\s]", "-"));
        	
        for(int m = 0; m < metricsDim.values().length; m++) {	
    		float total = this.resultSummary[m][platformDim.BASELINE.ordinal()] + 
    				this.resultSummary[m][platformDim.TEST.ordinal()] + 
    				this.resultSummary[m][platformDim.TIE.ordinal()];

    		SummaryStatistics[] metricStatsByPlatform = resultStats[m];

            System.out.print(String.format("%-30s", metricsDimLabels[m]) + 
        	"           " + 
        	String.format("%-3d (%-3.0f%%)", 
        		this.resultSummary
    			[m]
    			[platformDim.BASELINE.ordinal()], 
    			((float)this.resultSummary
    			[m]
    			[platformDim.BASELINE.ordinal()]/total)*100)
    		+ "    " + 
        	String.format("%-3d (%-3.0f%%)", 
            	this.resultSummary
        		[m]
        		[platformDim.TIE.ordinal()], 
        		((float)this.resultSummary
        		[m]
        		[platformDim.TIE.ordinal()]/total)*100)
    		+ "    " + 
        	String.format("%-3d (%-3.0f%%)", 
        		this.resultSummary
    			[m]
    			[platformDim.TEST.ordinal()], 
    			((float)this.resultSummary
    			[m]
    			[platformDim.TEST.ordinal()]/total)*100)
        	+ "   " + 
    		(m != metricsDim.topMatch.ordinal() && m != metricsDim.hasResults.ordinal() 
    			? String.format(" M=%-6.2f SD=%-6.2f n=%3d,[%6.2f,%-6.2f]", 
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()].getMean(), 
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()].getStandardDeviation(),
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()].getN(),
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()].getMin(), 
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()].getMax()) + "    " 
    					: "                                             ")
        	+ 
    		(m != metricsDim.topMatch.ordinal() && m != metricsDim.hasResults.ordinal() 
    			? String.format(" M=%-6.2f SD=%-6.2f n=%3d,[%6.2f,%-6.2f]", 
    					metricStatsByPlatform[platformDim.TEST.ordinal()].getMean(), 
    					metricStatsByPlatform[platformDim.TEST.ordinal()].getStandardDeviation(),
    					metricStatsByPlatform[platformDim.TEST.ordinal()].getN(),
    					metricStatsByPlatform[platformDim.TEST.ordinal()].getMin(), 
    					metricStatsByPlatform[platformDim.TEST.ordinal()].getMax()) + "    " 
    					: "                                             ")
        	);

            ArrayList<Double> dataBaseline = resultData[m][platformDim.BASELINE.ordinal()];
            ArrayList<Double> dataTest = resultData[m][platformDim.TEST.ordinal()];

    		// for each optimization, require our result to be the winner 80% of the time
    		if(m == metricsDim.T.ordinal() || m == metricsDim.W.ordinal() || m == metricsDim.X.ordinal()) {    			
    			TTest t = new TTest();
				SummaryStatistics check = metricStatsByPlatform[platformDim.TEST.ordinal()];
				if (check.getN() < 2) {
					System.out.println(" [FAIL -- NO RESULTS]");
					continue;
				}

				double pValue = t.tTest(metricStatsByPlatform[platformDim.TEST.ordinal()],
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()]) * 100;
    			
        		if(pValue < 65) {
            		System.out.print(" [FAIL p(t)=" + String.format("%3.1f",  pValue) + "% need 65%+]");
        		} else {
            		System.out.print(" [PASS p(t)=" + String.format("%3.1f",  pValue) + "%]");
        		}
        		
        		System.out.println("");
        		System.out.println("");
        		
    			long[] testHistogram = calcHistogram(ArrayUtils.toPrimitive((Double[])dataTest.toArray(new Double[] {})), 
    					metricStatsByPlatform[platformDim.TEST.ordinal()].getMin(), 
    					metricStatsByPlatform[platformDim.TEST.ordinal()].getMax(), 
    					(int)Math.min(metricStatsByPlatform[platformDim.TEST.ordinal()].getMax(), 10));
    			long[] baselineHistogram = calcHistogram(ArrayUtils.toPrimitive((Double[])dataBaseline.toArray(new Double[] {})),
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()].getMin(), 
    					metricStatsByPlatform[platformDim.BASELINE.ordinal()].getMax(), 
    					(int)Math.min(metricStatsByPlatform[platformDim.BASELINE.ordinal()].getMax(), 10));
    			
    			System.out.println("Bin TEST:                                                                                         BASELINE:");

    			for(int i = 0; i < testHistogram.length; i++) {
					if (i < baselineHistogram.length) {
						System.out.println(String.format("%-2d", (i + 1)) + " " + String.format("| %90s |  %90s",
								StringUtils.repeat("*", Math.min(90, (int) testHistogram[i])),
								StringUtils.repeat("*", Math.min(90, (int) baselineHistogram[i]))));
					} else {
						System.out.println(String.format("%-2d", (i + 1)) + " " + String.format("| %90s |  (?)",
								StringUtils.repeat("*", Math.min(90, (int) testHistogram[i]))));

					}
    			}    			
        	} else {
                float ourPercentage = 
        				((float)((this.resultSummary[m][platformDim.TEST.ordinal()] + 
        						this.resultSummary[m][platformDim.TIE.ordinal()])
        				/ (float)total)) * 100;

        		// for the other two metrics (has results and matches), require 100% and 80%+ respectively
        		if(m == metricsDim.hasResults.ordinal()) {
            		if(ourPercentage < 95) {
                		System.out.print(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 95%+]");
            		} else {
                		System.out.print(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
            		}
        		} else if(m == metricsDim.topMatch.ordinal()) {
            		if(ourPercentage < 60) {
                		System.out.print(" [FAIL; have " + String.format("%.0f",  ourPercentage) + "% need 60%+]");
            		} else {
                		System.out.print(" [PASS with " + String.format("%.0f",  ourPercentage) + "%]");
            		}
        		} else {
        			System.out.println("");
        		}
        	}
        	
        	System.out.println("");
       	}
    	
    	System.out.println("** 'no result' matches are included.\n");
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
    	    	
        	for(int m = 0; m < metricsDim.values().length; m++) {
        		float total = this.resultSummary[m][platformDim.BASELINE.ordinal()] + 
        				this.resultSummary[m][platformDim.TEST.ordinal()] + 
        				this.resultSummary[m][platformDim.TIE.ordinal()];

        		float ourPercentage = 
                		((float)((this.resultSummary[m][platformDim.TEST.ordinal()] + 
                				this.resultSummary[m][platformDim.TIE.ordinal()])
                		/ (float)total)) * 100;

                // for each optimization, require our result to be the winner 80% of the time
        		if(m == metricsDim.T.ordinal() || m == metricsDim.W.ordinal() || m == metricsDim.X.ordinal()) {
            		results.add(DynamicTest.dynamicTest("MultiDim: Metric "  + extractTestName(TEST_RESULTS_TXT) + ":" + metricsDimLabels[m] + ""
            				+ " > System under test equal to/greater on optimization vs. baseline >= 95%, " + ourPercentage, new Executable() {
            			@Override
            			public void execute() throws Throwable {
            				assertTrue(ourPercentage >= 95f);	
            			}
                   	}));

            	} else {
            		// for the other two metrics (has results and matches), require 100% and 80%+ respectively
            		if(m == metricsDim.hasResults.ordinal()) {
                		results.add(DynamicTest.dynamicTest("MultiDim: Metric "  + extractTestName(TEST_RESULTS_TXT) + ":" + metricsDimLabels[m] + " "
                				+ "> System under test provided a result >= 98%, " + ourPercentage, new Executable() {
                			@Override
                			public void execute() throws Throwable {
                				assertTrue(ourPercentage >= 98f);	
                			}
                       	}));

            		} else if(m == metricsDim.topMatch.ordinal()) {
                		results.add(DynamicTest.dynamicTest("MultiDim: Metric "  + extractTestName(TEST_RESULTS_TXT) + ":" + metricsDimLabels[m] + " "
                				+ "> System under test matches baseline >= 90%, " + ourPercentage, new Executable() {
                			@Override
                			public void execute() throws Throwable {
                				assertTrue(ourPercentage >= 90);	
                			}
                       	}));
            		}
            	}
    	}
   
       	return results;
    }

	private String extractTestName(String fileName) {
		String[] parts = fileName.split("/");
		if (parts.length < 2) return fileName;
		String accessiblePostFix = (parts[parts.length-1].contains("accessible")? "-ada":"");
		return parts[parts.length-2] + accessiblePostFix;
	}

	public static long[] calcHistogram(double[] data, double min, double max, int numBins) {
    	  final long[] result = new long[numBins];
    	  final double binSize = (max - min)/numBins;

    	  for (double d : data) {
    	    int bin = (int) ((d - min) / binSize);
    	    if (bin < 0) { /* this data is smaller than min */ }
    	    else if (bin >= numBins) { /* this data point is bigger than max */ }
    	    else {
    	      result[bin] += 1;
    	    }
    	  }
    	  return result;
    	}
}
