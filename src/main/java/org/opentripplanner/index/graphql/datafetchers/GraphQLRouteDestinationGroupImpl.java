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
import org.opentripplanner.index.model.StopTimesByRouteAndHeadsign;
import org.opentripplanner.index.model.StopTimesByStop;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.standalone.Router;

public class GraphQLRouteDestinationGroupImpl implements GraphQLDataFetchers.GraphQLRouteDestinationGroup {

	@Override
	public DataFetcher<Object> route() {
	    return environment -> {
	    	StopTimesByRouteAndHeadsign e = environment.getSource();
	    	return getGraphIndex(environment).routeForId.get(e.getRoute().id);
	    };
	}

	@Override
	public DataFetcher<String> headsign() {
	    return environment -> {
	    	StopTimesByRouteAndHeadsign e = environment.getSource();
	    	return e.getHeadsign();
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> stopTimes() {
	    return environment -> {
	    	StopTimesByRouteAndHeadsign e = environment.getSource();
	    	return e.getTimes().stream().collect(Collectors.toList());
	    };	
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
