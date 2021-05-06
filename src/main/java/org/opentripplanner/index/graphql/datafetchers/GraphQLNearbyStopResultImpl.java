package org.opentripplanner.index.graphql.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.model.EquipmentShort;
import org.opentripplanner.index.model.StopTimesByStop;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.standalone.Router;

public class GraphQLNearbyStopResultImpl implements GraphQLDataFetchers.GraphQLNearbyStopResult {

	@Override
	public DataFetcher<Iterable<Object>> stop() {
	    return environment -> {
	    	StopTimesByStop e = environment.getSource();

			List<AgencyAndId> ids = new ArrayList<AgencyAndId>();
			ids.addAll(getGraphIndex(environment).stopsForParentStation
					.get(e.getStop().id)
					.stream()
					.map(it -> { return it.getId(); })
					.collect(Collectors.toList()));
			
		    return getGraphIndex(environment).stopForId.values().stream()
					.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().equals(inputItem)))
					.distinct()
					.collect(Collectors.toList());		    
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> routeDestinations() {
	    return environment -> {
	    	StopTimesByStop e = environment.getSource();
	    	return e.getGroups().stream().collect(Collectors.toList());
	    };
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
