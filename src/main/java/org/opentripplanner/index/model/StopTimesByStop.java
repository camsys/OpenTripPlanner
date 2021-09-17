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

package org.opentripplanner.index.model;

import org.locationtech.jts.geom.Coordinate;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.VehicleInfo;
import org.opentripplanner.api.model.alertpatch.LocalizedAlert;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.util.PolylineEncoder;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * Some stopTimes all from the same stop.
 */
public class StopTimesByStop {

    private StopDetail stop;

    /** Upcoming arrivals and departures, grouped by route and headsign */
    private Map<String, StopTimesByRouteAndHeadsign> groupsByKey;

    /** Alerts for stop */
    public List<LocalizedAlert> alerts;

    public StopTimesByStop(Stop stop, double distance, long walkTime, Iterable<Coordinate> coordinates, boolean groupByParent) {
        this.stop = new StopDetail(stop);
        if (distance >= 0) {
            this.stop.dist = (int) Math.round(distance);
        }
        if (coordinates != null) {
            this.stop.geometry = PolylineEncoder.createEncodings(coordinates);
        }
        if (walkTime >= 0) {
            this.stop.walkTime = walkTime;
        }
        if (stop.getParentStation() != null && groupByParent) {
            this.stop.id.setId(stop.getParentStation());
            this.stop.cluster = null;
            // TODO we only know lat and lon match because it's an MTA convention
        }
        groupsByKey = new HashMap<>();
    }

    public StopTimesByStop(Stop stop,  List<StopTimesInPattern> stips) {
        this(stop, -1, -1, null, true);
        addPatterns(stips);
    }

    public StopTimesByStop(Stop stop, boolean groupByParent) {
        this(stop, -1, -1, null, groupByParent);
    }

        /**
         * Stop which these arrival-departures are supplied for. If groupByParent = true, this will be a parent station
         * (if parent stations are given in GTFS).
         */
    public StopShort getStop() {
        return stop;
    }

    /**
     * List of groups of arrival-departures, grouped by route and headsign
     */
    public Collection<StopTimesByRouteAndHeadsign> getGroups() {
        return groupsByKey.values();
    }
    
    // adds an LIRR Solari packet to the pattern list 
    public void addPatternsViaSolariPacket(JsonNode solariPacket, GraphIndex index) throws Exception {
    	String destinationLocation = solariPacket.get("destinationLocation").get("name").asText();
    	String direction = solariPacket.get("direction").asText();
    	String headsign = destinationLocation;
    	
    	Route route = index.routeForId.values().stream()
    			.filter(it -> it != null && it.getLongName() != null && it.getLongName().equals(destinationLocation))
    			.findFirst()
    			.orElse(null);

    	// from Sunny: 
    	// If westbound (direction is a field in Solari - E or W) and no match, hard code to CTZ.
    	// If eastbound and no match, look up based on eastern terminal (done above)
    	if(route == null) {
    		if(direction.equals("E") || direction.equals("W")) {
    		   	route = index.routeForId.values().stream()
	    			.filter(it -> it != null && it.getLongName() != null && it.getLongName().equals("City Zone"))
	    			.findFirst()
	    			.orElse(null);   			
    		} else
        		throw new Exception("Route " + destinationLocation + " not found.");
    	}
    	
    	RouteShort routeShort = new RouteShort(route);
    	
        String key = key(routeShort, headsign);
        StopTimesByRouteAndHeadsign group = groupsByKey.computeIfAbsent(key,
                 k -> new StopTimesByRouteAndHeadsign(routeShort, headsign, false)
        );
        group.addTime(solariPacket, index);    
    }
    
    public void addPatternWithSolariPacket(StopTimesInPattern stip, JsonNode solariPacket, GraphIndex index) {
        RouteShort route = stip.route;
        for (TripTimeShort tt : stip.times) {
            boolean isStopHeadsign = tt.stopHeadsign != null;
            String headsign = isStopHeadsign ? tt.stopHeadsign : tt.tripHeadsign;
	    	String key = key(route, headsign);
	    	StopTimesByRouteAndHeadsign group = groupsByKey.computeIfAbsent(key,
                    k -> new StopTimesByRouteAndHeadsign(route, headsign, isStopHeadsign)
            );
	        group.addTime(tt, solariPacket);
        }
    }
    
    public void addPatterns(List<StopTimesInPattern> stip) {
        for (StopTimesInPattern s : stip) {
            RouteShort route = s.route;
            for (TripTimeShort tt : s.times) {
                boolean isStopHeadsign = tt.stopHeadsign != null;
                String headsign = isStopHeadsign ? tt.stopHeadsign : tt.tripHeadsign;
                String key = key(route, headsign);
                StopTimesByRouteAndHeadsign group = groupsByKey.computeIfAbsent(key,
                        k -> new StopTimesByRouteAndHeadsign(route, headsign, isStopHeadsign)
                );
                group.addTime(tt);
            }
        }
    }

    public void addAlert(AlertPatch alertPatch, Locale locale) {
        if (alertPatch.getAlert() != null) {
            Alert alert = alertPatch.getAlert();
            if (alerts == null) {
                alerts = new ArrayList<>();
            }
            for (LocalizedAlert a : alerts) {
                if (a.alert.equals(alert)) {
                    return;
                }
            }
            alerts.add(new LocalizedAlert(alert, locale));
        }
        if (alertPatch.getVehicleInfo() != null) {
            VehicleInfo vehicleInfo = alertPatch.getVehicleInfo();
            for (StopTimesByRouteAndHeadsign group : groupsByKey.values()) {
                for (TripTimeShort tt : group.getTimes()) {
                    if (tt.tripId != null && tt.tripId.equals(alertPatch.getTrip())) {
                        tt.vehicleInfo = vehicleInfo;
                    }
                }
            }
        }
    }

    public void limitTimes(long startTime, int timeRange, int numberOfDepartures) {
        for (StopTimesByRouteAndHeadsign st : groupsByKey.values()) {
            st.limitTimes(startTime, timeRange, numberOfDepartures);
        }
    }

    private String key(RouteShort route, String headsign) {
        return route.id.toString() + "#" + headsign;
    }
}
