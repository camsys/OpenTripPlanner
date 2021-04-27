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

import java.util.Locale;

import com.vividsolutions.jts.geom.Geometry;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * A transit vehicle's journey between departure at one stop and arrival at the next.
 * This version represents a set of such journeys specified by a TripPattern.
 */
public class PatternHop extends TablePatternEdge implements OnboardEdge, HopEdge {

    private static final long serialVersionUID = 1L;

    private Stop begin, end;

    private RequestStops requestPickup = RequestStops.NO;

    private RequestStops requestDropoff = RequestStops.NO;

    private double serviceAreaRadius = 0d;

    private Geometry serviceArea = null;

    public int stopIndex;

    private LineString geometry = null;

    public PatternHop(PatternStopVertex from, PatternStopVertex to, Stop begin, Stop end, int stopIndex, boolean setInPattern) {
        super(from, to);
        this.begin = begin;
        this.end = end;
        this.stopIndex = stopIndex;
        getPattern().setPatternHop(stopIndex, this);
    }

    public PatternHop(PatternStopVertex from, PatternStopVertex to, Stop begin, Stop end, int stopIndex) {
        this(from, to, begin, end, stopIndex, true);
    }

    public double getDistance() {
        return SphericalDistanceLibrary.distance(begin.getLat(), begin.getLon(), end.getLat(),
                end.getLon());
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(getPattern().route);
    }
    
    public String getName() {
        return GtfsLibrary.getRouteName(getPattern().route);
    }
    
    @Override
    public String getName(Locale locale) {
        return this.getName();
    }

    public State optimisticTraverse(State state0) {
        RoutingRequest options = state0.getOptions();
        
        // Ignore this edge if either of its stop is banned hard
        if (!options.bannedStopsHard.isEmpty()) {
            if (options.bannedStopsHard.matches(((PatternStopVertex) fromv).getStop())
                    || options.bannedStopsHard.matches(((PatternStopVertex) tov).getStop())) {
                return null;
            }
        }
        
    	int runningTime = getPattern().scheduledTimetable.getBestRunningTime(stopIndex);
    	StateEditor s1 = state0.edit(this);
    	s1.incrementTimeInSeconds(runningTime);
    	s1.setBackMode(getMode());
    	s1.incrementWeight(runningTime);
    	return s1.makeState();
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return getPattern().scheduledTimetable.getBestRunningTime(stopIndex);
    }
    
    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }

    @Override
    public State traverse(State s0) {
        return traverse(s0, s0.edit(this));
    }

    public State traverse(State s0, StateEditor s1) {
        RoutingRequest options = s0.getOptions();
        
        // Ignore this edge if either of its stop is banned hard
        if (!options.bannedStopsHard.isEmpty()) {
            if (options.bannedStopsHard.matches(((PatternStopVertex) fromv).getStop())
                    || options.bannedStopsHard.matches(((PatternStopVertex) tov).getStop())) {
                return null;
            }
        }

        TripTimes tripTimes = s0.getTripTimes();
        int runningTime = tripTimes.getRunningTime(stopIndex);

        s1.incrementTimeInSeconds(runningTime);

        if (s0.getOptions().arriveBy)
            s1.setZone(getBeginStop().getZoneId());
        else
            s1.setZone(getEndStop().getZoneId());
        //s1.setRoute(pattern.getExemplar().route.getId());
        s1.incrementWeight(runningTime);
        s1.setBackMode(getMode());
        return s1.makeState();
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }

    public LineString getGeometry() {
        if (geometry == null) {

            Coordinate c1 = new Coordinate(begin.getLon(), begin.getLat());
            Coordinate c2 = new Coordinate(end.getLon(), end.getLat());

            geometry = GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] { c1, c2 });
        }
        return geometry;
    }

    @Override
    public Stop getEndStop() {
        return end;
    }

    @Override
    public Stop getBeginStop() {
        return begin;
    }

    public String toString() {
    	return "PatternHop(" + getFromVertex() + ", " + getToVertex() + ")";
    }


    @Override
    public int getStopIndex() {
        return stopIndex;
    }

    /**
     * Return the permissions associated with unscheduled pickups in between the endpoints of this
     * PatternHop. This relates to flag-stops in the GTFS-Flex specification; if flex and/or flag
     * stops are not enabled, this will always be RequestStops.NO.
     */
    public RequestStops getRequestPickup() {
        return requestPickup;
    }

    /**
     * Return the permissions associated with unscheduled dropoffs in between the endpoints of this
     * PatternHop. This relates to flag-stops in the GTFS-Flex specification; if flex and/or flag
     * stops are not enabled, this will always be RequestStops.NO.
     */
    public RequestStops getRequestDropoff() {
        return requestDropoff;
    }

    /**
     * Return whether flag stops are enabled in this hop. Flag stops are enabled if either pickups
     * or dropoffs at unscheduled locations can be requested. This is a GTFS-Flex feature.
     */
    private boolean hasFlagStopService() {
        return requestPickup.allowed() || requestDropoff.allowed();
    }

    /**
     * Return true if any GTFS-Flex service is defined for this hop.
     */
    public boolean hasFlexService() {
        return hasFlagStopService() || getServiceAreaRadius() > 0 || getServiceArea() != null;
    }

    public boolean canRequestService(boolean boarding) {
        return boarding ? requestPickup.allowed() : requestDropoff.allowed();
    }

    public double getServiceAreaRadius() {
        return serviceAreaRadius;
    }

    public Geometry getServiceArea() {
        return serviceArea;
    }

    public boolean hasServiceArea() {
        return serviceArea != null;
    }

    public void setRequestPickup(RequestStops requestPickup) {
        this.requestPickup = requestPickup;
    }

    public void setRequestPickup(int code) {
        setRequestPickup(RequestStops.fromGtfs(code));
    }

    public void setRequestDropoff(RequestStops requestDropoff) {
        this.requestDropoff = requestDropoff;
    }

    public void setRequestDropoff(int code) {
        setRequestDropoff(RequestStops.fromGtfs(code));
    }

    public void setServiceAreaRadius(double serviceAreaRadius) {
        this.serviceAreaRadius = serviceAreaRadius;
    }

    public void setServiceArea(Geometry serviceArea) {
        this.serviceArea = serviceArea;
    }

    private enum RequestStops {
        NO(1), YES(0), PHONE(2), COORDINATE_WITH_DRIVER(3);

        final int gtfsCode;

        RequestStops(int gtfsCode) {
            this.gtfsCode = gtfsCode;
        }

        private static RequestStops fromGtfs(int code) {
            for (RequestStops it : values()) {
                if(it.gtfsCode == code) {
                    return it;
                }
            }
            return NO;
        }

        boolean allowed() {
            return this != NO;
        }
    }
}
