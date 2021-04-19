package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.locationtech.jts.geom.LineString;
import java.util.Locale;

/**
 * A transfer directly between two stops without using the street network.
 *
 */
public class TransferEdge extends Edge {

    private static final long serialVersionUID = 1L;
    
    int time = 0;
    
    double distance;

    private LineString geometry = null;

    private boolean wheelchairAccessible = true;

    /**
     * @see Transfer(Vertex, Vertex, double, int)
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance) {
        this(fromv, tov, distance, (int) distance);
    }
    
    /**
     * Creates a new Transfer edge.
     * @param fromv     the Vertex where the transfer originates
     * @param tov       the Vertex where the transfer ends
     * @param distance  the distance in meters from the origin Vertex to the destination
     * @param time      the minimum time in seconds it takes to complete this transfer
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance, int time) {
        super(fromv, tov);
        this.distance = distance;
        this.time = time; 
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return distance;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    public String getName() {
        return "Transfer";
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    public State traverse(State s0) {
        /* Disallow chaining of transfer edges. TODO: This should really be guaranteed by the PathParser
           but the default Pathparser is currently very hard to read because
           we need a complement operator. */

        RoutingRequest options = s0.getOptions();
        int time = getTime(options);
        double weight = time * (options.applyWalkReluctanceInTransfers ? options.walkReluctance : 1.0);

        // Forbid taking shortcuts composed of two transfers in a row
        if (s0.backEdge instanceof TransferEdge) {
            return null;
        }
        if (s0.backEdge instanceof StreetTransitLink) {
            return null;
        }
//        if (!s0.isTransferPermissible()) {
//            return null;
//        }
        if (distance > s0.getOptions().maxTransferWalkDistance) {
            return null;
        }
        if (distance > s0.getOptions().maxWalkDistance && s0.getOptions().walkLimitingByLeg) {
            double beforeStateWalk = s0.getWalkSinceLastTransit();
            double afterStateWalk =  beforeStateWalk + getDistance();
            weight += calculateOverageWeight(beforeStateWalk, afterStateWalk,
                    options.getMaxWalkDistance(), options.softWalkPenalty,
                    options.softWalkOverageRate);
        }
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        // Only transfer right after riding a vehicle.
        StateEditor se = s0.edit(this);
        se.setBackMode(TraverseMode.WALK);
        se.incrementTimeInSeconds(time);
        se.incrementWeight(weight);
        se.incrementWalkDistance(distance);
        se.setTransferNotPermissible();
        return se.makeState();
    }

    /*
     * Before merge with SimpleTransfer, this class had its own time parameter = min_transfer_time, or distance if min_tranfer_time unset.
     * min_transfer_time is handled in the TransferTable, so now time can be calculated from distance.
     */
    private int getTime(RoutingRequest rr) {
        return (int) Math.ceil(distance / rr.walkSpeed) + 2 * StreetTransitLink.STL_TRAVERSE_COST;
    }

    public void setGeometry(LineString geometry) {
        this.geometry  = geometry;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    // borrowed from StreetEdge
    private double calculateOverageWeight(double firstValue, double secondValue, double maxValue,
                                          double softPenalty, double overageRate) {
        // apply penalty if we stepped over the limit on this traversal
        boolean applyPenalty = false;
        double overageValue;

        if(firstValue <= maxValue && secondValue > maxValue){
            applyPenalty = true;
            overageValue = secondValue - maxValue;
        } else {
            overageValue = secondValue - firstValue;
        }

        // apply overage and add penalty if necessary
        return (overageRate * overageValue) + (applyPenalty ? softPenalty : 0.0);
    }

}
