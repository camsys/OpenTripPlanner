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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge {

    public enum Mode { NONE, WALKWAY, STAIRS, MOVING_SIDEWALK, ESCALATOR, ELEVATOR, FARE_GATE, EXIT_GATE }

    private AgencyAndId id = null;
    
    private int traversalTime = -1;

    private int wheelchairTraversalTime = -1;

    private double maxSlope = Double.NaN;
    
    private int stairCount = -1;
    
    private double minWidth = Double.NaN;
    
    private Mode pathwayMode = Mode.NONE;

    private double length = Double.NaN;

    private boolean verbose = false;
    
    private int mtaIsAccessible = 0; // 0 = no accessibility info available (i.e. unknown)
    
    private LineString geometry = null;
    
    private static final Logger LOG = LoggerFactory.getLogger(PathwayEdge.class);
    
    public PathwayEdge(AgencyAndId id, Vertex fromv, Vertex tov, double length, int pathwayMode, int traversalTime, int wheelchairTraversalTime,
    		double minWidth, double maxSlope, int stairCount, int isAccessible) {
        super(fromv, tov);
        this.id = id;
        this.pathwayMode = Mode.values()[pathwayMode];
        
        if(!Double.isNaN(minWidth))
        	this.minWidth = minWidth;
        if(!Double.isNaN(maxSlope))
        	this.maxSlope = maxSlope;
        if(stairCount >= 0)
        	this.stairCount = stairCount;
        if(traversalTime >= 0)
        	this.traversalTime = traversalTime;
        if(wheelchairTraversalTime >= 0)
        	this.wheelchairTraversalTime = wheelchairTraversalTime;
        if(!Double.isNaN(length))
        	this.length = length;
        if(isAccessible >= 0)
        	this.mtaIsAccessible = isAccessible;
                
        // set some defaults
        if(this.traversalTime >= 0) {
        	if(Double.isNaN(this.length))
        		this.length = this.traversalTime * 1.4; // average walk speed: 1.4 m/s
        } else {
	        if(this.stairCount >= 0 && this.traversalTime == -1) {
	        	this.traversalTime = this.stairCount * 5; // 5s per stair
	        }
	        
	        if(this.length >= 0 && this.traversalTime == -1) {
	        	this.traversalTime = (int)(this.length * 1.4); // average walk speed: 1.4 m/s
	        }
        }

    }

    private static final long serialVersionUID = -3311099256178798981L;

    public String getDirection() {
        return null;
    }

    @Override
    public double getDistance() {
    	return length;
    }

    public void setGeometry(Coordinate start, Coordinate end) {
    	Coordinate[] coordinates = new Coordinate[] { start, end };
    	this.geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }
    
    @Override
    public LineString getGeometry() {
    	return this.geometry;
    }
    
    public AgencyAndId getPathwayId() {
    	return this.id;
    }
    
    public TraverseMode getMode() {
       return TraverseMode.WALK;
    }

    public Mode getPathwayMode() { 
    	return this.pathwayMode; 
    }

    @Override
    public String toString() {
        return "Pathway(from " + getFromVertex().toString() + " to " + getToVertex().toString() + ", id=" + 
        		this.getPathwayId() + ", accessible=" + isWheelchairAccessible() + " type=" + getName() + ")";
    }
    
    public String getName() {
    	String name = "";
    	
        switch(pathwayMode) {
        	case MOVING_SIDEWALK:
        	case ESCALATOR:
        		name += "escalator";
        		break;
            case ELEVATOR:
            	name += "elevator";
        		break;
            case STAIRS:
            	name += "stairs";
        		break;
            case WALKWAY:
            	name += "walkway";
        		break;
            case FARE_GATE:
            	name += "fare gate";
        		break;
            case EXIT_GATE:
            	name += "exit gate";
        		break;
            default:
            	name += "pathway";
        		break;
        }
        
        if(verbose) 
        	name += " (" + this.getPathwayId() + ")";

        return name;
    }

    @Override
    public String getName(Locale locale) {
        return this.getName();
    }
    
    public boolean isElevator() {
        return Mode.ELEVATOR.equals(pathwayMode);
    }

    public boolean hasDefinedMode() {
        return !pathwayMode.equals(Mode.NONE);
    }

    // currently there are two ways a TA can assert a pathway is accessible--setting isAccessible as so:
    //
    // 0 or empty - Station entrance will inherit its wheelchair_boarding behavior from the parent station, if specified for the parent.
    // 1 - Station entrance is wheelchair accessible.
    // 2 - No accessible path from station entrance to stops/platforms.
    //
    // or, setting wheelchairTraversalTime. 
    //
    @Override
    public boolean isWheelchairAccessible() {
    	if(this.mtaIsAccessible == 1) // MTA asserts path is accessible
    		return true;
    	else if(this.wheelchairTraversalTime >= 0) // MTA asserts path is accessible
    		return true;
    	else { // go by heuristic:
    		if(this.pathwayMode == Mode.STAIRS || this.pathwayMode == Mode.ESCALATOR || this.pathwayMode == Mode.MOVING_SIDEWALK 
    				|| this.pathwayMode == Mode.MOVING_SIDEWALK)
    			return false;

    		// from https://developers.google.com/transit/gtfs/reference#pathwaystxt
    		if((!Double.isNaN(maxSlope) && maxSlope > .83) || 
    				(!Double.isNaN(minWidth) && minWidth < 1)) 
    			return false;

    		return true;
    	}
    }

    public State traverse(State s0) {
        verbose = false;

        // once we've paid our fare, don't re-enter the transit system after reaching an exit
        if(s0.isEverBoarded()) {
        	if(s0.getVertex() instanceof TransitStop && ((TransitStop)s0.getVertex()).isEntrance()) {
        		if(s0.getBackEdge() != null) {
        			if(s0.getBackEdge().getToVertex() instanceof TransitStop 
        					&& !((TransitStop)s0.getBackEdge().getToVertex()).isExtendedLocationType()) {		
        				return null;
        			}        
        		}
        	}        		
        }
        	
        int time = this.traversalTime;
        if (s0.getOptions().wheelchairAccessible) {
            time = this.wheelchairTraversalTime;

        	if (!isWheelchairAccessible() ||
                    (!s0.getOptions().ignoreRealtimeUpdates && pathwayMode.equals(Mode.ELEVATOR) && elevatorIsOutOfService(s0))) {
                if (verbose) {
                    System.out.println("   wheelchairAccessible == true AND elevatorIsOutOfService == true");
                    LOG.info("   debug disallow, wheelchairAccessible == true AND elevatorIsOutOfService == true");
                }

                return null;
            }
        }
        
        StateEditor s1 = s0.edit(this);
        // Allow transfers to the street if the PathwayEdge is proceeded by a TransferEdge
        if (s0.backEdge instanceof TransferEdge) {
            s1.setTransferPermissible();
        }

        if(time == -1) {
        	LOG.debug("Traversal time on pathway {} is negative; bumping to 0 to avoid routing problems. "
        			+ "Check your pathways data: one of traversal time, length or stair count should be set.", this.getPathwayId());
        	time = 0;
        }
        
        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(time);
        
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    // for GTFS-RT/pathway linking 
    // assumes MTA format 120S-S2P_EL145_S-IN (STOP ID-TYPE OF PATHWAY_ELEVATOR ID_PLATFORM DIRECTION-TRAVERSE_DIRECTION
    public String getElevatorId() {
    	if(this.getPathwayMode() != Mode.ELEVATOR)
    		return null;
    	
    	String pathwayIdNoAgency = this.getPathwayId().getId();
    	String[] elevatorIdParts = pathwayIdNoAgency.split("_");

    	if(elevatorIdParts.length > 2)
    		return elevatorIdParts[1];
    	else
    		return null;
    }
    
    private boolean elevatorIsOutOfService(State s0) {
        List<Alert> alerts = getElevatorIsOutOfServiceAlerts(s0.getOptions().rctx.graph, s0);
        return !alerts.isEmpty();
    }

    public List<Alert> getElevatorIsOutOfServiceAlerts(Graph graph, State s0) {
        List<Alert> alerts = new ArrayList<>();
        for (AlertPatch alert : graph.getAlertPatches(this)) {
            if (alert.displayDuring(s0) && alert.getElevatorId() != null && pathwayMode == Mode.ELEVATOR) {
                alerts.add(alert.getAlert());
            }
        }
        return alerts;
    }
}
