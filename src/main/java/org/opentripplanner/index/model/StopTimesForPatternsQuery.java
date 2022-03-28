package org.opentripplanner.index.model;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class StopTimesForPatternsQuery {
    private long startTime;
    private Stop stop;
    private int timeRange;
    private int numberOfDepartures;
    private boolean omitNonPickups;
    private RouteMatcher routeMatcher;
    private Integer direction;
    private String headsign;
    private String tripHeadsign;
    private Stop requiredStop;
    private Set<String> bannedAgencies;
    private Set<Integer> bannedRouteTypes;
    private List<TripPattern> patterns;
    private Collection<String> trackIds;
    private boolean showCancelledTrips;
    private boolean includeStopsForTrip;
    private boolean signMode;
    private boolean ignoreRealtimeUpdates;
    private boolean includeTripPatterns;

    private StopTimesForPatternsQuery(Builder builder){
        this.startTime = builder.startTime;
        this.stop = builder.stop;
        this.timeRange = builder.timeRange;
        this.numberOfDepartures = builder.numberOfDepartures;
        this.omitNonPickups = builder.omitNonPickups;
        this.routeMatcher = builder.routeMatcher;
        this.direction = builder.direction;
        this.headsign = builder.headsign;
        this.tripHeadsign = builder.tripHeadsign;
        this.requiredStop = builder.requiredStop;
        this.bannedAgencies = builder.bannedAgencies;
        this.bannedRouteTypes = builder.bannedRouteTypes;
        this.patterns = builder.patterns;
        this.trackIds = builder.trackIds;
        this.showCancelledTrips = builder.showCancelledTrips;
        this.includeStopsForTrip = builder.includeStopsForTrip;
        this.signMode = builder.signMode;
        this.ignoreRealtimeUpdates = builder.ignoreRealtimeUpdates;
        this.includeTripPatterns = builder.includeTripPatterns;

    }

    public long getStartTime() {
        return startTime;
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

    public RouteMatcher getRouteMatcher() {
        return routeMatcher;
    }

    public Integer getDirection() {
        return direction;
    }

    public String getHeadsign() {
        return headsign;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public Stop getRequiredStop() {
        return requiredStop;
    }

    public Set<String> getBannedAgencies() {
        return bannedAgencies;
    }

    public Set<Integer> getBannedRouteTypes() {
        return bannedRouteTypes;
    }

    public List<TripPattern> getPatterns() {
        return patterns;
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

    public boolean includeTripPatterns(){
        return includeTripPatterns;
    }

    public static class Builder{
        private long startTime;
        private Stop stop;
        private int timeRange;
        private int numberOfDepartures = 2;
        private boolean omitNonPickups = true;
        private RouteMatcher routeMatcher = RouteMatcher.emptyMatcher();
        private Integer direction;
        private String headsign;
        private String tripHeadsign;
        private Stop requiredStop;
        private Set<String> bannedAgencies;
        private Set<Integer> bannedRouteTypes;
        private List<TripPattern> patterns;
        private Collection<String> trackIds;
        private boolean showCancelledTrips;
        private boolean includeStopsForTrip;
        private boolean signMode;
        private boolean ignoreRealtimeUpdates = false;
        private boolean includeTripPatterns = false;

        public Builder(Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups){
            this.startTime = startTime;
            this.stop = stop;
            this.timeRange = timeRange;
            this.numberOfDepartures = numberOfDepartures;
            this.omitNonPickups = omitNonPickups;
        }


        public Builder numberOfDepartures(int numberOfDepartures) {
            this.numberOfDepartures = numberOfDepartures;
            return this;
        }

        public Builder routeMatcher(RouteMatcher routeMatcher) {
            this.routeMatcher = routeMatcher;
            return this;
        }

        public Builder direction(Integer direction) {
            this.direction = direction;
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

        public Builder requiredStop(Stop requiredStop) {
            this.requiredStop = requiredStop;
            return this;
        }

        public Builder bannedAgencies(Set<String> bannedAgencies) {
            this.bannedAgencies = bannedAgencies;
            return this;
        }

        public Builder bannedRouteTypes(Set<Integer> bannedRouteTypes) {
            this.bannedRouteTypes = bannedRouteTypes;
            return this;
        }

        public Builder patterns(List<TripPattern> patterns) {
            this.patterns = patterns;
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

        public Builder ignoreRealtimeUpdates(boolean ignoreRealtimeUpdates){
            this.ignoreRealtimeUpdates = ignoreRealtimeUpdates;
            return this;
        }

        public Builder includeTripPatterns(boolean includeTripPatterns){
            this.includeTripPatterns = includeTripPatterns;
            return this;
        }

        public StopTimesForPatternsQuery build(){
            StopTimesForPatternsQuery query = new StopTimesForPatternsQuery(this);
            return query;
        }
    }
}
