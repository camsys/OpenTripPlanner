package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
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
 * This type of FlexTrip is used when a taxi-type service is modeled, which operates in one or
 * between two areas/groups of stops without a set schedule. The travel times are calculated based
 * on the driving time between the stops, with the schedule times being used just for deciding if a
 * trip is possible.
 */
public class UnscheduledTrip extends FlexTrip {
	
  private static final Logger LOG = LoggerFactory.getLogger(UnscheduledTrip.class);

  private static final long serialVersionUID = -3994562856996189076L;

  private static final int N_STOPS = 2;

  private final UnscheduledStopTime[] stopTimes;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  public static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> noExplicitTimes = Predicate.not(st -> st.isArrivalTimeSet() || st.isDepartureTimeSet());
    Predicate<StopTime> notContinuousStop = stopTime ->
        stopTime.getFlexContinuousDropOff() == PICKDROP_NONE && stopTime.getFlexContinuousPickup() == PICKDROP_NONE;
    return stopTimes.size() == N_STOPS
        && stopTimes.stream().allMatch(noExplicitTimes)
        && stopTimes.stream().allMatch(notContinuousStop);
  }

  public UnscheduledTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isUnscheduledTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for unscheduled trip");
    }

    this.stopTimes = new UnscheduledStopTime[N_STOPS];
    this.dropOffBookingInfos = new BookingInfo[N_STOPS];
    this.pickupBookingInfos = new BookingInfo[N_STOPS];

    for (int i = 0; i < N_STOPS; i++) {
      this.stopTimes[i] = new UnscheduledStopTime(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(0).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(0).getPickupBookingInfo();
    }
  }

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate serviceDate, FlexPathCalculator calculator
  ) {
	List<Integer> fromIndices = getFromIndex(access.stop, null);
    if (fromIndices.isEmpty()) { return Stream.empty(); }
    if (stopTimes[1].dropOffType == PICKDROP_NONE) { return Stream.empty(); }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

    for (StopLocation stop : expandStops(stopTimes[1].stop)) {
    	for(Integer fromIndex : fromIndices) 
    		res.add(new FlexAccessTemplate(access, this, fromIndex, 1, stop, serviceDate, calculator));
    }

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate serviceDate, FlexPathCalculator calculator
  ) {
    List<Integer> toIndices = getToIndex(egress.stop, null);
    if (toIndices.isEmpty()) { return Stream.empty(); }
    if (stopTimes[0].pickupType == PICKDROP_NONE) { return Stream.empty(); }

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

    for (StopLocation stop : expandStops(stopTimes[0].stop)) {
    	for(Integer toIndex : toIndices) 
    		res.add(new FlexEgressTemplate(egress, this, 0, toIndex, stop, serviceDate, calculator));
    }

    return res.stream();
  }

  @Override
  public int earliestDepartureTime(int departureTime, int fromStopIndex, int toStopIndex) {
	UnscheduledStopTime fromStopTime = stopTimes[fromStopIndex];
	UnscheduledStopTime toStopTime = stopTimes[toStopIndex];

    if(fromStopTime.flexWindowStart == MISSING_VALUE)
    	return -1;
    
    if (fromStopTime.flexWindowEnd < departureTime)
        return -1;

    return Math.max(departureTime, fromStopTime.flexWindowStart);
  }

  @Override
  public int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex) {
	UnscheduledStopTime fromStopTime = stopTimes[fromStopIndex];
	UnscheduledStopTime toStopTime = stopTimes[toStopIndex];
	    
    if(toStopTime.flexWindowEnd == MISSING_VALUE)
    	return -1;
    
    if (toStopTime.flexWindowStart > arrivalTime)
        return -1;

    return Math.min(arrivalTime, toStopTime.flexWindowEnd);
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
  public boolean isBoardingPossible(StopLocation stop, int time) {
    return !getFromIndex(stop, time).isEmpty();
  }

  @Override
  public boolean isAlightingPossible(StopLocation stop, int time) {
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
	for (int i = stopTimes.length - 1; i >= 0; i--) {
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
  public int getSafeTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex) {
	    UnscheduledStopTime fromStopTime = this.stopTimes[fromStopIndex];
	    UnscheduledStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			int safeFactor = Math.max(fromStopTime.safeFactor, toStopTime.safeFactor);
			int safeOffset = Math.max(fromStopTime.safeOffset, toStopTime.safeOffset);
			if(safeFactor != MISSING_VALUE && safeOffset != MISSING_VALUE)
				return (safeFactor * streetPath.durationSeconds) + safeOffset;
		}
		return streetPath.durationSeconds;					
  }

  @Override
  public int getMeanTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex) {
	    UnscheduledStopTime fromStopTime = this.stopTimes[fromStopIndex];
		UnscheduledStopTime toStopTime = this.stopTimes[toStopIndex];
		
		if(fromStopTime.stop.isArea() || toStopTime.stop.isArea()) {
			int meanFactor = Math.max(fromStopTime.meanFactor, toStopTime.meanFactor);
			int meanOffset = Math.max(fromStopTime.meanOffset, toStopTime.meanOffset);
			if(meanFactor != MISSING_VALUE && meanOffset != MISSING_VALUE)
				return (meanFactor * streetPath.durationSeconds) + meanOffset;
		}
		return streetPath.durationSeconds;
  }
  
  private static class UnscheduledStopTime implements Serializable {
	private static final long serialVersionUID = 8473095807707616815L;

	private final StopLocation stop;

    private final int safeFactor;
    private final int safeOffset;
    private final int meanFactor;
    private final int meanOffset;

    private final int flexWindowStart;
    private final int flexWindowEnd;
    
    private final int pickupType;
    private final int dropOffType;

    private UnscheduledStopTime(StopTime st) {
      stop = st.getStop();
      
      this.safeFactor = st.getSafeDurationFactor();
      this.safeOffset = st.getSafeDurationOffset();

      this.meanFactor = st.getMeanDurationFactor();
      this.meanOffset = st.getMeanDurationOffset();

      this.flexWindowStart = st.getFlexWindowStart();
      this.flexWindowEnd = st.getFlexWindowEnd();

      this.pickupType = st.getPickupType();
      this.dropOffType = st.getDropOffType();
    }
  }
}
