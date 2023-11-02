package org.opentripplanner.routing.connectivity;

import org.opentripplanner.model.StationElement;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;

/**
 * Extends default strategy to handle MTASBWY stops by finding first path to accessible entrance:
 * early return as soon an a wheelchair entrance is found
 * build AccessibilityResult
 * only use pathways that are accessible, include alerts in result
 */
public class MTAStopAccessibilityStrategy extends DefaultStopAccessibilityStrategy {

    public MTAStopAccessibilityStrategy(Graph graph) {
        super(graph);
    }

    @Override
    public AccessibilityResult stopIsAccessible(State state, StationElement stop) {
        // all MTA buses are accessible
        if(stop.getStopId().getFeedId().equals("MTA"))
            return AccessibilityResult.ALWAYS_ACCESSIBLE;
        return stop.getWheelchairBoarding() == WheelChairBoarding.POSSIBLE ? AccessibilityResult.ALWAYS_ACCESSIBLE : AccessibilityResult.NEVER_ACCESSIBLE;
    }

    @Override
    public boolean transitStopEvaluateGTFSAccessibilityFlag(Stop s) {
        // all MTA buses are accessible even though the GTFS doesn't say this
        if(s.getId().getFeedId().equals("MTA"))
            return true;

        // otherwise, default to what the GTFS says positively
        return s.getWheelchairBoarding() == WheelChairBoarding.POSSIBLE;
    }

}
