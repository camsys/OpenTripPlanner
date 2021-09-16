package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopTime.MISSING_VALUE;

/**
 * A scheduled deviated trip is similar to a regular scheduled trip, except that is continues stop
 * locations, which are not stops, but other types, such as groups of stops or location areas.
 */
public class ScheduledDeviatedTrip extends FlexTrip {

  private static final long serialVersionUID = -8704964150428339679L;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  public static boolean isScheduledFlexTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> notStopType = Predicate.not(st -> st.getStop() instanceof Stop);
    Predicate<StopTime> notContinuousStop = stopTime -> 
    	stopTime.getFlexContinuousDropOff() == PICKDROP_NONE && 
    	stopTime.getFlexContinuousPickup() == PICKDROP_NONE;
    return stopTimes.stream().anyMatch(notStopType)
        && stopTimes.stream().allMatch(notContinuousStop);
  }

  public ScheduledDeviatedTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isScheduledFlexTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for scheduled flex trip");
    }

    int nStops = stopTimes.size();
    this.stopTimes = new FlexTripStopTime[nStops];
    this.dropOffBookingInfos = new BookingInfo[nStops];
    this.pickupBookingInfos = new BookingInfo[nStops];

    for (int i = 0; i < nStops; i++) {
      this.stopTimes[i] = new FlexTripStopTime(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(i).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(i).getPickupBookingInfo();
    }
  }

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate serviceDate, FlexPathCalculator calculator
  ) {
    List<Integer> fromIndices = getFromIndex(access.stop, null);
    if (fromIndices.isEmpty()) { return Stream.empty(); }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

	for(Integer fromIndex : fromIndices) {
	    for (int toIndex = fromIndex; toIndex < stopTimes.length; toIndex++) {
	      if (stopTimes[toIndex].dropOffType == PICKDROP_NONE) continue;
	        for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
	          res.add(new FlexAccessTemplate(access, this, fromIndex, toIndex, stop, serviceDate, calculator));
	      }
	    }
	}

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate serviceDate, FlexPathCalculator calculator
  ) {
    List<Integer> toIndices = getToIndex(egress.stop, null);
    if (toIndices.isEmpty()) { return Stream.empty(); }

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

	for(Integer toIndex : toIndices) {
	    for (int fromIndex = toIndex; fromIndex >= 0; fromIndex--) {
	      if (stopTimes[fromIndex].pickupType == PICKDROP_NONE) continue;
	      for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
	        res.add(new FlexEgressTemplate(egress, this, fromIndex, toIndex, stop, serviceDate, calculator));
	      }
	    }
	}
	
    return res.stream();
  }
  
  @Override
  public int earliestDepartureTime(int departureTime, int fromStopIndex, int toStopIndex) {
	int stopDepartureTime = MISSING_VALUE;
    for (int i = fromStopIndex; stopDepartureTime == MISSING_VALUE && i >= 0; i--) {
    	stopDepartureTime = stopTimes[i].flexWindowStart;
    }
    if(stopDepartureTime == MISSING_VALUE)
    	return -1;
    return stopDepartureTime >= departureTime ? stopDepartureTime : -1;
  }

  @Override
  public int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex) {
    int stopArrivalTime = MISSING_VALUE;
    for (int i = toStopIndex; stopArrivalTime == MISSING_VALUE && i < stopTimes.length; i++) {
      stopArrivalTime = stopTimes[i].flexWindowEnd;
    }
    if(stopArrivalTime == MISSING_VALUE)
    	return -1;
    return stopArrivalTime <= arrivalTime ? stopArrivalTime : -1;
  }

  @Override
  public Collection<StopLocation> getStops() {
    return Arrays
        .stream(stopTimes)
        .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
        .collect(Collectors.toSet());
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int i) {
    return dropOffBookingInfos[i];
  }

  @Override
  public BookingInfo getPickupBookingInfo(int i) {
    return pickupBookingInfos[i];
  }

  @Override
  public boolean isBoardingPossible(StopLocation stop) {
    return !getFromIndex(stop, null).isEmpty();
  }

  @Override
  public boolean isAlightingPossible(StopLocation stop) {
    return !getToIndex(stop, null).isEmpty();
  }

  @Override
  public boolean isBoardingPossible(StopLocation stop, Integer time) {
    return !getFromIndex(stop, time).isEmpty();
  }

  @Override
  public boolean isAlightingPossible(StopLocation stop, Integer time) {
    return !getToIndex(stop, time).isEmpty();
  }

  private Collection<StopLocation> expandStops(StopLocation stop) {
    return stop instanceof FlexLocationGroup
        ? ((FlexLocationGroup) stop).getLocations()
        : Collections.singleton(stop);
  }

  private List<Integer> getFromIndex(StopLocation accessEgress, Integer time) {
	ArrayList<Integer> r = new ArrayList<Integer>();
    for (int i = 0; i < stopTimes.length; i++) {
      if (stopTimes[i].pickupType == PICKDROP_NONE) continue; // No pickup allowed here
      if(time != null) {
    	  if(!(time >= stopTimes[i].flexWindowStart && time <= stopTimes[i].flexWindowEnd))
    		  continue;
      }

      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof FlexLocationGroup) {
        if (((FlexLocationGroup) stop).getLocations().contains(accessEgress) )
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
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (stopTimes[i].dropOffType == PICKDROP_NONE) continue; // No drop off allowed here
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
	  	FlexTripStopTime fromStopTime = this.stopTimes[fromStopIndex];
	  	FlexTripStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			double safeFactor = fromStopTime.safeFactor;
			double safeOffset =fromStopTime.safeOffset;
			if(safeFactor != MISSING_VALUE && safeOffset != MISSING_VALUE)
				return (float)(safeFactor * streetPath.durationSeconds) + (float)(safeOffset * 60);
		}
		return streetPath.durationSeconds;					
  }

  @Override
  public float getMeanTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex) {
	  	FlexTripStopTime fromStopTime = this.stopTimes[fromStopIndex];
	  	FlexTripStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			double meanFactor = fromStopTime.meanFactor;
			double meanOffset = fromStopTime.meanOffset;
			if(meanFactor != MISSING_VALUE && meanOffset != MISSING_VALUE)
				return (float)(meanFactor * streetPath.durationSeconds) +  (float)(meanOffset * 60);
		}
		return streetPath.durationSeconds;
  }

}
