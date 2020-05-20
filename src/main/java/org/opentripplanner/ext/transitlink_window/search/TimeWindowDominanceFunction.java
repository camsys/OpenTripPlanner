package org.opentripplanner.ext.transitlink_window.search;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.spt.DominanceFunction;

import java.util.Objects;

public class TimeWindowDominanceFunction extends DominanceFunction {

    @Override
    protected boolean betterOrEqual(State a, State b) { return a.weight <= b.weight; }

    @Override
    public boolean betterOrEqualAndComparable(State a, State b) {

//      First check  if the lists of trips which have been boarded are identical, or if transit has not been used in either state
        if(a.getBackState().equals(b.getBackState())) {
            return super.betterOrEqualAndComparable(a, b);
        }

        return false;
    }
}

