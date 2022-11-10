package org.opentripplanner.index.graphql.datafetchers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.opentripplanner.api.model.*;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.index.model.*;
import org.opentripplanner.routing.core.*;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.spt.GraphPath;
import org.joda.time.DateTime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.resource.AccessibilityResource;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.api.resource.NearbySchedulesResource;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.datafetchers.GraphQLQueryTypeInputs.*;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLNyMtaAdaFlag;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.Router;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQLQueryTypeImpl implements GraphQLDataFetchers.GraphQLQueryType {

	private static final Logger LOG = LoggerFactory.getLogger(GraphQLQueryTypeImpl.class);

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
			GraphQLQueryTypeAlertsArgsInput input = 
				new GraphQLQueryTypeAlertsArgsInput(environment.getArguments());
		
			if(input.getGraphQLFeeds() != null) {
				List<String> ids = StreamSupport.stream(input.getGraphQLFeeds().spliterator(), false)
					.collect(Collectors.toList());

				return getRouter(environment).graph.getAlertPatches()
						.filter(c -> ids.stream().anyMatch(inputItem -> inputItem.equals(c.getFeedId())))
						.collect(Collectors.toList());			

			} else {			
				return getRouter(environment).graph.getAlertPatches()
					.collect(Collectors.toList());			
			}	
		};
	}
	
	@Override
	public DataFetcher<Object> feed() {
		return environment -> getGraphIndex(environment)
	    		.feedInfoForId
	    		.get(new GraphQLQueryTypeFeedArgsInput(environment.getArguments()).getGraphQLId());
	}

	@Override
	public DataFetcher<Iterable<Object>> feeds() {
	    return environment -> getGraphIndex(environment)
	    		.feedInfoForId.values().stream()
	    		.collect(Collectors.toList());
	}

	@Override
	public DataFetcher<Object> agency() {
	    return environment -> getGraphIndex(environment)
	            .getAgencyWithoutFeedId(new GraphQLQueryTypeAgencyArgsInput(environment.getArguments()).getGraphQLId());
	}

	@Override
	public DataFetcher<Iterable<Object>> agencies() {
	    return environment -> getGraphIndex(environment).getAllAgencies().stream()
	    		.collect(Collectors.toList());
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
		return environment -> {
			GraphQLQueryTypeStopsArgsInput input = 
					new GraphQLQueryTypeStopsArgsInput(environment.getArguments());

			if(input.getGraphQLMtaStationId() != null) {
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayStations.get("Station ID").get(new AgencyAndId("MTASBWY", input.getGraphQLMtaStationId()));
				if(records == null || records.isEmpty())
					throw new Exception("Station ID was not found.");
				
				List<AgencyAndId> ids = new ArrayList<AgencyAndId>();

				records
					.stream()
					.map(record ->
						new AgencyAndId("MTASBWY", record.get("GTFS Stop ID"))
					).forEach(it -> {
						ids.addAll(getGraphIndex(environment).stopsForParentStation.get(it)
								.stream()
								.map(stop -> { return stop.getId(); } )
								.collect(Collectors.toList()));					
					});
				
			    return getGraphIndex(environment).stopForId.values().stream()
			    		.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().equals(inputItem)))
						.distinct()
						.collect(Collectors.toList());	
				
			} else if(input.getGraphQLMtaComplexId() != null) {
				// this extra check is here because there are some Complex IDs in the Stations file that we should not respond to,
				// yet the Stations file is the one that maps the IDs to GTFS IDs
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayComplexes.get("Complex ID").get(new AgencyAndId("MTASBWY", input.getGraphQLMtaComplexId()));
				if(records == null || records.isEmpty())
					throw new Exception("Complex ID was not found.");

				// now map the valid Complex ID to GTFS IDs
				records = getGraphIndex(environment).mtaSubwayStations.get("Complex ID").get(new AgencyAndId("MTASBWY", input.getGraphQLMtaComplexId()));
				if(records == null || records.isEmpty())
					throw new Exception("Complex ID was not found (2).");
				
				List<AgencyAndId> ids = new ArrayList<AgencyAndId>();
				records
					.stream()
					.map(record ->
						new AgencyAndId("MTASBWY", record.get("GTFS Stop ID"))
					).forEach(it -> {
						ids.addAll(getGraphIndex(environment).stopsForParentStation.get(it)
								.stream()
								.map(stop -> { return stop.getId(); } )
								.collect(Collectors.toList()));					
					});

			    return getGraphIndex(environment).stopForId.values().stream()
			    		.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().equals(inputItem)))
						.distinct()
						.collect(Collectors.toList());		
				
			} else if(input.getGraphQLGtfsIds() != null) {
				Stream<String> inputStream = StreamSupport.stream(input.getGraphQLGtfsIds().spliterator(), false);
				
				List<AgencyAndId> ids = new ArrayList<AgencyAndId>();
				input.getGraphQLGtfsIds().forEach(id -> {
					ids.add(AgencyAndId.convertFromString(id));
				});
				
				inputStream.forEach(it -> {
					AgencyAndId aid = AgencyAndId.convertFromString(it);
					ids.addAll(getGraphIndex(environment).stopsForParentStation.get(aid)
							.stream()
							.map(stop -> { return stop.getId(); } )
							.collect(Collectors.toList()));					
				});
				
			    return getGraphIndex(environment).stopForId.values().stream()
			    		.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().equals(inputItem)))
						.distinct()
						.collect(Collectors.toList());			

			} else {
			    return getGraphIndex(environment).stopForId.values().stream()
						.distinct()
						.collect(Collectors.toList());			
			}
		};
	}
	
	@Override
	public DataFetcher<Object> stop() {
		return environment -> {
			GraphQLQueryTypeStopArgsInput input = 
					new GraphQLQueryTypeStopArgsInput(environment.getArguments());
			
			if(input.getGraphQLGtfsId() == null)
				throw new Exception("GTFS ID must be given. Use stops() if you want to provide a complex or station.");
				
			AgencyAndId theStop = AgencyAndId.convertFromString(input.getGraphQLGtfsId());

			if(!getGraphIndex(environment).stopForId.containsKey(theStop))
				throw new Exception("The requested stop ID must be or resolve to a GTFS platform. e.g. MTASBWY_R14S, not a station e.g. MTASBWY_R14");
			
			return getGraphIndex(environment).stopForId.get(theStop);				
		};
	}

	@Override
	public DataFetcher<Iterable<Object>> routes() {
		return environment -> {
			GraphQLQueryTypeRoutesArgsInput input = 
					new GraphQLQueryTypeRoutesArgsInput(environment.getArguments());
	    		    	
			if(input.getGraphQLGtfsIds() != null) {
				Stream<String> inputStream = StreamSupport.stream(input.getGraphQLGtfsIds().spliterator(), false);
				
				return getGraphIndex(environment)
						.routeForId.values().stream()
						.filter(c -> inputStream.anyMatch(inputItem -> inputItem.equals(AgencyAndId.convertToString(c.getId()))))
						.distinct()
						.collect(Collectors.toList());				
			
			} else if(input.getGraphQLName() != null) {
				return getGraphIndex(environment)
						.routeForId.values().stream()
						.filter(c -> c.getShortName().equals(input.getGraphQLName()) || c.getLongName().equals(input.getGraphQLName()))
						.distinct()
						.collect(Collectors.toList());
				
			} else {			
				return getGraphIndex(environment)
						.routeForId.values().stream()
						.distinct()
						.collect(Collectors.toList());
			}
		};
	}

	@Override
	public DataFetcher<Object> route() {
	    return environment -> {
			return getGraphIndex(environment)
					.routeForId.get(GtfsLibrary.convertIdFromString(
							new GraphQLQueryTypeRouteArgsInput(environment.getArguments()).getGraphQLGtfsId()));
	    };
	}
	
	@Override
	public DataFetcher<Object> accessibility() {
		return environment -> {
			GraphQLQueryTypeStopAccessibilityArgsInput input =
					new GraphQLQueryTypeStopAccessibilityArgsInput(environment.getArguments());

			String mtaAdaAccessible = null;
			String mtaAdaNotes = null;
			List<AgencyAndId> queries = null;			
			if(input.getGraphQLMtaComplexId() != null) {
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayComplexes.get("Complex ID").get(new AgencyAndId("MTASBWY", input.getGraphQLMtaComplexId()));
				if(records == null || records.size() != 1)
					throw new Exception("Complex ID was not found.");

				mtaAdaAccessible = GraphQLNyMtaAdaFlag.values()[Integer.parseInt(records.get(0).get("ADA"))].name();
				mtaAdaNotes = records.get(0).get("ADA Notes");

				records = getGraphIndex(environment).mtaSubwayStations.get("Complex ID").get(new AgencyAndId("MTASBWY", input.getGraphQLMtaComplexId()));
				if(records == null)
					throw new Exception("Complex ID was not found (2).");
				
				List<String> ids = new ArrayList<String>();
				for(HashMap<String, String> record : records) {
					ids.addAll(getGraphIndex(environment).stopsForParentStation
							.get(new AgencyAndId("MTASBWY", record.get("GTFS Stop ID")))
							.stream()
							.map(it -> { return AgencyAndId.convertToString(it.getId()); })
							.collect(Collectors.toList()));
				}

				queries = getGraphIndex(environment)
						.stopForId.keySet().stream()
						.filter(c -> ids.stream().anyMatch(inputItem -> AgencyAndId.convertToString(c).equals(inputItem)))
						.collect(Collectors.toList());				
				
			} else	if(input.getGraphQLMtaStationId() != null) {
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayStations.get("Station ID").get(new AgencyAndId("MTASBWY", input.getGraphQLMtaStationId()));
				if(records == null || records.size() != 1)
					throw new Exception("Station ID was not found.");

				mtaAdaAccessible = GraphQLNyMtaAdaFlag.values()[Integer.parseInt(records.get(0).get("ADA"))].name();
				mtaAdaNotes = records.get(0).get("ADA Notes");

				List<String> ids = new ArrayList<String>();
				for(HashMap<String, String> record : records) {
					ids.addAll(getGraphIndex(environment).stopsForParentStation
							.get(new AgencyAndId("MTASBWY", record.get("GTFS Stop ID")))
							.stream()
							.map(it -> { return AgencyAndId.convertToString(it.getId()); })
							.collect(Collectors.toList()));
				}
				
				queries = getGraphIndex(environment)
						.stopForId.keySet().stream()
						.filter(c -> ids.stream().anyMatch(inputItem -> AgencyAndId.convertToString(c).equals(inputItem)))
						.collect(Collectors.toList());						
			} else {
				List<AgencyAndId> ids = new ArrayList<AgencyAndId>();
				
				AgencyAndId aid = AgencyAndId.convertFromString(input.getGraphQLGtfsId());
				ids.addAll(getGraphIndex(environment).stopsForParentStation.get(aid)
							.stream()
							.map(stop -> { return stop.getId(); } )
							.collect(Collectors.toList()));									

				if(ids.isEmpty())
					throw new Exception("GTFS Station ID not found.");

				ArrayList<HashMap<String, String>> records = 
						getGraphIndex(environment).mtaSubwayStations.get("GTFS Stop ID").get(aid);
				if(records == null || records.size() != 1)
					throw new Exception("GTFS Station ID not found (2).");

				mtaAdaAccessible = GraphQLNyMtaAdaFlag.values()[Integer.parseInt(records.get(0).get("ADA"))].name();
				mtaAdaNotes = records.get(0).get("ADA Notes");

				queries = getGraphIndex(environment).stopForId.keySet().stream()
						.filter(c -> ids.stream().anyMatch(inputItem -> c.equals(inputItem)))
						.collect(Collectors.toList());			
			}
		
			AccessibilityResource ar = new AccessibilityResource(getRouter(environment), getGraphIndex(environment));
			ar.date = input.getGraphQLDate();
			ar.ignoreRealtimeUpdates = input.getGraphQLIncludeRealtime() != null ? !input.getGraphQLIncludeRealtime() : false;
			
			HashMap<String, Object> result = new HashMap<String, Object>();
			result.put("pairs", ar.getPairwiseAccessibility(queries));
			result.put("mtaAdaAccessible", mtaAdaAccessible);			
			result.put("mtaAdaAccessibleNotes", mtaAdaNotes);			
			return result;
		};
	}

	@Override
	public DataFetcher<Iterable<Object>> trips() {
	    return environment -> {
	    	FeedInfo f = environment.getSource();

	    	ArrayList<String> agencyIdsToInclude = new ArrayList<String>();
	    	
	    	agencyIdsToInclude.addAll(getGraphIndex(environment).agenciesForFeedId.get(f.getId()).keySet());

	    	// feed and agency ID are used interchangably (a bug) by clients, 
	    	// so add the feedID to the list of agency IDs to look for
	    	agencyIdsToInclude.add(f.getId());
	    	
	    	return getGraphIndex(environment).tripForId.values()
	    			.stream()
	    			.filter(it -> agencyIdsToInclude.contains(it.getId().getAgencyId()))
	    			.distinct()
	    			.collect(Collectors.toList());
	    };
	}

	@Override
	public DataFetcher<Object> trip() {
	    return environment -> {
			return getGraphIndex(environment)
					.tripForId.get(GtfsLibrary.convertIdFromString(
							new GraphQLQueryTypeTripArgsInput(environment.getArguments()).getGraphQLGtfsId()));
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> nearby() {
		return environment -> {
			GraphQLQueryTypeNearbyArgsInput input = 
					new GraphQLQueryTypeNearbyArgsInput(environment.getArguments());

			NearbySchedulesResource nsr = new NearbySchedulesResource(getRouter(environment));
			nsr.date = input.getGraphQLDate();
			nsr.time = input.getGraphQLTime();

			nsr.stopsStr = input.getGraphQLGtfsStopIdList();
			nsr.routesStr = input.getGraphQLRoutesList();
			nsr.stoppingAt = input.getGraphQLStoppingAtGtfsStopId();

			nsr.lat = (Double)input.getGraphQLLatitude();
			nsr.lon = (Double)input.getGraphQLLongitude();
			nsr.radius = (Double)input.getGraphQLRadius();

			nsr.maxStops = input.getGraphQLMaxStops();
			nsr.minStops = input.getGraphQLMinStops();
			
			nsr.numberOfDepartures = input.getGraphQLNumberOfDepartures();
			nsr.timeRange = input.getGraphQLTimeRange();

			nsr.omitNonPickups = input.getGraphQLOmitNonPickups();
			nsr.showCancelledTrips = input.getGraphQLShowCancelledTrips();
			nsr.trackIds = input.getGraphQLTracksList();

			nsr.tripHeadsign = input.getGraphQLTripHeadsign();
			nsr.direction = input.getGraphQLDirection();			
			nsr.groupByParent = input.getGraphQLGroupByParent();
			nsr.includeStopsForTrip = input.getGraphQLIncludeStopsForTrip();
			nsr.signMode = input.getGraphQLSignMode();
			
			// controls how sub elements display arrivals (realtime or scheduled); see ...StopImpl.java
			environment.<GraphQLRequestContext>getContext().setSignMode(nsr.signMode);
			
			return nsr.getNearbySchedules().stream().collect(Collectors.toList());
		};
	}

	@Override
	public DataFetcher<Iterable<Object>> schedule() {
		return environment -> {
			GraphQLScheduleImpl graphQLScheduleImpl = new GraphQLScheduleImpl();
			return graphQLScheduleImpl.getSchedule(environment);
		};
	}

	@Override
	public DataFetcher<Iterable<Object>> recentTrips() {
		return environment -> {
			GraphQLRecentTripsImpl graphQLRecentTripsImpl = new GraphQLRecentTripsImpl();
			return graphQLRecentTripsImpl.getRecentTrips(environment);
		};
	}



	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
