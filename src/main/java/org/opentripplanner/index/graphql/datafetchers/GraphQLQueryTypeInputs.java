package org.opentripplanner.index.graphql.datafetchers;

import java.util.Map;

import org.opentripplanner.index.graphql.generated.GraphQLTypes.*;

@SuppressWarnings("unchecked")
public class GraphQLQueryTypeInputs {

	public static class GraphQLQueryTypeFeedArgsInput extends GraphQLQueryTypeFeedArgs {
	    public GraphQLQueryTypeFeedArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLId((String)args.get("id"));
	        }
	    }
	}

	public static class GraphQLQueryTypeScheduleArgsInput extends GraphQLQueryTypeScheduleArgs {
		public GraphQLQueryTypeScheduleArgsInput(Map<String, Object> args) {
			if (args != null) {
				this.setGraphQLFromGtfsId((String)args.get("fromGtfsId"));
				this.setGraphQLToGtfsId((String)args.get("toGtfsId"));
				this.setGraphQLTime((String)args.get("time"));
				this.setGraphQLMaxResults((Integer)args.get("maxResults"));
				this.setGraphQLMaxTime((Integer)args.get("maxTime"));
			}
		}
	}

	public static class GraphQLQueryTypeRecentTripsArgsInput extends GraphQLQueryTypeRecentTripsArgs {
		public GraphQLQueryTypeRecentTripsArgsInput(Map<String, Object> args) {
			if (args != null) {
				this.setGraphQLFeedId((String)args.get("feedId"));
			}
		}
	}
	
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
	        	this.setGraphQLGtfsIds((Iterable<String>)args.get("gtfsIds"));
	        	this.setGraphQLMtaComplexId((String)args.get("mtaComplexId"));
	        	this.setGraphQLMtaStationId((String)args.get("mtaStationId"));
	        }
	    }
	}

	public static class GraphQLQueryTypeStopArgsInput extends GraphQLQueryTypeStopArgs {
	    public GraphQLQueryTypeStopArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLGtfsId((String)args.get("gtfsId"));
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

	public static class GraphQLQueryTypeTripsArgsInput extends GraphQLQueryTypeTripsArgs {
		public GraphQLQueryTypeTripsArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLFeeds( (Iterable<String>)args.get("feeds"));
	        }
	    }
	}	
	
	public static class GraphQLQueryTypeTripArgsInput extends GraphQLQueryTypeTripArgs {
	    public GraphQLQueryTypeTripArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLGtfsId((String)args.get("gtfsId"));
	        }
	    }
	}	

	public static class GraphQLQueryTypeStopAccessibilityArgsInput extends GraphQLQueryTypeAccessibilityArgs {
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

	public static class GraphQLQueryTypeNearbyArgsInput extends GraphQLQueryTypeNearbyArgs {
	    public GraphQLQueryTypeNearbyArgsInput(Map<String, Object> args) {
	        if (args != null) {
	        	this.setGraphQLLatitude((Double)args.get("latitude"));
	        	this.setGraphQLLongitude((Double)args.get("longitude"));
	        	this.setGraphQLRadius((Double)args.get("radius"));

	        	this.setGraphQLGtfsStopIdList((String)args.get("gtfsStopIdList"));

	        	this.setGraphQLMaxStops((Integer)args.get("maxStops"));
	        	this.setGraphQLMinStops((Integer)args.get("minStops"));

	        	this.setGraphQLRoutesList((String)args.get("routesList"));

	        	this.setGraphQLDirection((Integer)args.get("direction"));

	        	this.setGraphQLDate((String)args.get("date"));
	        	
	        	this.setGraphQLTime((String)args.get("time"));

	        	this.setGraphQLTimeRange((Integer)args.get("timeRange"));

	        	this.setGraphQLNumberOfDepartures((Integer)args.get("numberOfDepartures"));

	        	this.setGraphQLOmitNonPickups((Boolean)args.get("omitNonPickups"));

	        	this.setGraphQLTripHeadsign((String)args.get("tripHeadsign"));

	        	this.setGraphQLStoppingAtGtfsStopId((String)args.get("stoppingAtGtfsStopId"));

	        	this.setGraphQLGroupByParent((Boolean)args.get("groupByParent"));

	        	this.setGraphQLShowCancelledTrips((Boolean)args.get("showCancelledTrips"));

	        	this.setGraphQLIncludeStopsForTrip((Boolean)args.get("includeStopsForTrip"));

	        	this.setGraphQLTracksList((String)args.get("tracksList"));

	        	this.setGraphQLSignMode((Boolean)args.get("signMode"));
	        }
	    }
	}	

}
