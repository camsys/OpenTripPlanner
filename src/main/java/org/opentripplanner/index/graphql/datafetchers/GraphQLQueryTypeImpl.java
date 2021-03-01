package org.opentripplanner.index.graphql.datafetchers;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.model.PairwiseAccessibilityShort;
import org.opentripplanner.api.resource.AccessibilityResource;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.datafetchers.GraphQLQueryTypeInputs.*;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GraphQLQueryTypeImpl implements GraphQLDataFetchers.GraphQLQueryType {

	@Override
	public DataFetcher<Object> node() {
		return environment -> null;
	}

	@Override
	public DataFetcher<Iterable<Object>> alerts() {
		return environment -> {
			GraphQLQueryTypeAlertsArgsInput input = 
				new GraphQLQueryTypeAlertsArgsInput(environment.getArguments());
		
			if(input.getGraphQLFeeds() != null) {
				Stream<String> inputStream = StreamSupport.stream(input.getGraphQLFeeds().spliterator(), false);

				return getRouter(environment).graph.getAlertPatches()
						.filter(c -> inputStream.anyMatch(inputItem -> inputItem.equals(c.getId())))
						.collect(Collectors.toList());			

			} else {			
				return getRouter(environment).graph.getAlertPatches()
					.collect(Collectors.toList());			
			}	
		};
	}

	@Override
	public DataFetcher<Iterable<Object>> feeds() {
	    return environment -> getGraphIndex(environment)
	    		.feedInfoForId.keySet().stream()
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
	public DataFetcher<Iterable<Object>> stop() {
		return environment -> {
			GraphQLQueryTypeStopArgsInput input = 
					new GraphQLQueryTypeStopArgsInput(environment.getArguments());
			
			if(input.getGraphQLMtaStationId() != null) {
				String gtfsId = getGraphIndex(environment).mtaSubwayStationsByStationId.get(input.getGraphQLMtaStationId()).get("GTFS Stop ID");
									
				return getGraphIndex(environment)
						.stopForId.values().stream()
						.filter(c -> c.getId().getId().startsWith(gtfsId))
						.collect(Collectors.toList());				
			
			} else if(input.getGraphQLMtaComplexId() != null) {
				String gtfsId = getGraphIndex(environment).mtaSubwayStationsByComplexId.get(input.getGraphQLMtaComplexId()).get("GTFS Stop ID");
				
				return getGraphIndex(environment)
						.stopForId.values().stream()
						.filter(c -> c.getId().getId().startsWith(gtfsId))
						.collect(Collectors.toList());				
				
			} else {			
				String gtfsId = input.getGraphQLGtfsId();

			    return getGraphIndex(environment).stopForId.values().stream()
						.filter(c -> c.getId().getId().startsWith(gtfsId))
						.collect(Collectors.toList());			
			}	
		};
	}

	@Override
	public DataFetcher<Iterable<Object>> routes() {
		return environment -> {
			GraphQLQueryTypeRoutesArgsInput input = 
					new GraphQLQueryTypeRoutesArgsInput(environment.getArguments());
	    		    	
			if(input.getGraphQLIds() != null) {
				Stream<String> inputStream = StreamSupport.stream(input.getGraphQLIds().spliterator(), false);
				
				return getGraphIndex(environment)
						.routeForId.values().stream()
						.filter(c -> inputStream.anyMatch(inputItem -> inputItem.equals(AgencyAndId.convertToString(c.getId()))))
						.collect(Collectors.toList());				
			
			} else if(input.getGraphQLName() != null) {
				return getGraphIndex(environment)
						.routeForId.values().stream()
						.filter(c -> c.getShortName().equals(input.getGraphQLName()) || c.getLongName().equals(input.getGraphQLName()))
						.collect(Collectors.toList());
				
			} else {			
				return getGraphIndex(environment)
						.routeForId.values().stream()
						.collect(Collectors.toList());
			}
		};
	}

	@Override
	public DataFetcher<Object> route() {
	    return environment -> {
			return getGraphIndex(environment)
					.routeForId.get(AgencyAndId.convertFromString(
							new GraphQLQueryTypeRouteArgsInput(environment.getArguments()).getGraphQLId()));
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> stopAccessibility() {
		return environment -> {
			GraphQLQueryTypeStopAccessibilityArgsInput input = 
					new GraphQLQueryTypeStopAccessibilityArgsInput(environment.getArguments());

			AgencyAndId gtfsId = AgencyAndId.convertFromString(input.getGraphQLGtfsId());
										
			if(input.getGraphQLMtaComplexId() != null) 
				gtfsId = new AgencyAndId("MTASBWY", 
						getGraphIndex(environment).mtaSubwayStationsByComplexId.get(input.getGraphQLMtaComplexId()).get("GTFS Stop ID"));
				
			if(input.getGraphQLMtaStationId() != null) 
				gtfsId = new AgencyAndId("MTASBWY", 
						getGraphIndex(environment).mtaSubwayStationsByStationId.get(input.getGraphQLMtaStationId()).get("GTFS Stop ID"));
			
			AccessibilityResource ar = new AccessibilityResource(getRouter(environment), getGraphIndex(environment));
			ar.date = input.getGraphQLDate();
			ar.ignoreRealtimeUpdates = input.getGraphQLIncludeRealtime() != null ? !input.getGraphQLIncludeRealtime() : false;
			
			ArrayList<PairwiseAccessibilityShort> response = ar.getStopAccessibility(AgencyAndId.convertToString(gtfsId));

			if(response != null)
				return response.stream().collect(Collectors.toList());
			else 
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
