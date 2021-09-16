package org.opentripplanner.ext.flex.trip;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.io.Serializable;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;

public class FlexTripStopTime implements Serializable {

	private static final long serialVersionUID = 8473095807707616815L;

	public StopLocation stop;

	public double safeFactor;
	public double safeOffset;
	public double meanFactor;
	public double meanOffset;

	public int arrivalTime;
	public int departureTime;

	public int flexWindowStart;
	public int flexWindowEnd;

	public int pickupType;
	public int dropOffType;

	public FlexTripStopTime(StopTime st) {
		stop = st.getStop();

		this.safeFactor = st.getSafeDurationFactor();
		this.safeOffset = st.getSafeDurationOffset();

		this.meanFactor = st.getMeanDurationFactor();
		this.meanOffset = st.getMeanDurationOffset();

		this.flexWindowStart = st.getFlexWindowStart();
		this.flexWindowEnd = st.getFlexWindowEnd();

        this.arrivalTime = st.getArrivalTime() != MISSING_VALUE ? 
        		  st.getArrivalTime() : st.getFlexWindowStart();
          
        this.departureTime = st.getDepartureTime() != MISSING_VALUE ? 
        		  st.getDepartureTime() : st.getFlexWindowEnd();

		this.pickupType = st.getPickupType();
		this.dropOffType = st.getDropOffType();
	}
}
