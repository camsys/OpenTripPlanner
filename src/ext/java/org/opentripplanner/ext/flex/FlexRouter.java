package org.opentripplanner.ext.flex;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.model.*;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;

public class FlexRouter {

  private static final Logger LOG = LoggerFactory.getLogger(FlexRouter.class);

  private final RoutingRequest request;
  
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
      Collection<NearbyStop> egressTransfers,
      RoutingRequest request
  ) {
    this.graph = graph;
    this.flexIndex = graph.index.getFlexIndex();
    this.accessFlexPathCalculator = new StreetFlexPathCalculator(graph, false);
    this.egressFlexPathCalculator = new StreetFlexPathCalculator(graph, true);
    this.streetAccesses = streetAccesses;
    this.streetEgresses = egressTransfers;
    this.request = request;
    
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
    		  this.flexEgressTemplates.parallelStream().distinct().filter(t -> t.getTransferStop().equals(transferStop)).collect(Collectors.toList());

      if (!egressTemplates.isEmpty()) {
        for(FlexEgressTemplate egressTemplate : egressTemplates) {
            Itinerary itin = makeItinerary(template, egressTemplate, flexIndex);
            if (itin != null) {
                itineraries.add(itin);
            }
        }
      }
    }
    
    return itineraries;
  }

  public Itinerary makeItinerary(FlexAccessTemplate template, FlexEgressTemplate egressTemplate, FlexIndex flexIndex) {
      if (!template.getFlexTrip().equals(egressTemplate.getFlexTrip())) {
          LOG.debug("Trip mismatch found " + template.getFlexTrip() + " != " + egressTemplate.getFlexTrip());
          return null;
      }

      ZonedDateTime departureServiceDate= template.serviceDate.serviceDate.toZonedDateTime(startOfTime.getZone(),startOfTime.getSecond());
      Itinerary itinerary = template.createDirectItinerary(egressTemplate.getAccessEgress(), arriveBy,
              departureTime,departureServiceDate, flexIndex);

      if (itinerary != null) {
          LOG.debug("Creating itin for trip " + egressTemplate.getFlexTrip()+"/" +template.getFlexTrip() + " from:" + template.getAccessEgressStop() + " to:" +
                  egressTemplate.getAccessEgressStop() + " xfr=" + template.getTransferStop() + " itin=" + itinerary);
      }

      return itinerary;
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
  
  public Collection<FlexAccessEgress> createFlexAccesses() {
    calculateFlexAccessTemplates();

    return this.flexAccessTemplates
        .stream()
        .distinct()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .filter(e -> e != null)
        .distinct()
        .collect(Collectors.toList());
  }

  public Collection<FlexAccessEgress> createFlexEgresses() {
      calculateFlexEgressTemplatesForRaptor();

    return this.flexEgressTemplates
        .stream()
        .distinct()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .filter(e -> e != null)
        .distinct()
        .collect(Collectors.toList());
  }

  private void calculateFlexAccessTemplates() {
    if (this.flexAccessTemplates != null) { return; }

    this.flexAccessTemplates = new ArrayList<>();
    for(Entry<NearbyStop, Collection<FlexTrip>> e : getClosestFlexTrips(streetAccesses, true)) {
    	for(FlexTrip trip : e.getValue()) {
        	for(int i = 0; i < dates.length - 1; i++) {
        		FlexServiceDate date = dates[i];
        		if(date.isFlexTripRunning(trip, graph))
        			flexAccessTemplates.addAll(
        				trip.getFlexAccessTemplates(e.getKey(), date, accessFlexPathCalculator, request));
        	}
    	}
    }
  }

  private void calculateFlexEgressTemplates() {
    if (this.flexEgressTemplates != null) { return; }

    this.flexEgressTemplates = new ArrayList<>();
    for(Entry<NearbyStop, Collection<FlexTrip>> e : getClosestFlexTrips(streetEgresses, false)) {
    	for(FlexTrip trip : e.getValue()) {
        	for(int i = 0; i < dates.length - 1; i++) {
        		FlexServiceDate date = dates[i];
        		if(date.isFlexTripRunning(trip, graph))
        			flexEgressTemplates.addAll(
        				trip.getFlexEgressTemplates(e.getKey(), date, egressFlexPathCalculator, request));
        	}
    	}
    }
  }

    private void calculateFlexEgressTemplatesForRaptor() {
        if (this.flexEgressTemplates != null) { return; }

        this.flexEgressTemplates = new ArrayList<>();
        for(Entry<NearbyStop, Collection<FlexTrip>> e : getClosestFlexTrips(streetEgresses, false)) {
            for(FlexTrip trip : e.getValue()) {
                for(int i = 0; i < dates.length - 1; i++) {
                    FlexServiceDate date = dates[i];
                    if(date.isFlexTripRunning(trip, graph))
                        flexEgressTemplates.addAll(
                                trip.getFlexEgressTemplatesForRaptor(e.getKey(), date, egressFlexPathCalculator, request));
                }
            }
        }
    }

  private Set<Entry<NearbyStop, Collection<FlexTrip>>> getClosestFlexTrips(Collection<NearbyStop> nearbyStops, boolean isAccess) {

	// Find all trips reachable from the nearbyStops
    Collection<T2<NearbyStop, FlexTrip>> flexTripsReachableFromNearbyStops = nearbyStops
        .parallelStream()
        .flatMap(nearbyStop -> flexIndex
            .getFlexTripsByStop(nearbyStop.stop)
                .filter(flexTrip -> {
                    boolean hasStopThatAllowsPickupDropoff;
                    if(isAccess) {
                        hasStopThatAllowsPickupDropoff = flexIndex.hasStopThatAllowsPickup(flexTrip, nearbyStop.stop);
                    } else {
                        hasStopThatAllowsPickupDropoff = flexIndex.hasStopThatAllowsDropoff(flexTrip, nearbyStop.stop);
                    }
                    return hasStopThatAllowsPickupDropoff;
                })
            .map(flexTrip -> new T2<>(nearbyStop, flexTrip)))
        .collect(Collectors.toList());

    // Group all (NearbyStop, FlexTrip) tuples by flexTrip
    Collection<List<T2<NearbyStop, FlexTrip>>> groupedReachableFlexTrips = flexTripsReachableFromNearbyStops
    	.parallelStream()
        .distinct()
        .collect(Collectors.groupingBy(t2 -> t2.second.getId()))
        .values();

    HashMultimap<NearbyStop, FlexTrip> r = HashMultimap.create();
    
    // Get the stop with least walking time from each group 
    groupedReachableFlexTrips
    	.stream()
        .map(t2s -> t2s
            .parallelStream()
            .filter(t2 -> !t2.first.stop.isArea() && !t2.first.stop.isLine())
            .min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
        .flatMap(Optional::stream)
        .forEach(it -> {
        	r.put(it.first, it.second);
        });
    
    // ...and then get the same (least from each group) for both lines and areas
    //
    // (we handle these separately since areas can contain stops, but stops can have special
    // rules that apply only to that one point inside the area vs. just getting the area), so we want both 
    // in the list of options
    groupedReachableFlexTrips
            .stream()
            .map(t2s -> t2s
                .parallelStream()
                .filter(t2 -> t2.first.stop.isArea() || t2.first.stop.isLine())
                .min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
            .flatMap(Optional::stream)
            .forEach(it -> {
            	r.put(it.first, it.second);
            });

    return r.asMap().entrySet();
  }

}
