/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.locationtech.jts.geom.LineString;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transfer directly between two stops without using the street network.
 *
 */
public class TransferEdge extends Edge {

    private static final long serialVersionUID = 2L;

    private double distance;

    private LineString geometry = null;

    private boolean verbose = false;

    private boolean wheelchairAccessible = true;

    private static final Logger LOG = LoggerFactory.getLogger(TransferEdge.class);

    /**
     * Creates a new Transfer edge.
     * @param fromv     the Vertex where the transfer originates
     * @param tov       the Vertex where the transfer ends
     * @param distance  the distance in meters from the origin Vertex to the destination
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance) {
        super(fromv, tov);
        this.distance = distance;
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

    @Override
    public State traverse(State s0) {
        /* Disallow chaining of transfer edges. TODO: This should really be guaranteed by the PathParser
           but the default Pathparser is currently very hard to read because
           we need a complement operator. */
        verbose = false;
        RoutingRequest options = s0.getOptions();
        int time = getTime(options);
        double weight = time * options.walkReluctance;

        // Forbid taking shortcuts composed of two transfers in a row
        if (s0.backEdge instanceof TransferEdge) {
            if (verbose) {
                System.out.println("   backEdge isintanceof TransferEdge ");
                LOG.info("   debug disallow, backEdge instance of TransferEdge is true");
            }

            return null;
        }
        if (s0.backEdge instanceof StreetTransitLink) {
            if (verbose) {
                System.out.println("   backEdge isintanceof StreetTransitLink ");
                LOG.info("   debug disallow, backEdge instance of StreetTransitLink is true");
            }
            return null;
        }
        if (!s0.isTransferPermissible()) {
            if (verbose) {
                System.out.println("   isTransferPermissible == false ");
                LOG.info("   debug disallow, isTransferPremissible == false");
            }
            return null;
        }
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) {
            if (verbose) {
                LOG.info("   debug disallow, not wheelchairAccessible");
            }
            return null;
        }
        if (distance > s0.getOptions().maxTransferWalkDistance) {
            if (verbose) {
                System.out.println("   distance > maxTransferWalkDistance");
                LOG.info("   debug disallow, distance > maxTransferWalkDistance");
            }
            return null;
        }
        if (distance > s0.getOptions().maxWalkDistance && s0.getOptions().walkLimitingByLeg) {
            double beforeStateWalk = s0.getWalkSinceLastTransit();
            double afterStateWalk =  beforeStateWalk + getDistance();
            weight += calculateOverageWeight(beforeStateWalk, afterStateWalk,
                    options.getMaxWalkDistance(), options.softWalkPenalty,
                    options.softWalkOverageRate);
        }

        if (s0.getOptions().getRoutingContext() != null && s0.getOptions().getRoutingContext().graph.transferPermissionStrategy != null) {
            if (!s0.getOptions().getRoutingContext().graph.transferPermissionStrategy.isTransferAllowed(
                    s0, ((TransitStationStop) fromv).getStop(), ((TransitStationStop) tov).getStop(),
                    !s0.getOptions().arriveBy)) {
                if (verbose) {
                    System.out.println("   TransferAllowed == false");
                    LOG.info("   debug disallow, TransferAllowed == false");
                }
                return null;
            }
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

    @Override
    public double weightLowerBound(RoutingRequest rr) {
        return (getTime(rr) * rr.walkReluctance);
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }
    
    /*
     * Before merge with SimpleTransfer, this class had its own time parameter = min_transfer_time, or distance if min_tranfer_time unset.
     * min_transfer_time is handled in the TransferTable, so now time can be calculated from distance.
     */
    private int getTime(RoutingRequest rr) {
        return (int) Math.ceil(distance / rr.walkSpeed) + 2 * StreetTransitLink.STL_TRAVERSE_COST;
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
