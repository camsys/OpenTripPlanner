package org.opentripplanner.index.graphql.datafetchers;

import java.util.Map;

import org.opentripplanner.index.graphql.generated.GraphQLTypes.*;

@SuppressWarnings("unchecked")
public class GraphQLQueryTypeInputs {

	public static class GraphQLQueryTypeAgencyArgsInput extends GraphQLQueryTypeAgencyArgs {
	    public GraphQLQueryTypeAgencyArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLId((String)args.get("id"));
	        }
	    }
	}

	public static class GraphQLQueryTypeStopsArgsInput extends GraphQLQueryTypeStopsArgs {
	    public GraphQLQueryTypeStopsArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLGtfsId((String)args.get("gtfsId"));
	        	this.setGraphQLMtaComplexId((String)args.get("mtaComplexId"));
	        	this.setGraphQLMtaStationId((String)args.get("mtaStationId"));
	        }
	    }
	}
	
	public static class GraphQLQueryTypeAlertsArgsInput extends GraphQLQueryTypeAlertsArgs {
	    public GraphQLQueryTypeAlertsArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLFeeds( (Iterable<String>)args.get("feeds"));
	        }
	    }
	}	
	
	public static class GraphQLQueryTypeRoutesArgsInput extends GraphQLQueryTypeRoutesArgs {
		public GraphQLQueryTypeRoutesArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLGtfsIds((Iterable<String>)args.get("gtfsIds"));
	        	this.setGraphQLName((String)args.get("name"));
	        }
	    }
	}	
	
	public static class GraphQLQueryTypeRouteArgsInput extends GraphQLQueryTypeRouteArgs {
	    public GraphQLQueryTypeRouteArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLGtfsId((String)args.get("gtfsId"));
	        }
	    }
	}	
	
	public static class GraphQLQueryTypeStopAccessibilityArgsInput extends GraphQLQueryTypeStopAccessibilityArgs {
	    public GraphQLQueryTypeStopAccessibilityArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLDate((String)args.get("date"));
	        	this.setGraphQLIncludeRealtime((Boolean)args.get("includeRealtime"));
	        	this.setGraphQLGtfsId((String)args.get("gtfsId"));
	        	this.setGraphQLMtaComplexId((String)args.get("mtaComplexId"));
	        	this.setGraphQLMtaStationId((String)args.get("mtaStationId"));
	        }
	    }
	}	
		
	
}
