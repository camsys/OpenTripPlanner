package org.opentripplanner.index.model;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.Date;


public class StopTimesForPatternQuery {
    private TripPattern pattern;
    private Date startTime;
    private TimetableSnapshot snapshot;
    private Stop stop;
    private int timeRange;
    private int numberOfDepartures;
    private boolean omitNonPickups;
    private String headsign;
    private String tripHeadsign;
    private Collection<String> trackIds;
    private boolean showCancelledTrips;
    private boolean includeStopsForTrip;
    private boolean signMode;

    private StopTimesForPatternQuery(Builder builder){
        this.pattern = builder.pattern;
        this.startTime = builder.startTime;
        this.snapshot = builder.snapshot;
        this.stop = builder.stop;
        this.timeRange = builder.timeRange;
        this.numberOfDepartures = builder.numberOfDepartures;
        this.omitNonPickups = builder.omitNonPickups;
        this.headsign = builder.headsign;
        this.tripHeadsign = builder.tripHeadsign;
        this.trackIds = builder.trackIds;
        this.showCancelledTrips = builder.showCancelledTrips;
        this.includeStopsForTrip = builder.includeStopsForTrip;
        this.signMode = builder.signMode;
        this.includeTripPatterns = builder.includeTripPatterns;
    }
    public TripPattern getPattern() {
        return pattern;
    }

    public Date getStartTime() {
        return startTime;
    }

    public TimetableSnapshot getSnapshot() {
        return snapshot;
    }

    public Stop getStop() {
        return stop;
    }

    public int getTimeRange() {
        return timeRange;
    }

    public int getNumberOfDepartures() {
        return numberOfDepartures;
    }

    public boolean isOmitNonPickups() {
        return omitNonPickups;
    }

    public String getHeadsign() {
        return headsign;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public Collection<String> getTrackIds() {
        return trackIds;
    }

    public boolean isShowCancelledTrips() {
        return showCancelledTrips;
    }

    public boolean isIncludeStopsForTrip() {
        return includeStopsForTrip;
    }

    public boolean isSignMode() {
        return signMode;
    }

    public static class Builder{
        private TripPattern pattern;
        private Date startTime;
        private TimetableSnapshot snapshot;
        private Stop stop;
        private int timeRange = Integer.MAX_VALUE;
        private int numberOfDepartures = 1;
        private boolean omitNonPickups = true;
        private String headsign = null;
        private String tripHeadsign = null;
        private Collection<String> trackIds = null;
        private boolean showCancelledTrips = true;
        private boolean includeStopsForTrip = false;
        private boolean signMode = false;

        public Builder(TripPattern pattern, Date startTime, TimetableSnapshot snapshot, Stop stop){
            this.pattern = pattern;
            this.startTime = startTime;
            this.snapshot = snapshot;
            this.stop = stop;
        }

        public Builder timeRange(int timeRange){
            this.timeRange = timeRange;
            return this;
        }

        public Builder numberOfDepartures(int numberOfDepartures) {
            this.numberOfDepartures = numberOfDepartures;
            return this;
        }

        public Builder omitNonPickups(boolean omitNonPickups) {
            this.omitNonPickups = omitNonPickups;
            return this;
        }

        public Builder headsign(String headsign) {
            this.headsign = headsign;
            return this;
        }

        public Builder tripHeadsign(String tripHeadsign) {
            this.tripHeadsign = tripHeadsign;
            return this;
        }

        public Builder trackIds(Collection<String> trackIds) {
            this.trackIds = trackIds;
            return this;
        }

        public Builder showCancelledTrips(boolean showCancelledTrips) {
            this.showCancelledTrips = showCancelledTrips;
            return this;
        }

        public Builder includeStopsForTrip(boolean includeStopsForTrip) {
            this.includeStopsForTrip = includeStopsForTrip;
            return this;
        }

        public Builder signMode(boolean signMode) {
            this.signMode = signMode;
            return this;
        }

        public StopTimesForPatternQuery build(){
            StopTimesForPatternQuery query = new StopTimesForPatternQuery(this);
            return query;
        }
    }
}
