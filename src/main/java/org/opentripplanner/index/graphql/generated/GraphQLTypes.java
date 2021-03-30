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
    ENTRANCE
    
  }
  
  /** Enhanced accessibility information set by NY MTA's Accessibility Group. */
  public enum GraphQLNyMtaAdaFlag {
    UNKNOWN,
    NOT_ACCESSIBLE,
    ACCESSIBLE,
    PARTLY_ACCESSIBLE
    
  }
  
  
  public static class GraphQLQueryTypeFeedByFeedIdArgs {
    private String feedId;
  
    public GraphQLQueryTypeFeedByFeedIdArgs() {}
  
    public String getGraphQLFeedId() { return this.feedId; }
    public void setGraphQLFeedId(String feedId) { this.feedId = feedId; }
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
  
  
  
  
  /** Accessibility information from GTFS. */
  public enum GraphQLWheelchairBoarding {
    NO_INFORMATION,
    POSSIBLE,
    NOT_POSSIBLE
    
  }
  
}
