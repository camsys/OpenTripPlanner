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
    for (SimpleTransfer transfer : graph.transfersByStop.values()) {
      transfersToStop.put(transfer.to, transfer);
    }

    for (FlexTrip flexTrip : graph.flexTripsById.values()) {
      routeById.put(flexTrip.getTrip().getRoute().getId(), flexTrip.getTrip().getRoute());
      tripById.put(flexTrip.getTrip().getId(), flexTrip.getTrip());
      for (StopLocation stop : flexTrip.getStops()) {
        if (stop instanceof FlexLocationGroup) {
          for (StopLocation stopElement : ((FlexLocationGroup) stop).getLocations()) {
            flexTripsByStop.put(stopElement, flexTrip);
          }
        } else {
          flexTripsByStop.put(stop, flexTrip);
        }
      }
      // Adding map to track stops that allow pickups
      for(FlexTripStopTime flexTripStopTime : flexTrip.getStopTimes()){
        StopLocation flexStopLocation = flexTripStopTime.stop;
        if(flexTripStopTime.pickupType != PICKDROP_NONE){
          flexTripStopsWithPickup.put(flexTrip.getId().toString(), flexStopLocation.getId().toString());
          if(flexStopLocation instanceof FlexLocationGroup){
            for (StopLocation stopElement : ((FlexLocationGroup) flexStopLocation).getLocations()) {
              flexTripStopsWithPickup.put(flexTrip.getId().toString(), stopElement.getId().toString());
            }
          }
        }
        if(flexTripStopTime.dropOffType != PICKDROP_NONE){
          flexTripStopsWithDropoff.put(flexTrip.getId().toString(), flexStopLocation.getId().toString());
          if(flexStopLocation instanceof FlexLocationGroup){
            for (StopLocation stopElement : ((FlexLocationGroup) flexStopLocation).getLocations()) {
              flexTripStopsWithDropoff.put(flexTrip.getId().toString(), stopElement.getId().toString());
            }
          }
        }
      }
    }
    
    for (FlexLocationGroup flexLocationGroup : graph.locationGroupsById.values()) {
      for (StopLocation stop : flexLocationGroup.getLocations()) {
        locationGroupsByStop.put(stop, flexLocationGroup);
      }
    }
    
    for (FlexStopLocation flexStopLocation : graph.locationsById.values()) {
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

}
