package org.opentripplanner.index.graphql.datafetchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayStationsByStationId.get(input.getGraphQLMtaStationId());						
				if(records == null)
					throw new Exception("Station ID was not found.");
				
				List<String> ids = new ArrayList<String>();
				for(HashMap<String, String> record : records) {
					ids.add(record.get("GTFS Stop ID"));
				}
				
				return getGraphIndex(environment)
						.stopForId.values().stream()
						.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().getId().startsWith(inputItem)))
						.collect(Collectors.toList());				
			
			} else if(input.getGraphQLMtaComplexId() != null) {
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayStationsByComplexId.get(input.getGraphQLMtaComplexId());
				if(records == null)
					throw new Exception("Complex ID was not found.");

				List<String> ids = new ArrayList<String>();
				for(HashMap<String, String> record : records) {
					ids.add(record.get("GTFS Stop ID"));
				}

				return getGraphIndex(environment)
						.stopForId.values().stream()
						.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().getId().startsWith(inputItem)))
						.collect(Collectors.toList());				
				
			} else {			
				AgencyAndId gtfsAgencyAndId = AgencyAndId.convertFromString(input.getGraphQLGtfsId());

			    return getGraphIndex(environment).stopForId.values().stream()
						.filter(c -> c.getId().getId().startsWith(gtfsAgencyAndId.getId()))
						.collect(Collectors.toList());			
			}	
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
							new GraphQLQueryTypeRouteArgsInput(environment.getArguments()).getGraphQLGtfsId()));
	    };
	}
	
	@Override
	public DataFetcher<Iterable<Object>> stopAccessibility() {
		return environment -> {
			GraphQLQueryTypeStopAccessibilityArgsInput input = 
					new GraphQLQueryTypeStopAccessibilityArgsInput(environment.getArguments());

			List<AgencyAndId> queries = null;			
			if(input.getGraphQLMtaComplexId() != null) {
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayStationsByComplexId.get(input.getGraphQLMtaComplexId());
				if(records == null)
					throw new Exception("Complex ID was not found.");

				List<String> ids = new ArrayList<String>();
				for(HashMap<String, String> record : records) {
					ids.add(record.get("GTFS Stop ID"));
				}

				queries = getGraphIndex(environment)
						.stopForId.keySet().stream()
						.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().startsWith(inputItem)))
						.collect(Collectors.toList());				
				
			} else	if(input.getGraphQLMtaStationId() != null) {
				ArrayList<HashMap<String, String>> records = getGraphIndex(environment).mtaSubwayStationsByStationId.get(input.getGraphQLMtaStationId());
				if(records == null)
					throw new Exception("Station ID was not found.");
				
				List<String> ids = new ArrayList<String>();
				for(HashMap<String, String> record : records) {
					ids.add(record.get("GTFS Stop ID"));
				}
				
				queries = getGraphIndex(environment)
						.stopForId.keySet().stream()
						.filter(c -> ids.stream().anyMatch(inputItem -> c.getId().startsWith(inputItem)))
						.collect(Collectors.toList());						
			} else {
				queries = getGraphIndex(environment).stopForId.keySet().stream()
						.filter(c -> c.getId().startsWith(AgencyAndId.convertFromString(input.getGraphQLGtfsId()).getId()))
						.collect(Collectors.toList());			
			}
		
			AccessibilityResource ar = new AccessibilityResource(getRouter(environment), getGraphIndex(environment));
			ar.date = input.getGraphQLDate();
			ar.ignoreRealtimeUpdates = input.getGraphQLIncludeRealtime() != null ? !input.getGraphQLIncludeRealtime() : false;
			
			ArrayList<PairwiseAccessibilityShort> response = ar.getPairwiseAccessibility(queries);					
			return response.stream().collect(Collectors.toList());
		};
	}

	private Router getRouter(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getRouter();
	}

	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}




}
