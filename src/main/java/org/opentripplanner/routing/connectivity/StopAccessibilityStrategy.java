package org.opentripplanner.routing.connectivity;

import org.opentripplanner.model.StationElement;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.routing.core.State;

public interface StopAccessibilityStrategy {
    AccessibilityResult stopIsAccessible(State state, StationElement stop);

    public boolean transitStopEvaluateGTFSAccessibilityFlag(Stop s);
}
