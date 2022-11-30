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

import com.amazonaws.auth.*;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import org.opentripplanner.routing.mta.comparison.test_file_format.Result;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.graph_builder.GraphBuilder;

import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPMain;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;

public class HistoricalTestsIT extends RoutingResource {
	
    private static final Logger LOG = LoggerFactory.getLogger(HistoricalTestsIT.class);

	private static String ALL_TESTS_DIR = "src/test/resources/mta/comparison/"; 

	private Graph graph;
	
	private Router router;
	
	@BeforeAll
	private static void syncS3ToDisk() {
		
		LOG.info("Starting sync to disk from S3...");
		String assumeRoleArn = "arn:aws:iam::347059689224:role/mta-otp-integration-test-bundle";

		if (System.getProperty("assumeRoleArn") != null) {
			assumeRoleArn = System.getProperty("assumeRoleArn");
		}

		String bucketName = "mta-otp-integration-test-bundles";
		if (System.getProperty("bucketName") != null) {
			bucketName = System.getProperty("bucketName");
		}
		String accessKey = System.getProperty("accessKey");
		String secretKey = System.getProperty("secretKey");

		try {
			TransferManager tm;
			if (accessKey != null && secretKey != null) {
				LOG.info("using S3 directly with accessKey '" + accessKey);
				AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
				AmazonS3ClientBuilder.standard()
						.withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
						.build();
				tm = TransferManagerBuilder.standard().withS3Client(AmazonS3ClientBuilder.standard()
						.withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build()).build();
			} else {
				LOG.info("attempting to assume role " + assumeRoleArn + " to bucket " + bucketName);
				AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
						.withCredentials(new DefaultAWSCredentialsProviderChain())
						.withRegion("us-east-1")
						.build();

				AssumeRoleRequest roleRequest = new AssumeRoleRequest()
						.withRoleArn(assumeRoleArn)
						.withRoleSessionName(UUID.randomUUID().toString());

				AssumeRoleResult roleResponse = stsClient.assumeRole(roleRequest);
				Credentials sessionCredentials = roleResponse.getCredentials();

				BasicSessionCredentials awsCredentials = new BasicSessionCredentials(
						sessionCredentials.getAccessKeyId(),
						sessionCredentials.getSecretAccessKey(),
						sessionCredentials.getSessionToken());

				AmazonS3ClientBuilder.standard()
						.withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
						.build();
				tm = TransferManagerBuilder.standard().build();
			}

			LOG.info("Got credentials.");

			File f = new File(ALL_TESTS_DIR);

			LOG.info("Starting xfer from s3://" + bucketName);

		    MultipleFileDownload x = tm.downloadDirectory(bucketName, null, f);
			x.addProgressListener(new ProgressListener() {
				@Override
				public void progressChanged(ProgressEvent progressEvent) {
					LOG.debug("download: " + progressEvent.toString());
				}
			});
		    x.waitForCompletion();
		    tm.shutdownNow();

			LOG.info("Complete.");

		} catch (AmazonClientException | InterruptedException e) {
			LOG.error("Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void buildGraph(File graphDir) {
		LOG.info("Starting graph build for dir=" + graphDir);

		GraphBuilder builder = GraphBuilder.forDirectory(new CommandLineParameters(), graphDir);
		builder.run();
		builder.getGraph();

		LOG.info("Success.");
	}
	
	private void loadGraph(File graphDir) throws Exception {
		File graphFile = new File(graphDir + "/Graph.obj");
		if(graphFile.exists()) {
			LOG.info("Graph file is present, trying to load it...");

			graph = Graph.load(graphFile);
			
			LOG.info("Initializing router...");
    		router = new Router(graphDir.getParent(), graph);
    		LOG.info("Complete");
    		
    		LOG.info("Calling graph startup to load JSON...");
    		router.startup(OTPMain.loadJson(new File(graphDir, Router.ROUTER_CONFIG_FILENAME)));
    		LOG.info("Complete");
		}
	}
	
    private List<File> findTestDirs() {
    	List<File> data = new ArrayList<File>();
    
    	File allTests = new File(ALL_TESTS_DIR);
    	for(String testDirPath : allTests.list()) {
    		File testDir = new File(ALL_TESTS_DIR + "/" + testDirPath);    		
    		if(!testDir.isDirectory())
    			continue;
    		
    		LOG.info("Found test directory " + testDir);

        	data.add(testDir); 
    	}
    	
    	return data;
    }
    	  
    private void runThroughGraph(File input, File output) throws Exception {
		FileWriter resultsFileWriter = new FileWriter(output);
		
		LOG.info("Loading test ideals from " + input);

		List<Result> ideals = Result.loadResults(input);			
		for(Result result : ideals) {
			this.fromPlace = result.query.origin;
			this.toPlace = result.query.destination;
			this.wheelchair = result.query.accessible;
			
    		long epoch = result.query.time;

/*    		
    		// shift dates by +1 week to try to match the 
    		// same service period
    		Calendar c = Calendar.getInstance();
			c.setTime(new Date(epoch));
    			
    		while(epoch < DateTime.now().getMillis()) {
    			c.add(Calendar.DAY_OF_MONTH, 7);    				
        		epoch = c.getTimeInMillis();
    		}			
*/			
    		DateTimeFormatter dateF = DateTimeFormat.forPattern("MM-dd-YYYY");
    		DateTimeFormatter timeF = DateTimeFormat.forPattern("hh:mm aa");
    		this.date = new DateTime(epoch).toString(dateF);
    		this.time = new DateTime(epoch).toString(timeF);
			
			this.modes = new QualifiedModeSet("TRANSIT,WALK");
			this.maxWalkDistance = 500.0;
			this.ignoreRealtimeUpdates = true;

			switch(result.query.optimizeFlag) {
    			case "W":
    				this.optimize = OptimizeType.WALKING; 
    				break;
    			case "X":
    				this.optimize = OptimizeType.TRANSFERS;
    				break;
    			case "T":
    				this.optimize = OptimizeType.QUICK;
    				break;
	  		}
	  		  		
			RoutingRequest request = 
					super.buildRequest(router.defaultRoutingRequest, graph.getTimeZone());

			// ##############
			
            String optimizeFlag = null;
    		switch(request.optimize) {
			case WALKING:
				optimizeFlag = "W";
				break;
			case TRANSFERS:
				optimizeFlag = "X";
				break;
			case QUICK:
				optimizeFlag = "T";
				break;
			default:
				break;
    		}
    		
            resultsFileWriter.write("Q " + ((request.wheelchairAccessible) ? "Y " : "N ") + 
    				request.dateTime*1000 + " " + 
    				request.from.lat + "," + request.from.lng + " " + 
    				request.to.lat + "," + request.to.lng + " " + 
    				optimizeFlag + 
    				"\n");
            
	  		try {
	  			GraphPathFinder gpFinder = new GraphPathFinder(router);
	  			List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(request);

	  			TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);

                int i = 1;
                for(Itinerary itin : plan.itinerary) {
                	ItinerarySummary is = ItinerarySummary.fromItinerary(itin);
                	is.itineraryNumber = i;
                	i++;
                    
                	resultsFileWriter.write(
                     		 "S " + is.itineraryNumber + " " + String.format("%.2f",is.walkDistance)
                     		 		+ " " + is.transitTime + " " + is.routes + "\n");
                }
	  		} catch (Exception e) {
	            if(!PlannerError.isPlanningError(e.getClass()))
	                LOG.warn("Error while planning path: ", e);

	  			resultsFileWriter.write("**** NOT FOUND ****\n");
	  		}
            
		} // for result

		resultsFileWriter.close();
    }
    
    private void runQueries(File testDir) throws Exception {    	
    	if(!testDir.isDirectory())
    		return;
    		
			File idealFile = new File(testDir + "/ideal.txt");
			File idealResultsFile = new File(testDir + "/ideal_results.txt");
			if(idealFile.exists())
				runThroughGraph(idealFile, idealResultsFile);
			
			File baselineFile = new File(testDir + "/baseline.txt");
			File baselineResultsFile = new File(testDir + "/baseline_results.txt");
			if(baselineFile.exists())
				runThroughGraph(baselineFile, baselineResultsFile);
	
			File baselineAccessibleFile = new File(testDir + "/baseline-accessible.txt");
			File baselineAccessibleResultsFile = new File(testDir + "/baseline-accessible_results.txt");
			if(baselineAccessibleFile.exists())
				runThroughGraph(baselineAccessibleFile, baselineAccessibleResultsFile);
    }
    
	@TestFactory
	@Test
	public Collection<DynamicTest> runTests() throws Exception {		
		List<DynamicTest> generatedTests = new ArrayList<>();

		for(File testDir : this.findTestDirs()) {
			graph = null;
			router = null;
			
			System.out.println("***************************************************************");
    		System.out.println("                TEST DIR: " + testDir.getName());
    		System.out.println("***************************************************************");

    		try {
    			loadGraph(new File(testDir + "/graph"));
    		} catch(Exception e) {
    			System.out.println("Graph failed to load with exception " + e.getMessage() + ". Rebuilding...");

    			new File(testDir + "/graph/Graph.obj").delete();
    			graph = null;
    		} finally {
    			if(graph == null) {
    				buildGraph(new File(testDir + "/graph"));
    				loadGraph(new File(testDir + "/graph"));
    			}
    		}
    		
    		if(graph == null) {
    			throw new Exception("Graph could not be loaded or rebuilt.");
    		}
    		
    		runQueries(testDir);
			
			File idealFile = new File(testDir.getAbsolutePath() + "/ideal.txt");
			File idealResultsFile = new File(testDir.getAbsolutePath() + "/ideal_results.txt");
			if(idealFile.exists() && idealResultsFile.exists()) {			
				ScoreAgainstIdealComparison t2 = new ScoreAgainstIdealComparison();
				t2.setIdealFile(idealFile.getPath());
				t2.setTestResultsFile(idealResultsFile.getPath());			
	    		generatedTests.addAll(t2.getTests());
			}
			
			File baselineFile = new File(testDir.getAbsolutePath() + "/baseline.txt");
			File baselineResultsFile = new File(testDir.getAbsolutePath() + "/baseline_results.txt");
			if(baselineFile.exists() && baselineResultsFile.exists()) {
				System.out.println("***************************************************************");
	    		System.out.println("                       NOT ACCESSIBLE");
	    		System.out.println("***************************************************************");

				QualitativeMultiDimInstanceComparison t1 = new QualitativeMultiDimInstanceComparison();
				t1.setBaselineResultsFile(baselineFile.getPath());
				t1.setTestResultsFile(baselineResultsFile.getPath());
				generatedTests.addAll(t1.getTests());
			}

			File baselineAccessibleFile = new File(testDir.getAbsolutePath() + "/baseline-accessible.txt");
			File baselineAccessibleResultsFile = new File(testDir.getAbsolutePath() + "/baseline-accessible_results.txt");
			if(baselineAccessibleFile.exists() && baselineAccessibleResultsFile.exists()) {
				System.out.println("***************************************************************");
	    		System.out.println("                        ACCESSIBLE");
	    		System.out.println("***************************************************************");

	    		QualitativeMultiDimInstanceComparison t3 = new QualitativeMultiDimInstanceComparison();
				t3.setBaselineResultsFile(baselineAccessibleFile.getPath());
				t3.setTestResultsFile(baselineAccessibleResultsFile.getPath());
				generatedTests.addAll(t3.getTests());
			}

			// free up graph memory
			if(router != null)
				router.shutdown();
		}

		return generatedTests;
	}
    
}

