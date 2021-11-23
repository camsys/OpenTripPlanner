package org.opentripplanner.ext.flex;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexRouter {

  private static final Logger LOG = LoggerFactory.getLogger(FlexRouter.class);

  private final Graph graph;
  private final FlexIndex flexIndex;

  private final Collection<NearbyStop> streetAccesses;
  private final Collection<NearbyStop> streetEgresses;
  
  private final FlexPathCalculator accessFlexPathCalculator;
  private final FlexPathCalculator egressFlexPathCalculator;

  /* "Start of Time" refers to the service date (midnight) of the date of the request */
  private final ZonedDateTime startOfTime;

  /* this is the number of seconds since startOfTime/the service date */
  private final int departureTime;
  
  private final boolean arriveBy;

  private final FlexServiceDate[] dates;

  /* State */
  private List<FlexAccessTemplate> flexAccessTemplates = null;
  private List<FlexEgressTemplate> flexEgressTemplates = null;

  public FlexRouter(
      Graph graph,
      Instant searchInstant,
      boolean arriveBy,
      int additionalPastSearchDays,
      int additionalFutureSearchDays,
      Collection<NearbyStop> streetAccesses,
      Collection<NearbyStop> egressTransfers
  ) {
    this.graph = graph;
    this.flexIndex = graph.index.getFlexIndex();
    this.accessFlexPathCalculator = new StreetFlexPathCalculator(graph, false);
    this.egressFlexPathCalculator = new StreetFlexPathCalculator(graph, true);
    this.streetAccesses = streetAccesses;
    this.streetEgresses = egressTransfers;
    
    ZoneId tz = graph.getTimeZone().toZoneId();
    LocalDate searchDate = LocalDate.ofInstant(searchInstant, tz);
    this.startOfTime = DateMapper.asStartOfService(searchDate, tz);
    this.departureTime = DateMapper.secondsSinceStartOfTime(startOfTime, searchInstant);
    this.arriveBy = arriveBy;

    int totalDays = additionalPastSearchDays + 1 + additionalFutureSearchDays;
    this.dates = new FlexServiceDate[totalDays];

    for (int d = -additionalPastSearchDays; d <= additionalFutureSearchDays; ++d) {
      LocalDate date = searchDate.plusDays(d);
      int index = d + additionalPastSearchDays;
      
      ServiceDate serviceDate = new ServiceDate(date);
      dates[index] = new FlexServiceDate(
          serviceDate,
          graph.index.getServiceCodesRunningForDate().get(serviceDate)
      );
    }
  }

  public Collection<Itinerary> createFlexOnlyItineraries() {
    calculateFlexAccessTemplates();
    calculateFlexEgressTemplates();

    Set<Itinerary> itineraries = new HashSet<>();

    LOG.info("Direct Routing - Accesses: " + this.flexAccessTemplates.stream()
    	.map(e -> e.getFlexTrip() + " " + e.getAccessEgressStop().getId() + "->" + e.getTransferStop().getId() + "\n")
    	.distinct().collect(Collectors.toList()));
        
    LOG.info("Direct Routing - Egresses: " + this.flexEgressTemplates.stream()
    	.map(e -> e.getFlexTrip() + " " + e.getTransferStop().getId() + " -> " + e.getAccessEgressStop().getId() + "\n")
		.distinct().collect(Collectors.toList()));
    
    for (FlexAccessTemplate template : this.flexAccessTemplates.stream().distinct().collect(Collectors.toList())) {
      StopLocation transferStop = template.getTransferStop();

      List<FlexEgressTemplate> egressTemplates = 
    		  this.flexEgressTemplates.parallelStream().distinct().filter(t -> t.getTransferStop().equals(transferStop)).distinct().collect(Collectors.toList());

      if (!egressTemplates.isEmpty()) {
        for(FlexEgressTemplate egressTemplate : egressTemplates) {
          Itinerary itinerary = template.createDirectItinerary(egressTemplate.getAccessEgress(), arriveBy, departureTime, startOfTime);
  
          if (itinerary != null) {
              LOG.info("Creating itin for trip " + egressTemplate.getFlexTrip()+"/" +template.getFlexTrip() + " from:" + template.getAccessEgressStop() + " to:" + 
                		egressTemplate.getAccessEgressStop() + " xfr=" + template.getTransferStop() + " itin=" + itinerary);
                
            itineraries.add(itinerary);
          }
        }
      }
    }
    
    return itineraries;
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
  
  public Collection<FlexAccessEgress> createFlexAccesses() {
    calculateFlexAccessTemplates();

    return this.flexAccessTemplates
        .parallelStream()
        .distinct()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .filter(e -> e != null)
        .distinct()
        .collect(Collectors.toList());
  }

  public Collection<FlexAccessEgress> createFlexEgresses() {
    calculateFlexEgressTemplates();

    return this.flexEgressTemplates
        .parallelStream()
        .distinct()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .filter(e -> e != null)
        .distinct()
        .collect(Collectors.toList());
  }

  private void calculateFlexAccessTemplates() {
    if (this.flexAccessTemplates != null) { return; }

    // Fetch the closest flexTrips reachable from the access stops
    this.flexAccessTemplates = getClosestFlexTrips(streetAccesses)
        // For each date the router has data for
        .flatMap(t2 -> Arrays.stream(dates)
            // Discard if service is not running on date
            .filter(date -> date.isFlexTripRunning(t2.second, this.graph))
            // Create templates from trip, boarding at the nearbyStop
            .flatMap(date -> t2.second.getFlexAccessTemplates(
                t2.first,
                date,
                accessFlexPathCalculator
            )))
        .collect(Collectors.toList());
  }

  private void calculateFlexEgressTemplates() {
    if (this.flexEgressTemplates != null) { return; }

    // Fetch the closest flexTrips reachable from the egress stops
    this.flexEgressTemplates = getClosestFlexTrips(streetEgresses)
        // For each date the router has data for
        .flatMap(t2 -> Arrays.stream(dates)
            // Discard if service is not running on date
            .filter(date -> date.isFlexTripRunning(t2.second, this.graph))
            // Create templates from trip, alighting at the nearbyStop
            .flatMap(date -> t2.second.getFlexEgressTemplates(
                t2.first, 
                date,
                egressFlexPathCalculator
            )))
        .collect(Collectors.toList());
  }

  private Stream<T2<NearbyStop, FlexTrip>> getClosestFlexTrips(Collection<NearbyStop> nearbyStops) {
	  
	// Find all trips reachable from the nearbyStops
    Collection<T2<NearbyStop, FlexTrip>> flexTripsReachableFromNearbyStops = nearbyStops
        .parallelStream()
        .flatMap(accessEgress -> flexIndex
            .getFlexTripsByStop(accessEgress.stop)
            .map(flexTrip -> new T2<>(accessEgress, flexTrip)))
        .collect(Collectors.toList());

    // Group all (NearbyStop, FlexTrip) tuples by flexTrip
    Collection<List<T2<NearbyStop, FlexTrip>>> groupedReachableFlexTrips = flexTripsReachableFromNearbyStops
    	.parallelStream()
        .distinct()
        .collect(Collectors.groupingBy(t2 -> t2.second.getId()))
        .values();

    // Get the stop with least walking time from each group 
    List<T2<NearbyStop, FlexTrip>> r = groupedReachableFlexTrips
        .parallelStream()
        .map(t2s -> t2s
            .parallelStream()
            .min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
    
    Stream<T2<NearbyStop, FlexTrip>> s = r.stream();    

    LOG.info("getClosestStops() returns " + s.count() + " pairs");    
    
    return s;
  }

}
