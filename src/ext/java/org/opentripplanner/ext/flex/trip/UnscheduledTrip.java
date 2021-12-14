package org.opentripplanner.ext.flex.trip;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;


/**
 * This type of FlexTrip is used when a taxi-type service is modeled, which operates in one or
 * between two areas/groups of stops without a set schedule. The travel times are calculated based
 * on the driving time between the stops, with the schedule times being used just for deciding if a
 * trip is possible.
 */
public class UnscheduledTrip extends FlexTrip {
	
  private static final long serialVersionUID = -3994562856996189076L;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  public static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
	// all areas, all time ranges	  
	return stopTimes.stream().allMatch(it -> it.getStop().isArea() && it.getStop().isLine())
			&& stopTimes.stream().allMatch(it -> it.getArrivalTime() == StopTime.MISSING_VALUE 
			&& it.getDepartureTime() == StopTime.MISSING_VALUE);	  
  }

  public UnscheduledTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isUnscheduledTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for unscheduled trip");
    }

    this.stopTimes = new FlexTripStopTime[stopTimes.size()];
    this.dropOffBookingInfos = new BookingInfo[stopTimes.size()];
    this.pickupBookingInfos = new BookingInfo[stopTimes.size()];

    for (int i = 0; i < stopTimes.size(); i++) {
      this.stopTimes[i] = new FlexTripStopTime(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(0).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(0).getPickupBookingInfo();
    }
  }

  @Override
  public List<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate serviceDate, FlexPathCalculator calculator, RoutingRequest request
  ) {
	List<Integer> fromIndices = getFromIndex(access.stop, null);
    if (fromIndices.isEmpty()) { return List.of(); }
//    if (stopTimes[1].dropOffType == PICKDROP_NONE) { return Stream.empty(); }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

	for(Integer fromIndex : fromIndices) 
		for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
    		res.add(new FlexAccessTemplate(access, this, fromIndex, fromIndex, stop, serviceDate, calculator, request));
    }

    return res;
  }

  @Override
  public List<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate serviceDate, FlexPathCalculator calculator, RoutingRequest request
  ) {
    List<Integer> toIndices = getToIndex(egress.stop, null);
    if (toIndices.isEmpty()) { return List.of(); }
//    if (stopTimes[0].pickupType == PICKDROP_NONE) { return Stream.empty(); }

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

	for(Integer toIndex : toIndices) 
		for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
    		res.add(new FlexEgressTemplate(egress, this, toIndex, toIndex, stop, serviceDate, calculator, request));
    }

    return res;
  }

  // See RaptorTransfer for definitions/semantics
  @Override
  public int earliestDepartureTime(int departureTime, int fromStopIndex, int toStopIndex) {
	FlexTripStopTime ftst = stopTimes[fromStopIndex];
	    
	if (departureTime < ftst.flexWindowEnd) // latest possible time bus will leave stop 
		return -1;

	return departureTime;
  }

  // See RaptorTransfer for definitions/semantics
  @Override
  public int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex) {
	FlexTripStopTime ftst = stopTimes[toStopIndex];
	    
  if (arrivalTime < ftst.flexWindowStart) // earliest possible arrival time
        return -1;

	return arrivalTime;
  }

  @Override
  public List<StopLocation> getStops() {
    return Arrays
        .stream(stopTimes)
        .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
        .collect(Collectors.toList());
  }
  
  @Override
  public BookingInfo getDropOffBookingInfo(int i) {
    return dropOffBookingInfos[i];
  }

  @Override
  public BookingInfo getPickupBookingInfo(int i) {
    return pickupBookingInfos[i];
  }

  private static Collection<StopLocation> expandStops(StopLocation stop) {
    return stop instanceof FlexLocationGroup
        ? ((FlexLocationGroup) stop).getLocations()
        : Collections.singleton(stop);
  }

  private List<Integer> getFromIndex(StopLocation accessEgress, Integer time) {
	ArrayList<Integer> r = new ArrayList<Integer>();
    for (int i = 0; i < stopTimes.length; i++) {
      if (stopTimes[i].pickupType == PICKDROP_NONE) continue;
      if(time != null) {
    	  if(!(time >= stopTimes[i].flexWindowStart && time <= stopTimes[i].flexWindowEnd))
    		  continue;
      }

      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof FlexLocationGroup) {
        if (((FlexLocationGroup) stop).getLocations().contains(accessEgress))
          r.add(i);
      } else {
        if (stop.equals(accessEgress))
          r.add(i);
      }
    }
	return r;
  }

  private List<Integer> getToIndex(StopLocation accessEgress, Integer time) {
	ArrayList<Integer> r = new ArrayList<Integer>();
    for (int i = 0; i < stopTimes.length; i++) {
      if (stopTimes[i].dropOffType == PICKDROP_NONE) continue;
      if(time != null) {
    	  if(!(time >= stopTimes[i].flexWindowStart && time <= stopTimes[i].flexWindowEnd))
    		  continue;
      }

      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof FlexLocationGroup) {
        if (((FlexLocationGroup) stop).getLocations().contains(accessEgress))
          r.add(i);
      } else {
        if (stop.equals(accessEgress))
          r.add(i);
      }
    }
	return r;
  }

  @Override
  public float getSafeTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex) {
	  	if(streetPath == null)
	  		return -1;
	  	
	  	FlexTripStopTime fromStopTime = this.stopTimes[fromStopIndex];
	  	FlexTripStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			double safeFactor = fromStopTime.safeFactor;
			double safeOffset = fromStopTime.safeOffset;
			if(safeFactor != MISSING_VALUE && safeOffset != MISSING_VALUE)
				return (float)(safeFactor * streetPath.durationSeconds) + (float)(safeOffset * 60);
		}
		return streetPath.durationSeconds;					
  }

  @Override
  public float getMeanTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex) {
	  	if(streetPath == null)
	  		return -1;
	  	
	  	FlexTripStopTime fromStopTime = this.stopTimes[fromStopIndex];
	  	FlexTripStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			double meanFactor = fromStopTime.meanFactor;
			double meanOffset = fromStopTime.meanOffset;
			if(meanFactor != MISSING_VALUE && meanOffset != MISSING_VALUE)
				return (float)(meanFactor * streetPath.durationSeconds) + (float)(meanOffset * 60);
		}
		return streetPath.durationSeconds;
  }

}
