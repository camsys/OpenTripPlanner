package org.opentripplanner.index.graphql.datafetchers;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.analyst.batch.BasicPopulation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLLocationType;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLNyMtaAdaFlag;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLWheelchairBoarding;
import org.opentripplanner.index.model.*;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQLStopImpl implements GraphQLDataFetchers.GraphQLStop {

	private static final Logger LOG = LoggerFactory.getLogger(GraphQLStopImpl.class);


	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return AgencyAndId.convertToString(e.getId());
	    };
	}

	@Override
	public DataFetcher<String> gtfsIdWithColon() {
		return environment -> {
			Stop e = environment.getSource();
			return colonFormatAgency(e.getId());
		};
	}

	public String colonFormatAgency(AgencyAndId aid) {
		return aid == null ? null : aid.getAgencyId() + ':' + aid.getId();
	}

	@Override
	public DataFetcher<String> name() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getName();
	    };
	}

	@Override
	public DataFetcher<Double> lat() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getLat();
	    };	
	 }

	@Override
	public DataFetcher<Double> lon() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getLon();
	    };
	}

	@Override
	public DataFetcher<String> code() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getCode();
	    };
	}

	@Override
	public DataFetcher<String> desc() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getDesc();
	    };
	}

	@Override
	public DataFetcher<String> zoneId() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getZoneId();
	    };
	}

	@Override
	public DataFetcher<String> url() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getUrl();
	    };
	}

	@Override
	public DataFetcher<String> locationType() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return GraphQLLocationType.values()[e.getLocationType()].name();
	    };
	}

	@Override
	public DataFetcher<Object> parentStation() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	
	    	if(e.getParentStation() != null) 
	    		return getGraphIndex(environment).getParentStopForStop(e);	    	
	    	else 
	    		return null;
	    };
	}

	@Override
	public DataFetcher<String> wheelchairBoarding() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return GraphQLWheelchairBoarding.values()[e.getWheelchairBoarding()].name();
	    };
	}

	@Override
	public DataFetcher<String> direction() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getDirection();
	    };
	}

	@Override
	public DataFetcher<String> timezone() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getTimezone();
	    };
	}

	@Override
	public DataFetcher<Integer> vehicleType() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getVehicleType();
	    };
	}

	@Override
	public DataFetcher<String> platformCode() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getPlatformCode();
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	AgencyAndId stationId = e.getId();
	    	
	    	if(e.getLocationType() == Stop.LOCATION_TYPE_STATION)
	    		return getGraphIndex(environment).stopsForParentStation
	    			.get(stationId).stream()
	    			.distinct()
	    			.collect(Collectors.toList());
	    	else
	    		return null;
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stopsForMtaComplex() {
		 return environment -> {
		    	Stop e = environment.getSource();
		    	AgencyAndId gtfsId = e.getParentStation() != null ? new AgencyAndId(e.getId().getAgencyId(), e.getParentStation()) : e.getId();

		    	AgencyAndId complexId = new AgencyAndId(gtfsId.getAgencyId(), getGraphIndex(environment)
		    			.mtaSubwayStations
		    			.get("GTFS Stop ID")
		    			.get(gtfsId)
		    			.get(0)
		    			.get("Complex ID"));
		    	
		    	List<HashMap<String, String>> complexRecord = getGraphIndex(environment)
		    			.mtaSubwayStations
		    			.get("Complex ID")
		    			.get(complexId);
		    	
		    	if(complexRecord != null) {		    	
		    		List<AgencyAndId> gtfsIds = 
		    			complexRecord.stream().map(r -> { 
		    				return new AgencyAndId(e.getId().getAgencyId(), r.get("GTFS Stop ID"));
		    			})
		    			.collect(Collectors.toList());
		    			
		    		ArrayList<Stop> r = new ArrayList<Stop>();
		    		for(AgencyAndId stopId : gtfsIds) {
		    			Stop stop = getGraphIndex(environment).stopForId.get(stopId);
		    			if(stop != null)
			    			r.add(stop);

		    			Collection<Stop> stops = getGraphIndex(environment).stopsForParentStation.get(stopId);
		    			if(!stops.isEmpty())
				    		r.addAll(stops);		    			
		    		}
		    		return r.stream().collect(Collectors.toList());
		    	}
		    	
		    	return null;
 		 };
	}

	@Override
	public DataFetcher<Iterable<Object>> routes() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	
	    	if(e.getLocationType() == Stop.LOCATION_TYPE_STATION || e.getLocationType() == Stop.LOCATION_TYPE_STOP) {
				Map<String, Object> localContext = environment.getLocalContext();

				// case when we're nested inside a stopTime, e.g. in nearby's stopTime, return routes
				// that stop at this station within the next 30m
				if(localContext.containsKey("stop") && localContext.containsKey("trip")) {
					Trip trip = (Trip)localContext.get("trip");
					TripPattern pattern = getGraphIndex(environment).getTripPatternForTripId(trip.getId());
					Timetable timetable = getGraphIndex(environment).currentUpdatedTimetableForTripPattern(pattern);
					// (the triptimes that are used prior are not for the entire trip, so have to refetch here)
					TripTimes tripTimes = timetable.getTripTimes(trip);

					int stopIndex = pattern.getStops().indexOf(e);
					long ourArrivalEpochSeconds = (new ServiceDate().getAsDate().getTime()/1000) + tripTimes.getArrivalTime(stopIndex);

					Stop stop = e;
					long startTime = ourArrivalEpochSeconds;
					int timeRange = (int) TimeUnit.MINUTES.toSeconds(30);
					int numberOfDepartures = 1000;
					boolean omitNonPickups = true;
					boolean isSignMode = environment.<GraphQLRequestContext>getContext().getSignMode();

		    		// get arrivals at this stop within the following 30m of our arrival
					StopTimesForPatternsQuery query = new StopTimesForPatternsQuery
														.Builder(stop, startTime, timeRange, numberOfDepartures, omitNonPickups)
														.signMode(isSignMode)
														.build();

					Collection<StopTimesInPattern> stip = 
								getGraphIndex(environment).stopTimesForStop(query);

					return stip.stream()
							.map(r -> { return getGraphIndex(environment).routeForId.get(r.route.id); })
							.distinct()
							.collect(Collectors.toList());
				} else {
		    		return getGraphIndex(environment).routesForStop(e).stream()
			    			.distinct()
			    			.collect(Collectors.toList());
				}
	    	} else
	    		return null;
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> routesForMtaComplex() {
		 return environment -> {
		    	Stop e = environment.getSource();
		    	if(e.getLocationType() == Stop.LOCATION_TYPE_STATION || e.getLocationType() == Stop.LOCATION_TYPE_STOP) {
					Map<String, Object> localContext = environment.getLocalContext();
					GraphIndex graphIndex = getGraphIndex(environment);
					// case when we're nested inside a stopTime, e.g. in nearby's stopTime, return routes
					// that stop at this station within the next 30m
					if(localContext.containsKey("stop") && localContext.containsKey("trip")) {
						Trip trip = (Trip)localContext.get("trip");

						Set<Route> routes = graphIndex.routesForMtaComplexCache.getIfPresent(e.getMtaStopId() + "_" + trip.getId());
						if(routes == null){
							routes = new HashSet<>();

							TripPattern pattern = getGraphIndex(environment).getTripPatternForTripId(trip.getId());
							Timetable timetable = getGraphIndex(environment).currentUpdatedTimetableForTripPattern(pattern);
							TripTimes tripTimes = timetable.getTripTimes(trip);

							// (the triptimes that are used prior are not for the entire trip, so have to refetch here)
							int stopIndex = pattern.getStops().indexOf(e);
							long ourArrivalEpochSeconds = (new ServiceDate().getAsDate().getTime()/1000) + tripTimes.getArrivalTime(stopIndex);

							for(Object _stop : stopsForMtaComplex().get(environment)) {
								Stop stop = (Stop)_stop;

								// get arrivals at this stop within the following 30m of our arrival
								long startTime = ourArrivalEpochSeconds;
								int timeRange = (int) TimeUnit.MINUTES.toSeconds(30);
								int numberOfDepartures = 1000;
								boolean omitNonPickups = true;
								boolean isSignMode = environment.<GraphQLRequestContext>getContext().getSignMode();

								// get arrivals at this stop within the following 30m of our arrival
								StopTimesForPatternsQuery query = new StopTimesForPatternsQuery
										.Builder(stop, startTime, timeRange, numberOfDepartures, omitNonPickups)
										.signMode(isSignMode)
										.build();

								Collection<StopTimesInPattern> stip = getGraphIndex(environment).stopTimesForStop(query);

								routes.addAll(stip.stream()
										.map(r -> { return getGraphIndex(environment).routeForId.get(r.route.id); })
										.distinct()
										.collect(Collectors.toList()));
							}

							graphIndex.routesForMtaComplexCache.put(e.getMtaStopId() + "_" + trip.getId(), routes);

						}

						return routes.stream().collect(Collectors.toList());

					// standalone case
					} else {
			    		Set<Route> routes = new HashSet<Route>();
			    		for(Object _stop : stopsForMtaComplex().get(environment)) {
			    			Stop stop = (Stop)_stop;
			    			
			    			routes.addAll(getGraphIndex(environment).routesForStop(stop).stream()
			    				.distinct()
			    				.collect(Collectors.toList()));
			    		}
			    		return routes.stream().collect(Collectors.toList());
					}
		    	} else
		    		return null;
 		 };
	}

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
	    	Stop e = environment.getSource();

			return getRouter(environment).graph.getAlertPatches()
					.filter(s -> s.getStop() != null ? s.getStop().equals(e.getId()) : false)
					.collect(Collectors.toList());
		};
	}
	
	@Override
	public DataFetcher<String> mtaComplexId() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	AgencyAndId gtfsId = e.getParentStation() != null ? new AgencyAndId(e.getId().getAgencyId(), e.getParentStation()) : e.getId();

	    	String candidateComplexID = getGraphIndex(environment)
	    			.mtaSubwayStations
	    			.get("GTFS Stop ID")
	    			.get(gtfsId)
	    			.get(0)
	    			.get("Complex ID");
	    	
	    	boolean complexIdExists = getGraphIndex(environment)
	    			.mtaSubwayComplexes
	    			.get("Complex ID")
	    			.get(new AgencyAndId(e.getId().getAgencyId(), candidateComplexID)) != null;
	    	
	    	return complexIdExists ? candidateComplexID : null;
	    };	
	}

	@Override
	public DataFetcher<String> mtaStationId() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	AgencyAndId gtfsId = e.getParentStation() != null ? new AgencyAndId(e.getId().getAgencyId(), e.getParentStation()) : e.getId();
	    	
	    	return getGraphIndex(environment)
	    			.mtaSubwayStations
	    			.get("GTFS Stop ID")
	    			.get(gtfsId)
	    			.get(0)
	    			.get("Station ID");
	    };	
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DataFetcher<Iterable<Object>> mtaEquipment() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	AgencyAndId gtfsId = e.getParentStation() != null ? new AgencyAndId(e.getId().getAgencyId(), e.getParentStation()) : e.getId();

	    	Set<PathwayEdge> equipmentHere = getGraphIndex(environment).equipmentEdgesForStationId.get(gtfsId);

	    	if (equipmentHere != null) {
	    		Set<EquipmentShort> result = new HashSet<EquipmentShort>();
    		
	    		for(PathwayEdge equipmentEdge : equipmentHere) {
	    			String equipmentId = equipmentEdge.getElevatorId();

	    			EquipmentShort resultItem = new EquipmentShort();
	    			resultItem.isCurrentlyAccessible = true;
	    			resultItem.equipmentId = equipmentId;
    	    	
	    	    	Set<Alert> alerts = new HashSet<Alert>();
	        	   	for (AlertPatch alert : getRouter(environment).graph.getAlertPatches(equipmentEdge)) {
	        	   		if(alert.getStop().equals(gtfsId) 
	        	   				&& alert.getElevatorId().equals(equipmentId)) {
	        	   			alerts.add(alert.getAlert());
	
	        	   			if(alert.isRoutingConsequence())
	        	    	    	resultItem.isCurrentlyAccessible = false;
	        	   		}
	        	   	}
	        	    resultItem.alerts = alerts;    	 
	    	    	result.add(resultItem);
	    		} 
	    		
	    		return result.stream().collect(Collectors.toList());
	    	}
	    	
	    	return null;
		};
	}

	@Override
	public DataFetcher<String> mtaAdaAccessible() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	AgencyAndId gtfsId = e.getParentStation() != null ? new AgencyAndId(e.getId().getAgencyId(), e.getParentStation()) : e.getId();
	    	
	    	return GraphQLNyMtaAdaFlag.values()[Integer.parseInt(getGraphIndex(environment)
	    			.mtaSubwayStations
	    			.get("GTFS Stop ID")
	    			.get(gtfsId)
	    			.get(0)
	    			.get("ADA"))].name();
	    };	
	}

	@Override
	public DataFetcher<String> mtaAdaAccessibleNotes() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	AgencyAndId gtfsId = e.getParentStation() != null ? new AgencyAndId(e.getId().getAgencyId(), e.getParentStation()) : e.getId();

	    	return getGraphIndex(environment)
	    			.mtaSubwayStations
	    			.get("GTFS Stop ID")
	    			.get(gtfsId)
	    			.get(0)
	    			.get("ADA Notes");
		};
	}
	
	
	@Override
	public DataFetcher<Iterable<Object>> preferredTransfers() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	HashMap<String, Object> localContext = environment.getLocalContext();

	    	Trip tripContext = (Trip)localContext.get("trip"); // the trip the user is on to determine transfers
	    	Stop requiredStop = (Stop)localContext.get("stop"); // the stop the user boarded at (LIRR only)
    		
    		TransferTable tt = getRouter(environment).graph.getTransferTable();
    		
    		// this agency hasn't set preferred transfers
	        if(tt.hasFeedTransfers(e.getId().getAgencyId(), e.getId().getAgencyId())) {
	        	if(tripContext != null) {
	        		List<AgencyAndId> tripIds = tt.getPreferredTransfers(e.getId(), e.getId(), tripContext, requiredStop);

	        		if(tripIds.isEmpty())
	        			return List.of();
	        		
	        		return getGraphIndex(environment).tripForId.values()
	    	    			.stream()
	    	    			.filter(it -> tripIds.contains(it.getId()))
	    	    			.distinct()
	    	    			.collect(Collectors.toList());
	        	}
	    	
	    		return List.of();
	        }
	        
	    	return null;
	    };
	}

	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}



}
