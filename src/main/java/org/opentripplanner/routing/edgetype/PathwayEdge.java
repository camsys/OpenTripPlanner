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

import org.onebusaway.gtfs.model.Pathway;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge {

    public enum Mode { NONE, WALKWAY, STAIRS, ELEVATOR, ESCALATOR }

    private int traversalTime;

    private int wheelchairTraversalTime = -1;

    private Mode pathwayMode = Mode.NONE;

    private String pathwayCode;

    private double distance;

    private boolean verbose = false;
    private static final Logger LOG = LoggerFactory.getLogger(PathwayEdge.class);

    public PathwayEdge(Vertex fromv, Vertex tov, int pathwayMode, String pathwayCode, int traversalTime, int wheelchairTraversalTime) {
        super(fromv, tov);
        this.traversalTime = traversalTime;
        this.wheelchairTraversalTime = wheelchairTraversalTime;
        switch (pathwayMode) {
            case Pathway.MODE_LINK:
                this.pathwayMode = Mode.NONE;
                break;
            case Pathway.MODE_WALKWAY:
                this.pathwayMode = Mode.WALKWAY;
                break;
            case Pathway.MODE_ELEVATOR:
                this.pathwayMode = Mode.ELEVATOR;
                break;
            case Pathway.MODE_STAIRS:
                this.pathwayMode = Mode.STAIRS;
                break;
        }
        this.pathwayCode = pathwayCode;
        this.distance = SphericalDistanceLibrary.distance(fromv.getCoordinate(), tov.getCoordinate());
    }

    public PathwayEdge(Vertex fromv, Vertex tov, int pathwayMode, String pathwayCode, int traversalTime) {
        this(fromv, tov, pathwayMode, pathwayCode, traversalTime, -1);
    }

    private static final long serialVersionUID = -3311099256178798981L;

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return distance;
    }
    
    public TraverseMode getMode() {
       return TraverseMode.WALK;
    }

    public Mode getPathwayMode() { return this.pathwayMode; }

    @Override
    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { getFromVertex().getCoordinate(),
                getToVertex().getCoordinate() };
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    @Override
    public boolean isApproximateGeometry() {
        return true;
    }

    public String getName() {
        switch(pathwayMode) {
            case ELEVATOR:
                return "elevator";
            case STAIRS:
                return "stairs";
            case WALKWAY:
                return "walkway";
            default:
                return "pathway";
        }
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    public String getPathwayCode() {
        return pathwayCode;
    }

    public boolean isElevator() {
        return Mode.ELEVATOR.equals(pathwayMode);
    }

    public boolean hasDefinedMode() {
        return !pathwayMode.equals(Mode.NONE);
    }

    @Override
    public boolean isWheelchairAccessible() {
        //TODO determine what else could indicate wheelchair accessible
        if (wheelchairTraversalTime >= 0) {
            return wheelchairTraversalTime >= 0;
        }else if (!Mode.STAIRS.equals(pathwayMode) ) {
            return true;
        }

        return false;
    }

    public State traverse(State s0) {
        verbose = false;
        int time = traversalTime;
        if (s0.getOptions().wheelchairAccessible) {
            if (!isWheelchairAccessible() ||
                    (!s0.getOptions().ignoreRealtimeUpdates && pathwayMode.equals(Mode.ELEVATOR) && elevatorIsOutOfService(s0))) {
                if (verbose) {
                    System.out.println("   wheelchairAccessible == true AND elevatorIsOutOfService == true");
                    LOG.info("   debug disallow, wheelchairAccessible == true AND elevatorIsOutOfService == true");
                }

                return null;
            }

            if (wheelchairTraversalTime == -1) {
                time = 1;
            }else {
                time = wheelchairTraversalTime;
            }
        }
        StateEditor s1 = s0.edit(this);
        // Allow transfers to the street if the PathwayEdge is proceeded by a TransferEdge
        if (s0.backEdge instanceof TransferEdge) {
            s1.setTransferPermissible();
        }
        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(time);
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    private boolean elevatorIsOutOfService(State s0) {
        List<Alert> alerts = getElevatorIsOutOfServiceAlerts(s0.getOptions().rctx.graph, s0);
        return !alerts.isEmpty();
    }

    public List<Alert> getElevatorIsOutOfServiceAlerts(Graph graph, State s0) {
        List<Alert> alerts = new ArrayList<>();
        for (AlertPatch alert : graph.getAlertPatches(this)) {
            if (alert.displayDuring(s0) && alert.getElevatorId() != null && pathwayCode.equals(alert.getElevatorId())) {
                alerts.add(alert.getAlert());
            }
        }
        return alerts;
    }
}
