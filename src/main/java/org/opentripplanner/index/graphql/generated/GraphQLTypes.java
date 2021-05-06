// THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.
package org.opentripplanner.index.graphql.generated;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class GraphQLTypes {
  
  
  /** Cause of a alert */
  public enum GraphQLAlertCauseType {
    UNKNOWN_CAUSE,
    OTHER_CAUSE,
    TECHNICAL_PROBLEM,
    STRIKE,
    DEMONSTRATION,
    ACCIDENT,
    HOLIDAY,
    WEATHER,
    MAINTENANCE,
    CONSTRUCTION,
    POLICE_ACTIVITY,
    MEDICAL_EMERGENCY
    
  }
  
  /** Effect of a alert */
  public enum GraphQLAlertEffectType {
    NO_SERVICE,
    REDUCED_SERVICE,
    SIGNIFICANT_DELAYS,
    DETOUR,
    ADDITIONAL_SERVICE,
    MODIFIED_SERVICE,
    OTHER_EFFECT,
    UNKNOWN_EFFECT,
    STOP_MOVED,
    NO_EFFECT
    
  }
  
  /** Severity level of a alert */
  public enum GraphQLAlertSeverityLevelType {
    UNKNOWN_SEVERITY,
    INFO,
    WARNING,
    SEVERE
    
  }
  
  public enum GraphQLBikesAllowed {
    NO_INFORMATION,
    ALLOWED,
    NOT_ALLOWED
    
  }
  
  
  
  
  
  /** Identifies whether this stop represents a stop or station. */
  public enum GraphQLLocationType {
    STOP,
    STATION,
    ENTRANCE_EXIT,
    GENERIC_NODE,
    BOARDING_AREA
    
  }
  
  /** Enhanced accessibility information set by NY MTA's Accessibility Group. */
  public enum GraphQLNyMtaAdaFlag {
    UNKNOWN,
    NOT_ACCESSIBLE,
    ACCESSIBLE,
    PARTLY_ACCESSIBLE
    
  }
  
  
  
  public static class GraphQLQueryTypeAgencyArgs {
    private String id;
  
    public GraphQLQueryTypeAgencyArgs() {}
  
    public String getGraphQLId() { return this.id; }
    public void setGraphQLId(String id) { this.id = id; }
  }
  public static class GraphQLQueryTypeStopsArgs {
    private String gtfsId;
    private String mtaComplexId;
    private String mtaStationId;
  
    public GraphQLQueryTypeStopsArgs() {}
  
    public String getGraphQLGtfsId() { return this.gtfsId; }
    public String getGraphQLMtaComplexId() { return this.mtaComplexId; }
    public String getGraphQLMtaStationId() { return this.mtaStationId; }
    public void setGraphQLGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
    public void setGraphQLMtaComplexId(String mtaComplexId) { this.mtaComplexId = mtaComplexId; }
    public void setGraphQLMtaStationId(String mtaStationId) { this.mtaStationId = mtaStationId; }
  }
  public static class GraphQLQueryTypeAlertsArgs {
    private Iterable<String> feeds;
  
    public GraphQLQueryTypeAlertsArgs() {}
  
    public Iterable<String> getGraphQLFeeds() { return this.feeds; }
    public void setGraphQLFeeds(Iterable<String> feeds) { this.feeds = feeds; }
  }
  public static class GraphQLQueryTypeRoutesArgs {
    private Iterable<String> gtfsIds;
    private String name;
  
    public GraphQLQueryTypeRoutesArgs() {}
  
    public Iterable<String> getGraphQLGtfsIds() { return this.gtfsIds; }
    public String getGraphQLName() { return this.name; }
    public void setGraphQLGtfsIds(Iterable<String> gtfsIds) { this.gtfsIds = gtfsIds; }
    public void setGraphQLName(String name) { this.name = name; }
  }
  public static class GraphQLQueryTypeRouteArgs {
    private String gtfsId;
  
    public GraphQLQueryTypeRouteArgs() {}
  
    public String getGraphQLGtfsId() { return this.gtfsId; }
    public void setGraphQLGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
  }
  public static class GraphQLQueryTypeTripsArgs {
    private Iterable<String> feeds;
  
    public GraphQLQueryTypeTripsArgs() {}
  
    public Iterable<String> getGraphQLFeeds() { return this.feeds; }
    public void setGraphQLFeeds(Iterable<String> feeds) { this.feeds = feeds; }
  }
  public static class GraphQLQueryTypeTripArgs {
    private String gtfsId;
  
    public GraphQLQueryTypeTripArgs() {}
  
    public String getGraphQLGtfsId() { return this.gtfsId; }
    public void setGraphQLGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
  }
  public static class GraphQLQueryTypeStopAccessibilityArgs {
    private String date;
    private Boolean includeRealtime;
    private String gtfsId;
    private String mtaComplexId;
    private String mtaStationId;
  
    public GraphQLQueryTypeStopAccessibilityArgs() {}
  
    public String getGraphQLDate() { return this.date; }
    public Boolean getGraphQLIncludeRealtime() { return this.includeRealtime; }
    public String getGraphQLGtfsId() { return this.gtfsId; }
    public String getGraphQLMtaComplexId() { return this.mtaComplexId; }
    public String getGraphQLMtaStationId() { return this.mtaStationId; }
    public void setGraphQLDate(String date) { this.date = date; }
    public void setGraphQLIncludeRealtime(Boolean includeRealtime) { this.includeRealtime = includeRealtime; }
    public void setGraphQLGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
    public void setGraphQLMtaComplexId(String mtaComplexId) { this.mtaComplexId = mtaComplexId; }
    public void setGraphQLMtaStationId(String mtaStationId) { this.mtaStationId = mtaStationId; }
  }
  public static class GraphQLQueryTypeNearbyArgs {
    private Double latitude;
    private Double longitude;
    private Double radius;
    private String gtfsStopIdList;
    private Integer maxStops;
    private Integer minStops;
    private String routesList;
    private Integer direction;
    private String date;
    private String time;
    private Integer timeRange;
    private Integer numberOfDepartures;
    private Boolean omitNonPickups;
    private String tripHeadsign;
    private String stoppingAtGtfsStopId;
    private Boolean groupByParent;
    private Boolean showCancelledTrips;
    private Boolean includeStopsForTrip;
    private String tracksList;
    private Boolean signMode;
  
    public GraphQLQueryTypeNearbyArgs() {}
  
    public Double getGraphQLLatitude() { return this.latitude; }
    public Double getGraphQLLongitude() { return this.longitude; }
    public Double getGraphQLRadius() { return this.radius; }
    public String getGraphQLGtfsStopIdList() { return this.gtfsStopIdList; }
    public Integer getGraphQLMaxStops() { return this.maxStops; }
    public Integer getGraphQLMinStops() { return this.minStops; }
    public String getGraphQLRoutesList() { return this.routesList; }
    public Integer getGraphQLDirection() { return this.direction; }
    public String getGraphQLDate() { return this.date; }
    public String getGraphQLTime() { return this.time; }
    public Integer getGraphQLTimeRange() { return this.timeRange; }
    public Integer getGraphQLNumberOfDepartures() { return this.numberOfDepartures; }
    public Boolean getGraphQLOmitNonPickups() { return this.omitNonPickups; }
    public String getGraphQLTripHeadsign() { return this.tripHeadsign; }
    public String getGraphQLStoppingAtGtfsStopId() { return this.stoppingAtGtfsStopId; }
    public Boolean getGraphQLGroupByParent() { return this.groupByParent; }
    public Boolean getGraphQLShowCancelledTrips() { return this.showCancelledTrips; }
    public Boolean getGraphQLIncludeStopsForTrip() { return this.includeStopsForTrip; }
    public String getGraphQLTracksList() { return this.tracksList; }
    public Boolean getGraphQLSignMode() { return this.signMode; }
    public void setGraphQLLatitude(Double latitude) { this.latitude = latitude; }
    public void setGraphQLLongitude(Double longitude) { this.longitude = longitude; }
    public void setGraphQLRadius(Double radius) { this.radius = radius; }
    public void setGraphQLGtfsStopIdList(String gtfsStopIdList) { this.gtfsStopIdList = gtfsStopIdList; }
    public void setGraphQLMaxStops(Integer maxStops) { this.maxStops = maxStops; }
    public void setGraphQLMinStops(Integer minStops) { this.minStops = minStops; }
    public void setGraphQLRoutesList(String routesList) { this.routesList = routesList; }
    public void setGraphQLDirection(Integer direction) { this.direction = direction; }
    public void setGraphQLDate(String date) { this.date = date; }
    public void setGraphQLTime(String time) { this.time = time; }
    public void setGraphQLTimeRange(Integer timeRange) { this.timeRange = timeRange; }
    public void setGraphQLNumberOfDepartures(Integer numberOfDepartures) { this.numberOfDepartures = numberOfDepartures; }
    public void setGraphQLOmitNonPickups(Boolean omitNonPickups) { this.omitNonPickups = omitNonPickups; }
    public void setGraphQLTripHeadsign(String tripHeadsign) { this.tripHeadsign = tripHeadsign; }
    public void setGraphQLStoppingAtGtfsStopId(String stoppingAtGtfsStopId) { this.stoppingAtGtfsStopId = stoppingAtGtfsStopId; }
    public void setGraphQLGroupByParent(Boolean groupByParent) { this.groupByParent = groupByParent; }
    public void setGraphQLShowCancelledTrips(Boolean showCancelledTrips) { this.showCancelledTrips = showCancelledTrips; }
    public void setGraphQLIncludeStopsForTrip(Boolean includeStopsForTrip) { this.includeStopsForTrip = includeStopsForTrip; }
    public void setGraphQLTracksList(String tracksList) { this.tracksList = tracksList; }
    public void setGraphQLSignMode(Boolean signMode) { this.signMode = signMode; }
  }
  
  
  
  
  
  
  /** Accessibility information from GTFS. */
  public enum GraphQLWheelchairBoarding {
    NO_INFORMATION,
    POSSIBLE,
    NOT_POSSIBLE
    
  }
  
}
