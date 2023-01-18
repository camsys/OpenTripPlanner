package org.opentripplanner.routing.connectivity;

import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;

/**
 * Implement strategy that a stop is accessible only if it has a wheelchair entrance.
 */
public class DefaultStopAccessibilityStrategy implements StopAccessibilityStrategy {
    @Override
    public AccessibilityResult stopIsAccessible(State state, StationElement stop) {
        if (stop.getWheelchairBoarding() == WheelChairBoarding.POSSIBLE) {
            return AccessibilityResult.ALWAYS_ACCESSIBLE;
        }
        return AccessibilityResult.NEVER_ACCESSIBLE;
    }

    public DefaultStopAccessibilityStrategy(Graph graph) {

    }

    @Override
    public boolean transitStopEvaluateGTFSAccessibilityFlag(Stop s) {
        return s.getWheelchairBoarding() != WheelChairBoarding.NOT_POSSIBLE;
    }
}
