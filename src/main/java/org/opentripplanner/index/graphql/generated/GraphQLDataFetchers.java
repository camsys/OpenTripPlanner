//THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.
package org.opentripplanner.index.graphql.generated;

import graphql.schema.TypeResolver;
import graphql.schema.DataFetcher;

public class GraphQLDataFetchers {
  /** A feed provides routing data (stops, routes, timetables, etc.) from one or more public transport agencies. */
  public interface GraphQLFeed {
    public DataFetcher<String> feedId();
    public DataFetcher<Iterable<Object>> agencies();
  }
  
  /** Alert of a current or upcoming disruption in public transportation */
  public interface GraphQLAlert {
    public DataFetcher<graphql.relay.Relay.ResolvedGlobalId> id();
    public DataFetcher<Integer> alertHash();
    public DataFetcher<String> feed();
    public DataFetcher<Object> agency();
    public DataFetcher<Object> route();
    public DataFetcher<Object> stop();
    public DataFetcher<String> alertHeaderText();
    public DataFetcher<String> alertHeaderTextTranslations();
    public DataFetcher<String> alertDescriptionText();
    public DataFetcher<String> alertDescriptionTextTranslations();
    public DataFetcher<String> alertUrl();
    public DataFetcher<String> alertUrlTranslations();
    public DataFetcher<Object> alertEffect();
    public DataFetcher<Object> alertCause();
    public DataFetcher<Object> alertSeverityLevel();
    public DataFetcher<Object> effectiveStartDate();
    public DataFetcher<Object> effectiveEndDate();
  }
  
  /**
   * Route represents a public transportation service, usually from point A to point
   * B and *back*, shown to customers under a single name, e.g. bus M60. Routes
   * contain patterns (see field `patterns`), which describe different variants of
   * the route, e.g. outbound pattern from point A to point B and inbound pattern
   * from point B to point A.
   */
  public interface GraphQLRoute {
    public DataFetcher<graphql.relay.Relay.ResolvedGlobalId> id();
    public DataFetcher<String> gtfsId();
    public DataFetcher<Object> agency();
    public DataFetcher<String> shortName();
    public DataFetcher<String> longName();
    public DataFetcher<Integer> type();
    public DataFetcher<String> desc();
    public DataFetcher<String> url();
    public DataFetcher<String> color();
    public DataFetcher<String> textColor();
    public DataFetcher<Iterable<Object>> stops();
    public DataFetcher<Iterable<Object>> alerts();
  }
  
  /**
   * Stop can represent either a single public transport stop, where passengers can
   * board and/or disembark vehicles, or a station, which contains multiple stops.
   * See field `locationType`.
   */
  public interface GraphQLStop {
    public DataFetcher<graphql.relay.Relay.ResolvedGlobalId> id();
    public DataFetcher<String> gtfsId();
    public DataFetcher<String> mtaComplexId();
    public DataFetcher<String> mtaStationId();
    public DataFetcher<Iterable<Object>> mtaEquipment();
    public DataFetcher<Object> mtaAdaAccessible();
    public DataFetcher<String> mtaAdaAccessibleNotes();
    public DataFetcher<String> name();
    public DataFetcher<Double> lat();
    public DataFetcher<Double> lon();
    public DataFetcher<String> code();
    public DataFetcher<String> desc();
    public DataFetcher<String> zoneId();
    public DataFetcher<String> url();
    public DataFetcher<Object> locationType();
    public DataFetcher<Object> parentStation();
    public DataFetcher<Object> wheelchairBoarding();
    public DataFetcher<String> direction();
    public DataFetcher<String> timezone();
    public DataFetcher<Integer> vehicleType();
    public DataFetcher<String> platformCode();
    public DataFetcher<Iterable<Object>> stops();
    public DataFetcher<Iterable<Object>> routes();
    public DataFetcher<Iterable<Object>> alerts();
  }
  
  /** Interface for places, e.g. stops, stations, parking areas.. */
  public interface GraphQLPlaceInterface extends TypeResolver {
    default public DataFetcher<graphql.relay.Relay.ResolvedGlobalId> id() { return null; }
    default public DataFetcher<Double> lat() { return null; }
    default public DataFetcher<Double> lon() { return null; }
  }
  
  /** Station equipment such as an escalator or elevator. */
  public interface GraphQLEquipment {
    public DataFetcher<graphql.relay.Relay.ResolvedGlobalId> id();
    public DataFetcher<String> mtaEquipmentId();
    public DataFetcher<Boolean> isCurrentlyAccessible();
    public DataFetcher<Iterable<Object>> alerts();
  }
  
  /** A public transport agency */
  public interface GraphQLAgency {
    public DataFetcher<graphql.relay.Relay.ResolvedGlobalId> id();
    public DataFetcher<String> gtfsId();
    public DataFetcher<String> name();
    public DataFetcher<String> url();
    public DataFetcher<String> timezone();
    public DataFetcher<String> lang();
    public DataFetcher<String> phone();
    public DataFetcher<String> fareUrl();
    public DataFetcher<Iterable<Object>> routes();
    public DataFetcher<Iterable<Object>> alerts();
    public DataFetcher<Iterable<Object>> mtaEquipment();
  }
  
  /** An object with an ID */
  public interface GraphQLNode extends TypeResolver {
    default public DataFetcher<graphql.relay.Relay.ResolvedGlobalId> id() { return null; }
  }
  
  public interface GraphQLPairwiseAccessibleResult {
    public DataFetcher<Object> from();
    public DataFetcher<Object> to();
    public DataFetcher<Iterable<String>> dependsOnEquipment();
    public DataFetcher<Boolean> isCurrentlyAccessible();
    public DataFetcher<Iterable<Object>> alerts();
  }
  
  public interface GraphQLQueryType {
    public DataFetcher<Iterable<Object>> feeds();
    public DataFetcher<Iterable<Object>> agencies();
    public DataFetcher<Object> agency();
    public DataFetcher<Iterable<Object>> stop();
    public DataFetcher<Iterable<Object>> alerts();
    public DataFetcher<Iterable<Object>> routes();
    public DataFetcher<Object> route();
    public DataFetcher<Iterable<Object>> stopAccessibility();
  }
  
}
