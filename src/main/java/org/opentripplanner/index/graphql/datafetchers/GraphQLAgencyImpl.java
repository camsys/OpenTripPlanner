package org.opentripplanner.index.graphql.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.model.EquipmentShort;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.standalone.Router;

public class GraphQLAgencyImpl implements GraphQLDataFetchers.GraphQLAgency {

	@Override
	public DataFetcher<String> gtfsId() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getId();
	    };
	}

	@Override
	public DataFetcher<String> name() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getName();
	    };
	}

	@Override
	public DataFetcher<String> url() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getUrl();
	    };
	}

	@Override
	public DataFetcher<String> timezone() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getTimezone();
	    };
	}

	@Override
	public DataFetcher<String> lang() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getLang();
	    };
	}

	@Override
	public DataFetcher<String> phone() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getPhone();
	    };
	}

	@Override
	public DataFetcher<String> fareUrl() {
	    return environment -> {
	    	Agency e = environment.getSource();
	    	return e.getFareUrl();
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> stops() {
	    return environment -> {
	    	Agency e = environment.getSource();

	    	return getGraphIndex(environment).stopForId.entrySet().stream()
	    		.filter(o -> o.getKey().getAgencyId().equals(e.getId()))
	    		.map(s -> s.getValue())
	    		.collect(Collectors.toList());
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> routes() {
	    return environment -> {
	    	Agency e = environment.getSource();

	    	return getGraphIndex(environment).routeForId.values().stream()
	    		.filter(o -> o.getId().getAgencyId().equals(e.getId()))
	    		.distinct()
	    		.collect(Collectors.toList());
	    };
	}

	@Override
	public DataFetcher<Iterable<Object>> mtaEquipment() {
		return environment -> {
	    	Agency e = environment.getSource();
	    	
	    	Set<EquipmentShort> result = new HashSet<EquipmentShort>();

	    	for(Vertex v : getRouter(environment).graph.getVertices()) {
	    		if(!(v instanceof TransitStationStop))
	    			continue;
	    		
	    		TransitStationStop tss = (TransitStationStop)v;
	    		
	    		if(!tss.getStopId().getAgencyId().equals(e.getId()))
	    			continue;

	    		AgencyAndId parentStationId = tss.getStopId();	    		
	    		if(tss.getStop().getParentStation() != null)
	    			parentStationId = new AgencyAndId(tss.getStopId().getAgencyId(), tss.getStop().getParentStation());
	    		
	        	Set<PathwayEdge> equipmentHere = 
	        			getGraphIndex(environment).equipmentEdgesForStationId.get(parentStationId);

	        	if(equipmentHere == null || equipmentHere.isEmpty())
	        		continue;
	        	
	    	    for(PathwayEdge equipmentEdge : equipmentHere) {
	    	    	String equipmentId = equipmentEdge.getElevatorId();

	    	    	EquipmentShort resultItem = new EquipmentShort();
	    	    	resultItem.isCurrentlyAccessible = true;
	    	    	resultItem.equipmentId = equipmentId;
	    	    	
	    	    	Set<Alert> alerts = new HashSet<Alert>();
	        	   	for (AlertPatch alert : getRouter(environment).graph.getAlertPatches(equipmentEdge)) {
	        	   		if(alert.getStop().equals(tss.getStopId())
	        	   				&& alert.getElevatorId().equals(equipmentId)) {
	        	   			alerts.add(alert.getAlert());

	        	   			if(alert.isRoutingConsequence())
	        	    	    	resultItem.isCurrentlyAccessible = false;
	        	   		}
	        	   	}
	        	   	
	        	    resultItem.alerts = alerts;    	 
	    	    	
	    	    	result.add(resultItem);
	    	    } 
	    	}        	
	    

	    	return result.stream().collect(Collectors.toList());
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
	    	Agency e = environment.getSource();

			return getRouter(environment).graph.getAlertPatches()
					.filter(s -> s.getAgency() != null ? 
							s.getAgency().equals(e.getId()) : 
								s.getFeedId() != null ? s.getFeedId().equals(e.getId()) : false)
					.collect(Collectors.toList());
		};
	}

	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}
}
