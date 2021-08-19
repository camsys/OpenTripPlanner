package org.opentripplanner.ext.flex;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlexRouter {

  private static final Logger LOG = LoggerFactory.getLogger(FlexRouter.class);

  /* Transit data */
  private final Graph graph;
  private final Collection<NearbyStop> streetAccesses;
  private final Collection<NearbyStop> streetEgresses;
  private final FlexIndex flexIndex;
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
    this.streetAccesses = streetAccesses;
    this.streetEgresses = egressTransfers;
    this.flexIndex = graph.index.getFlexIndex();
    this.accessFlexPathCalculator = new StreetFlexPathCalculator(graph, false);
    this.egressFlexPathCalculator = new StreetFlexPathCalculator(graph, true);

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

    LOG.info("Direct Routing - Accesses: " + this.flexAccessTemplates.stream()
    	.map(e -> e.getAccessEgressStop().getId() + "->" + e.getTransferStop().getId() + "\n")
    	.distinct().collect(Collectors.toList()));
    LOG.info("Direct Routing - Egresses: " + this.flexEgressTemplates.stream()
    	.map(e -> e.getAccessEgressStop().getId() + "->" + e.getTransferStop().getId() + "\n")
    	.distinct().collect(Collectors.toList()));

    Multimap<StopLocation, NearbyStop> streetEgressByStop = HashMultimap.create();
    streetEgresses.forEach(it -> streetEgressByStop.put(it.stop, it));

    Set<Itinerary> itineraries = new HashSet<>();

    for (FlexAccessTemplate template : this.flexAccessTemplates) {
      StopLocation transferStop = template.getTransferStop();

      LOG.debug("Direct Routing - Trip: " + template.getFlexTrip() + 
    		" Access: " + template.getAccessEgressStop().getId() + 
    		" Transfer: " + template.getTransferStop() +
      		" From:" + template.getFlexTrip().getStops().toArray()[template.fromStopIndex] + 
      		" To:" + template.getFlexTrip().getStops().toArray()[template.toStopIndex]);
      		
      if(LOG.isDebugEnabled()) {
	      for(FlexEgressTemplate egress : this.flexEgressTemplates) {
		      LOG.debug("Direct Routing - Trip: " + egress.getFlexTrip() + 
		    		" Egress: " + egress.getAccessEgressStop().getId() + 
		    		" Transfer: " + egress.getTransferStop() +       		
		    		" From:" + egress.getFlexTrip().getStops().toArray()[egress.fromStopIndex] + 
		      		" To:" + egress.getFlexTrip().getStops().toArray()[egress.toStopIndex]);
	      }
      }
      
      if (this.flexEgressTemplates.stream().anyMatch(t -> t.getAccessEgressStop().equals(transferStop))) {
        for(NearbyStop egress : streetEgressByStop.get(transferStop)) {

          Itinerary itinerary = template.createDirectItinerary(egress, arriveBy, departureTime, startOfTime);
          if (itinerary != null) {
            itineraries.add(itinerary);
          }
        }
      }
    }
    
    return itineraries;
  }

  public Collection<FlexAccessEgress> createFlexAccesses() {
    calculateFlexAccessTemplates();

    return this.flexAccessTemplates
        .stream()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .filter(e -> e != null)
        .collect(Collectors.toList());
  }

  public Collection<FlexAccessEgress> createFlexEgresses() {
    calculateFlexEgressTemplates();

    return this.flexEgressTemplates
        .stream()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .filter(e -> e != null)
        .collect(Collectors.toList());
  }

  private void calculateFlexAccessTemplates() {
    if (this.flexAccessTemplates != null) { return; }

    // Fetch the closest flexTrips reachable from the access stops
    this.flexAccessTemplates = getClosestFlexTrips(streetAccesses, true)
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
    this.flexEgressTemplates = getClosestFlexTrips(streetEgresses, false)
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

  private Stream<T2<NearbyStop, FlexTrip>> getClosestFlexTrips(Collection<NearbyStop> nearbyStops, boolean pickup) {
    // Find all trips reachable from the nearbyStops
    Collection<T2<NearbyStop, FlexTrip>> flexTripsReachableFromNearbyStops = nearbyStops
        .stream()
        .flatMap(accessEgress -> flexIndex
            .getFlexTripsByStop(accessEgress.stop)
            .filter(flexTrip -> pickup ? flexTrip.isBoardingPossible(accessEgress.stop, arriveBy == false ? departureTime : null) 
            		: flexTrip.isAlightingPossible(accessEgress.stop, arriveBy == false ? departureTime : null))
            .map(flexTrip -> new T2<>(accessEgress, flexTrip)))
        .collect(Collectors.toList());

    // Group all (NearbyStop, FlexTrip) tuples by flexTrip
    Collection<List<T2<NearbyStop, FlexTrip>>> groupedReachableFlexTrips = flexTripsReachableFromNearbyStops
    	.stream()
        .collect(Collectors.groupingBy(t2 -> t2.second))
        .values();

    // Get the stop with least walking time from each group 
    List<T2<NearbyStop, FlexTrip>> r = groupedReachableFlexTrips
        .stream()
        .map(t2s -> t2s
            .stream()
            .filter(t2 -> !t2.first.stop.isArea() && !t2.first.stop.isLine())
            .min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
    
    // ...and then the same (least from each group) for both lines and areas
    //
    // (we handle these separately since areas can contain stops, but stops can have special
    // rules that apply only to that one point inside the area vs. just getting the area)
    r.addAll(groupedReachableFlexTrips
            .stream()
            .map(t2s -> t2s
                .stream()
                .filter(t2 -> t2.first.stop.isArea() || t2.first.stop.isLine())
                .min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds() + 60)))
            .flatMap(Optional::stream)
            .collect(Collectors.toList()));
        
    return r.stream();
  }

}
