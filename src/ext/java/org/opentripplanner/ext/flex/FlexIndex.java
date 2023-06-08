package org.opentripplanner.ext.flex;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graph.Graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

public class FlexIndex {
  public Multimap<StopLocation, SimpleTransfer> transfersToStop = ArrayListMultimap.create();

  public Multimap<StopLocation, FlexTrip> flexTripsByStop = HashMultimap.create();

  public Multimap<StopLocation, FlexLocationGroup> locationGroupsByStop = ArrayListMultimap.create();

  public HashGridSpatialIndex<FlexStopLocation> locationIndex = new HashGridSpatialIndex<FlexStopLocation>();

  public Map<FeedScopedId, Route> routeById = new HashMap<>();

  public Map<FeedScopedId, Trip> tripById = new HashMap<>();

  public Multimap<String, String> flexTripStopsWithPickup = HashMultimap.create();

  public Multimap<String, String> flexTripStopsWithDropoff = HashMultimap.create();

  public FlexIndex(Graph graph) {
    processTransfers(graph.transfersByStop.values());
    processFlexTrips(graph.flexTripsById.values());
    processLocationGroups(graph.locationGroupsById.values());
    processStopLocations(graph.locationsById.values());
  }

  private void processTransfers(Collection<SimpleTransfer> transfers) {
    for (SimpleTransfer transfer : transfers) {
      transfersToStop.put(transfer.to, transfer);
    }
  }

  private void processFlexTrips(Collection<FlexTrip> flexTrips) {
    for (FlexTrip flexTrip : flexTrips) {
      addTripAndRouteToMaps(flexTrip);
      addStopsToFlexTripsMap(flexTrip);
      processStopTimes(flexTrip);
    }
  }

  private void addTripAndRouteToMaps(FlexTrip flexTrip) {
    Trip trip = flexTrip.getTrip();
    routeById.put(trip.getRoute().getId(), trip.getRoute());
    tripById.put(trip.getId(), trip);
  }

  private void addStopsToFlexTripsMap(FlexTrip flexTrip) {
    for (StopLocation stop : flexTrip.getStops()) {
      addStopToMultimap(flexTripsByStop, stop, flexTrip);
    }
  }

  private void processStopTimes(FlexTrip flexTrip) {
    for(FlexTripStopTime flexTripStopTime : flexTrip.getStopTimes()){
      StopLocation flexStopLocation = flexTripStopTime.stop;
      if(flexTripStopTime.pickupType != PICKDROP_NONE){
        addStopToMultimap(flexTripStopsWithPickup, flexTrip.getId().toString(), flexStopLocation);
      }
      if(flexTripStopTime.dropOffType != PICKDROP_NONE){
        addStopToMultimap(flexTripStopsWithDropoff, flexTrip.getId().toString(), flexStopLocation);
      }
    }
  }

  private void addStopToMultimap(Multimap<String, String> multimap, String tripId, StopLocation stop) {
    multimap.put(tripId, stop.getId().toString());
    if (stop instanceof FlexLocationGroup) {
      for (StopLocation stopElement : ((FlexLocationGroup) stop).getLocations()) {
        multimap.put(tripId, stopElement.getId().toString());
      }
    }
  }

  private void addStopToMultimap(Multimap<StopLocation, FlexTrip> multimap, StopLocation stop, FlexTrip flexTrip) {
    multimap.put(stop, flexTrip);
    if (stop instanceof FlexLocationGroup) {
      for (StopLocation stopElement : ((FlexLocationGroup) stop).getLocations()) {
        multimap.put(stopElement, flexTrip);
      }
    }
  }

  private void processLocationGroups(Collection<FlexLocationGroup> locationGroups) {
    for (FlexLocationGroup flexLocationGroup : locationGroups) {
      for (StopLocation stop : flexLocationGroup.getLocations()) {
        locationGroupsByStop.put(stop, flexLocationGroup);
      }
    }
  }

  private void processStopLocations(Collection<FlexStopLocation> stopLocations) {
    for (FlexStopLocation flexStopLocation : stopLocations) {
      locationIndex.insert(flexStopLocation.getGeometry().getEnvelopeInternal(), flexStopLocation);
    }
  }

  public Stream<FlexTrip> getFlexTripsByStop(StopLocation stopLocation) {
    return flexTripsByStop.get(stopLocation).stream();
  }

  public boolean hasStopThatAllowsPickup(FlexTrip trip, StopLocation stopLocation){
    if(flexTripStopsWithPickup.containsKey(trip.getId().toString())){
      return flexTripStopsWithPickup.get(trip.getId().toString()).contains(stopLocation.getId().toString());
    }
    return false;
  }

  public boolean hasStopThatAllowsDropoff(FlexTrip trip, StopLocation stopLocation){
    if(flexTripStopsWithDropoff.containsKey(trip.getId().toString())){
      return flexTripStopsWithDropoff.get(trip.getId().toString()).contains(stopLocation.getId().toString());
    }
    return false;
  }


  public void reset() {
    transfersToStop.clear();
    flexTripsByStop.clear();
    locationGroupsByStop.clear();
    locationIndex.reset();
    routeById.clear();
    tripById.clear();
    flexTripStopsWithPickup.clear();
    flexTripStopsWithDropoff.clear();
  }
}
