package org.opentripplanner.index.graphql.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.Stop;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLLocationType;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLNyMtaAdaFlag;
import org.opentripplanner.index.graphql.generated.GraphQLTypes.GraphQLWheelchairBoarding;
import org.opentripplanner.index.model.EquipmentShort;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
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
	public DataFetcher<Object> locationType() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return GraphQLLocationType.values()[e.getLocationType()];
	    };
	}

	@Override
	public DataFetcher<Object> parentStation() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return e.getParentStation();
	    };
	}

	@Override
	public DataFetcher<Object> wheelchairBoarding() {
	    return environment -> {
	    	Stop e = environment.getSource();
	    	return GraphQLWheelchairBoarding.values()[e.getWheelchairBoarding()];
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
					.filter(s -> s.getStop().equals(e.getId()))
					.collect(Collectors.toList());
		};
	}
	
	@Override
	public DataFetcher<String> mtaComplexId() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	String gtfsId = e.getParentStation() != null ? e.getParentStation() : e.getId().getId();
	    	
	    	return getGraphIndex(environment).mtaSubwayStationsByGtfsId
	    			.get(gtfsId).get(0).get("Complex ID");
	    };	
	}

	@Override
	public DataFetcher<String> mtaStationId() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	String gtfsId = e.getParentStation() != null ? e.getParentStation() : e.getId().getId();
	    	
	    	return getGraphIndex(environment).mtaSubwayStationsByGtfsId
	    			.get(gtfsId).get(0).get("Station ID");
	    };	
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataFetcher<Iterable<Object>> mtaEquipment() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	String gtfsId = e.getParentStation() != null ? e.getParentStation() : e.getId().getId();

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
	        	   		if(alert.getStop().equals(new AgencyAndId("MTASBWY", gtfsId)) 
	        	   				&& alert.getElevatorId().equals(equipmentId)) {
	        	   			alerts.add(alert.getAlert());
	
	        	   			if(alert.isRoutingConsequence())
	        	    	    	resultItem.isCurrentlyAccessible = false;
	        	   		}
	        	   	}
	        	    resultItem.alerts = alerts;    	 
	    	    	result.add(resultItem);
	    		} 
	    		
	    		return (Iterable<Object>)result.iterator();
	    	}
	    	
	    	return null;
		};
	}

	@Override
	public DataFetcher<Object> mtaAdaAccessible() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	String gtfsId = e.getParentStation() != null ? e.getParentStation() : e.getId().getId();
	    	
	    	return GraphQLNyMtaAdaFlag.values()[Integer.parseInt(getGraphIndex(environment).mtaSubwayStationsByGtfsId
	    			.get(gtfsId).get(0).get("ADA"))];
	    };	
	}

	@Override
	public DataFetcher<String> mtaAdaAccessibleNotes() {
		return environment -> {
	    	Stop e = environment.getSource();
	    	String gtfsId = e.getParentStation() != null ? e.getParentStation() : e.getId().getId();
	    	
	    	return getGraphIndex(environment).mtaSubwayStationsByGtfsId
	    			.get(gtfsId).get(0).get("ADA Notes");
	    };	
	}
	
	
	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}


}
