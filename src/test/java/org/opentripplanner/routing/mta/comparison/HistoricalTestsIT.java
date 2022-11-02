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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.AddTransitModelEntitiesToGraph;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.BicycleOptimizeType;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.mta.comparison.test_file_format.ItinerarySummary;
import org.opentripplanner.routing.mta.comparison.test_file_format.Result;
import org.opentripplanner.routing.spt.GraphPath;

import org.opentripplanner.standalone.OTPMain;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.configure.OTPAppConstruction;
import org.opentripplanner.standalone.configure.OTPConfiguration;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.standalone.server.RouterService;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;
import static org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper.generateItinerary;

public class HistoricalTestsIT extends RoutingResource {
	
    private static final Logger LOG = LoggerFactory.getLogger(HistoricalTestsIT.class);

	private static String ALL_TESTS_DIR = "src/test/resources/mta/comparison/"; 

	private Graph graph;
	
	private Router router;

	@Context
	protected OTPServer otpServer;
	
	@BeforeAll
	private static void syncS3ToDisk() {
		
		LOG.info("Starting sync to disk from S3...");
		
		try {
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                    .withCredentials(new DefaultAWSCredentialsProviderChain())
                    .withRegion("us-east-1")
                    .build();
            
//            AssumeRoleRequest roleRequest = new AssumeRoleRequest()
//            		.withRoleArn("arn:aws:iam::347059689224:role/mta-otp-integration-test-bundle")
//            		.withRoleSessionName(UUID.randomUUID().toString());
//
//            AssumeRoleResult roleResponse = stsClient.assumeRole(roleRequest);

			//BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials("AKIAVBTS6S4ENFAZF66V", "o2LGU+RTBtTngPF8TnSMzRwhDPrjhia/pF0AUjVz");

			BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials("AKIAJR6QRHZLCABYKPSQ", "vFNCfEFkm6kd2CuAZbFj2r7RRtbdkBT0lFyC7iWG");

            
//            BasicSessionCredentials awsCredentials = new BasicSessionCredentials(
//					basicAWSCredentials);

			AmazonS3ClientBuilder.standard()
		            .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
		            .build();

			LOG.info("Got credentials.");

			File f = new File(ALL_TESTS_DIR);

			LOG.info("Starting xfer.");

			TransferManager tm = TransferManagerBuilder.standard().build();
		    MultipleFileDownload x = tm.downloadDirectory("mta-otp-integration-test-bundles", null, f);
		    x.waitForCompletion();
		    tm.shutdownNow();

			LOG.info("Complete.");

		} catch (AmazonClientException | InterruptedException e) {
			LOG.error("Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static Graph buildOsmGraph(String osmPath) {

		try {
			var graph = new Graph();
			// Add street data from OSM
			File osmFile = new File(osmPath);
			BinaryOpenStreetMapProvider osmProvider =
					new BinaryOpenStreetMapProvider(osmFile, true);
			OpenStreetMapModule osmModule =
					new OpenStreetMapModule(Lists.newArrayList(osmProvider));
			osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
			osmModule.skipVisibility = true;
			osmModule.buildGraph(graph, new HashMap<>());
			return graph;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void buildGraph(File graphDir) {
		LOG.info("Starting graph build for dir=" + graphDir);

		//GraphBuilder builder = GraphBuilder.forDirectory(new CommandLineParameters(), graphDir);


		Graph graph = null;
		CommandLineParameters params = new CommandLineParameters();
		params.baseDirectory = Collections.singletonList(new File("/"));
		OTPAppConstruction app = new OTPAppConstruction(params);
//
//		// Validate data sources, command line arguments and config before loading and
//		// processing input data to fail early
//		app.validateConfigAndDataSources();
//
		/* Load graph from disk if one is not present from build. */
		//if (params.doLoadGraph() || params.doLoadStreetGraph()) {
//			DataSource inputGraph = params.doLoadGraph()
//					? app.store().getGraph()
//					: app.store().getStreetGraph();
//		DataSource inputGraph = app.store().getGraph();
//			SerializedGraphObject obj = SerializedGraphObject.load(inputGraph);
//			graph = obj.graph;
//			app.config().updateConfigFromSerializedGraph(obj.buildConfig, obj.routerConfig);
		//}
//		try {
//			graph = Graph.load(new File("src/test/java/org/opentripplanner/routing/mta/graph.obj"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		GraphBuilder builder = app.createGraphBuilder(buildOsmGraph(ConstantsForTests.HERRENBERG_OSM));
		builder.run();
		builder.getGraph();

		LOG.info("Success.");
	}
	
	private Graph loadGraph(File path) throws Exception {
		File file = OtpDataStore.graphFile(path);
		Graph graph = SerializedGraphObject.load(file);
		if(graph == null) { throw new IllegalStateException(); }
		graph.index();
		return graph;
	}
	
    private List<File> findTestDirs() {
    	List<File> data = new ArrayList<File>();
    
    	File allTests = new File(ALL_TESTS_DIR);
    	for(String testDirPath : allTests.list()) {
			//File testDir = new File(ALL_TESTS_DIR + "/" + "03-24-2021-lirr-test");
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
			
    		long epoch = result.query.time;//1616613120000L


    		// shift dates by +1 week to try to match the
    		// same service period
    		Calendar c = Calendar.getInstance();
			c.setTime(new Date(epoch));

			Calendar target = Calendar.getInstance();
			target.set(2021,3,24);
    		while(epoch < target.getTimeInMillis()) {
    			c.add(Calendar.DAY_OF_MONTH, 7);
        		epoch = c.getTimeInMillis();
    		}

    		DateTimeFormatter dateF = DateTimeFormat.forPattern("MM-dd-YYYY");
    		DateTimeFormatter timeF = DateTimeFormat.forPattern("hh:mm aa");
    		this.date = new DateTime(epoch).toString(dateF);
    		this.time = new DateTime(epoch).toString(timeF);
			
			this.modes = new QualifiedModeSet("TRANSIT,WALK");
			this.maxWalkDistance = 8047.0;
			this.ignoreRealtimeUpdates = true;

			//RoutingRequest request = router.defaultRoutingRequest.clone();
			RoutingRequest request = super.buildRequest(router.defaultRoutingRequest.clone(),graph.getTimeZone());
			//request.from = new GenericLocation(40.833427, -73.896773);
			//request.to = new GenericLocation(40.710923,-73.85347);
			request.transferCost = 600;
			request.carReluctance = 15;
			switch(result.query.optimizeFlag) {
    			case "W":
    				this.optimize = BicycleOptimizeType.QUICK;  //?
					request.walkReluctance = 15;
    				break;
    			case "X":
    				this.optimize = BicycleOptimizeType.TRANSFERS;
					request.transferCost = 1800;
    				break;
    			case "T":
    				this.optimize = BicycleOptimizeType.QUICK;
    				break;
	  		}

			this.optimize = null;





			// ##############
			
            String optimizeFlag = null;
    		switch(request.optimize) {
//			case WALKING:
//				optimizeFlag = "W";
//				break;
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
//				RoutingService routingService = new RoutingService(router.graph);
//				RoutingResponse res = routingService.route(request, router);
//
//	  			TripPlan plan = res.getTripPlan();


				//V1
//				GraphPathFinder gpFinder = new GraphPathFinder(router);
//				List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(request);
//
//
//
//				TripPlan plan = TripPlanMapper.mapTripPlan(request, GraphPathToItineraryMapper.mapItineraries(paths,request));

				//v2
				RoutingService routingService = new RoutingService(router.graph);
				RoutingResponse res = routingService.route(request, router);

				TripPlan plan = res.getTripPlan();

				int i = 1;
                for(Itinerary itin : plan.itineraries) {
                	ItinerarySummary is = ItinerarySummary.fromItinerary(itin);
                	is.itineraryNumber = i;
                	i++;
                    
                	resultsFileWriter.write(
                     		 "S " + is.itineraryNumber + " " + String.format("%.2f",is.walkDistance)
                     		 		+ " " + is.transitTime + " " + is.routes + "\n");
                }
	  		} catch (Exception e) {
	            //if(!PlannerError.isPlanningError(e.getClass()))
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

				graph = loadGraph(new File("/Users/msalvatore/Projects/dev2.xFork/src/test/resources/integration_test_gtfs/" + testDir.getName()));
				router = new Router(graph, RouterConfig.DEFAULT);
				graph.index();
				router.startup();

    		} catch(Exception e) {
    			System.out.println("Graph failed to load with exception " + e.getMessage() + ". Rebuilding...");

    			new File(testDir + "/graph/graph.obj").delete();
    			graph = null;
    		} finally {
    			if(graph == null) {
    				//buildGraph(new File(testDir + "/graph"));
    				//loadGraph(new File(testDir + "/graph"));
					System.out.println("graph load failed, exiting");
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

			 //free up graph memory
			if(router != null)
				router.shutdown();
		}

		return generatedTests;
	}
    
}

