package org.opentripplanner.api.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.FeedScopedId;

import java.util.List;

public class TripTimesResponse {

	FeedScopedId tripId;
	
	List<TripTimeShort> stopTimes;
	
	public TripTimesResponse(FeedScopedId tripId, List<TripTimeShort> stopTimes) {
		this.tripId = tripId;
		this.stopTimes = stopTimes;
	}
	
	public FeedScopedId getTripId() {
		return tripId;
	}

	public void setTripId(FeedScopedId tripId) {
		this.tripId = tripId;
	}

	public List<TripTimeShort> getStopTimes() {
		return stopTimes;
	}

	public void setStopTimes(List<TripTimeShort> stopTimes) {
		this.stopTimes = stopTimes;
	}
	
}
