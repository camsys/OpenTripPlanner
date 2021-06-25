package org.opentripplanner.index.graphql.datafetchers;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLLocationType;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLNyMtaAdaFlag;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLWheelchairBoarding;
import org.opentripplanner.index.model.EquipmentShort;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

public class GraphQLStopImpl implements GraphQLDataFetchers.GraphQLStop {

	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return AgencyAndId.convertToString(e.getId());
	    };
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
	public DataFetcher<Iterable<Object>> routes() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	
	    	if(e.getLocationType() == Stop.LOCATION_TYPE_STATION || e.getLocationType() == Stop.LOCATION_TYPE_STOP)
	    		return getGraphIndex(environment).routesForStop(e).stream()
	    			.distinct()
	    			.collect(Collectors.toList());
	    	else
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

	    	return getGraphIndex(environment)
	    			.mtaSubwayStations
	    			.get("GTFS Stop ID")
	    			.get(gtfsId)
	    			.get(0)
	    			.get("Complex ID");
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

	        		return getGraphIndex(environment).tripForId.values()
	    	    			.stream()
	    	    			.filter(it -> tripIds.contains(it.getId()))
	    	    			.map(it -> it.getRoute())
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
