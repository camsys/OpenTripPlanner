package org.opentripplanner.index.model;

import com.google.common.collect.Lists;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.List;

/**
 * Some stopTimes all in the same pattern.
 * TripTimeShort should probably be renamed StopTimeShort
 */
public class StopTimesInPattern {

    public PatternShort pattern;
    public RouteShort route;
    public List<TripTimeShort> times = Lists.newArrayList();

    public StopTimesInPattern(TripPattern pattern) {
        this.pattern = new PatternShort(pattern);
        this.route = new RouteShort(pattern.route);
    }

}
