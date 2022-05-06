package org.opentripplanner.index.model;

import com.google.common.collect.Lists;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.List;

/**
 * Some stopTimes all in the same pattern.
 */
public class StopTimesInPatternDetail extends StopTimesInPattern{

    /**
     * Pattern which these arrival-departures are associated with.
     */
    public TripPattern tripPattern;


    public StopTimesInPatternDetail(TripPattern pattern) {
        super(pattern);
        this.tripPattern = pattern;
    }

}
