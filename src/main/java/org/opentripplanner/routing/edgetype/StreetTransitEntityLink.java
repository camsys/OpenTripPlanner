package org.opentripplanner.routing.edgetype;

import java.util.Locale;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.connectivity.AccessibilityResult;
import org.opentripplanner.routing.connectivity.StopAccessibilityStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

/**
 * This represents the connection between a street vertex and a transit vertex.
 */
public abstract class StreetTransitEntityLink<T extends Vertex> extends Edge implements CarPickupableEdge {

    private static final long serialVersionUID = -3311099256178798981L;
    static final int STEL_TRAVERSE_COST = 1;

    private final T transitEntityVertex;

    private final boolean wheelchairAccessible;

    public StreetTransitEntityLink(StreetVertex fromv, T tov, boolean wheelchairAccessible) {
        super(fromv, tov);
        this.transitEntityVertex = tov;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public StreetTransitEntityLink(T fromv, StreetVertex tov, boolean wheelchairAccessible) {
        super(fromv, tov);
        this.transitEntityVertex = fromv;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    protected abstract int getStreetToStopTime();

    protected T getTransitEntityVertex() {
        return transitEntityVertex;
    }

    public String getDirection() {
        return null;
    }

    public double getDistanceMeters() {
        return 0;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[]{fromv.getCoordinate(), tov.getCoordinate()};
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    public String getName() {
        return this.transitEntityVertex.getName();
    }

    public String getName(Locale locale) {
        //TODO: localize
        return getName();
    }


    public State traverse(State s0) {

        // Forbid taking shortcuts composed of two street-transit links associated with the same stop in a row. Also
        // avoids spurious leg transitions. As noted in https://github.com/opentripplanner/OpenTripPlanner/issues/2815,
        // it is possible that two stops can have the same GPS coordinate thus creating a possibility for a
        // legitimate StreetTransitLink > StreetTransitLink sequence, so only forbid two StreetTransitLinks to be taken
        // if they are for the same stop.
        if (
                s0.backEdge instanceof StreetTransitEntityLink &&
                        ((StreetTransitEntityLink<?>) s0.backEdge).transitEntityVertex
                                == this.transitEntityVertex
        ) {
            return null;
        }

        RoutingRequest req = s0.getOptions();
        if (s0.getOptions().wheelchairAccessible) {
            StopAccessibilityStrategy strategy = s0.getOptions().getRoutingContext().graph.stopAccessibilityStrategy;
            AccessibilityResult accessibilityResult = strategy.stopIsAccessible(s0, transitEntityVertex.getStationElement());
            if (!accessibilityResult.isAccessible()) {
                return null;
            }
        }

        StateEditor s1 = s0.edit(this);

        // Require that if we enter the transit network, we use transit before leaving.
        // This forbids shortcuts through the transit network, in the context of pathways -
        // conceptually it's similar to (s0.backEdge instanceof StreetTransitLink) but with
        // intervening pathways.
        boolean leavingTransit = isLeavingTransitNetwork(req);

        boolean firstLink = s0.getPreTransitNumBoardings() == 0
                && s0.getOptions().rctx.fromVertices.stream().anyMatch(l -> l instanceof TransitStopVertex);
        if (s0.getPreTransitNumBoardings() >= 0 && leavingTransit && !firstLink) {
            if (s0.getNumBoardings() == s0.getPreTransitNumBoardings()) {
                return null;
            }
        } else if (!leavingTransit) {
            s1.setPreTransitNumBoardings();
        }
        // Don't reenter street network following a transfer which has been followed by intervening pathways
        if (leavingTransit && !s0.isTransferPermissible()) {
            return null;
        }

        switch (s0.getNonTransitMode()) {
            case BICYCLE:
                // Forbid taking your own bike in the station if bike P+R activated.
                if (s0.getOptions().bikeParkAndRide && !s0.isBikeParked()) {
                    return null;
                }
                // Forbid taking a (station) rental bike in the station. This allows taking along
                // floating bikes.
                else if (s0.isBikeRentingFromStation() && !(s0.mayKeepRentedBicycleAtDestination() && s0.getOptions().allowKeepingRentedBicycleAtDestination)) {
                    return null;
                }
                // Allow taking an owned bike in the station
                break;
            case CAR:
                // For Kiss & Ride allow dropping of the passenger before entering the station
                if (s0.getCarPickupState() != null) {
                    if (canDropOffAfterDriving(s0) && isLeavingStreetNetwork(req)) {
                        dropOffAfterDriving(s0, s1);
                    }
                    else {
                        return null;
                    }
                }
                // If Kiss & Ride (Taxi) mode is not enabled allow car traversal so that the Stop
                // may be reached by car
                break;
        case WALK:
                break;
            default:
                return null;
        }

        if (s0.isBikeRentingFromStation()
                && s0.mayKeepRentedBicycleAtDestination()
                && s0.getOptions().allowKeepingRentedBicycleAtDestination) {
            s1.incrementWeight(s0.getOptions().keepingRentedBicycleAtDestinationCost);
        }

        s1.setBackMode(null);

        // streetToStopTime may be zero so that searching from the stop coordinates instead of
        // the stop id catch transit departing at that exact search time.
        int streetToStopTime = getStreetToStopTime();
        s1.incrementTimeInSeconds(streetToStopTime);
        s1.incrementWeight(STEL_TRAVERSE_COST + streetToStopTime);
        s1.incrementNumBoardings();
        s1.setTransferNotPermissible();
        return s1.makeState();
    }

    boolean isLeavingStreetNetwork(RoutingRequest req) {
        return (req.arriveBy ? fromv : tov) == getTransitEntityVertex();
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }

    public Trip getTrip() {
        return null;
    }

    public boolean isRoundabout() {
        return false;
    }

    private boolean isLeavingTransitNetwork(RoutingRequest options) {
        if (options.arriveBy && tov == transitEntityVertex) {
            return true;
        } else if (!options.arriveBy && fromv == transitEntityVertex) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "StreetTransitLink(" + fromv + " -> " + tov + ")";
    }
}
