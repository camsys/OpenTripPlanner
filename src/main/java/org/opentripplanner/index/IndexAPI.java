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

package org.opentripplanner.index;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import com.webcohesion.enunciate.metadata.Ignore;
import com.webcohesion.enunciate.metadata.rs.TypeHint;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.model.VehicleInfo;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.module.RouteStopsAccessibilityTaggerModule.RouteStopTag;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.AccessibilityShort;
import org.opentripplanner.index.model.EquipmentShort;
import org.opentripplanner.index.model.PatternDetail;
import org.opentripplanner.index.model.PatternShort;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.StopClusterDetail;
import org.opentripplanner.index.model.StopDetail;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TransferShort;
import org.opentripplanner.index.model.TripDetail;
import org.opentripplanner.index.model.TripShort;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.Landmark;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class
@Path("/routers/{routerId}/index")    // It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class IndexAPI {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(IndexAPI.class);
    private static final double MAX_STOP_SEARCH_RADIUS = 5000;
    private static final String MSG_404 = "FOUR ZERO FOUR";
    private static final String MSG_400 = "FOUR HUNDRED";

    /** Choose short or long form of results. */
    @QueryParam("detail") private boolean detail = false;

    private final GraphIndex index;
    private final StreetVertexIndexService streetIndex;
    private final ObjectMapper deserializer = new ObjectMapper();
    private final Graph graph;

    public IndexAPI (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        this.index = router.graph.index;
        this.graph = router.graph;
        this.streetIndex = router.graph.streetIndex;
    }

    public IndexAPI(Graph graph) {
        this.index = graph.index;
        this.graph = graph;
        this.streetIndex = graph.streetIndex;
    }

   /* Needed to check whether query parameter map is empty, rather than chaining " && x == null"s */
   @Context UriInfo uriInfo;

    /**
     * Return all feed IDs represented in the graph
     * @return  . .
     */
    @GET
    @Path("/feeds")
    @TypeHint(String[].class)
    public Response getFeeds() {
        return Response.status(Status.OK).entity(index.agenciesForFeedId.keySet()).build();
    }

    /**
     * Return FeedInfo for a specific feed
     *
     * @param feedId ID of feed
     * @return . .
     */
    @GET
    @Path("/feeds/{feedId}")
    @TypeHint(FeedInfo.class)
    public Response getFeedInfo(@PathParam("feedId") String feedId) {
        FeedInfo feedInfo = index.feedInfoForId.get(feedId);
        if (feedInfo == null) {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        } else {
            return Response.status(Status.OK).entity(feedInfo).build();
        }
    }

   /**
    * Return a list of all agencies in the graph for a particular feed.
    *
    * @param feedId ID of feed in question
    * @return . .
    */
   @GET
   @Path("/agencies/{feedId}")
   @TypeHint(Agency[].class)
   public Response getAgencies (@PathParam("feedId") String feedId) {
       return Response.status(Status.OK).entity(
               index.agenciesForFeedId.getOrDefault(feedId, new HashMap<>()).values()).build();
   }

   /**
    * Return specific agency in the graph, by ID.
    *
    * @param agencyId agency to return data for
    * @param feedId feed to return data for
    * @return . .
    */
   @GET
   @Path("/agencies/{feedId}/{agencyId}")
   @TypeHint(Agency.class)
   public Response getAgency (@PathParam("feedId") String feedId, @PathParam("agencyId") String agencyId) {
       for (Agency agency : index.agenciesForFeedId.get(feedId).values()) {
           if (agency.getId().equals(agencyId)) {
               return Response.status(Status.OK).entity(agency).build();
           }
       }
       return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
   }

    /**
     * Return all routes for the specific agency.
     *
     * @param agencyId agency to return data for
     * @param feedId feed to return data for
     * @return . .
     */
    @GET
    @Path("/agencies/{feedId}/{agencyId}/routes")
    @TypeHint(RouteShort[].class)
    public Response getAgencyRoutes (@PathParam("feedId") String feedId, @PathParam("agencyId") String agencyId) {
        Collection<Route> routes = index.routeForId.values();
        Agency agency = index.agenciesForFeedId.get(feedId).get(agencyId);
        if (agency == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        Collection<Route> agencyRoutes = new ArrayList<>();
        for (Route route: routes) {
            if (route.getAgency() == agency) {
                agencyRoutes.add(route);
            }
        }
        routes = agencyRoutes;
        if (detail){
            return Response.status(Status.OK).entity(routes).build();
        }
        else {
            return Response.status(Status.OK).entity(RouteShort.list(routes)).build();
        }
    }
   
   /**
    * Return specific transit stop in the graph, by ID.
    *
    * @param stopIdString stop in Agency:Stop ID format
    * @return . .
    */
   @GET
   @Path("/stops/{stopId}")
   @TypeHint(Stop.class)
   public Response getStop (@PathParam("stopId") String stopIdString) {
       AgencyAndId stopId = GtfsLibrary.convertIdFromString(stopIdString);
       Stop stop = index.stopForId.get(stopId);
       if (stop != null) {
           return Response.status(Status.OK).entity(stop).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }


   /**
    * Return a list of all stops in the given area. Supply lat/lon/radius to return all stops
    * in a circle around the given coordinate, or supply minLat/minLon/maxLat/maxLon to return
    * all stops in the given bounding box.
    *
    * @param minLat coordinate of bounding box
    * @param minLon coordinate of bounding box
    * @param maxLat coordinate of bounding box
    * @param maxLon coordinate of bounding box
    * @param lat coordinate of circle
    * @param lon coordinate of circle
    * @param radius radius of circle
    * @param debug include extra "debug" info on stop linking data
    * @param mode mode only return clusters with this mode. Valid values are TRAM, SUBWAY, RAIL, BUS, FERRY, CABLE_CAR, GONDOLA, FUNICULAR. (optional)
    * @param agencyId  only return clusters for this agency (optional)
    * @return . .
    */
   @GET
   @Path("/stops")
   @TypeHint(StopDetail[].class)
   public Response getStopsInRadius (
           @QueryParam("minLat") Double minLat,
           @QueryParam("minLon") Double minLon,
           @QueryParam("maxLat") Double maxLat,
           @QueryParam("maxLon") Double maxLon,
           @QueryParam("lat")    Double lat,
           @QueryParam("lon")    Double lon,
           @QueryParam("radius") Double radius,
           @QueryParam("debug") Boolean debug,
           @QueryParam("mode") String mode,
           @QueryParam("agencyId") String agencyId) {

       List<StopDetail> stops;
       TraverseMode traverseMode = (mode == null) ? null : TraverseMode.valueOf(mode);

       /* If any of the circle parameters are specified, expect a circle not a box. */
       boolean expectCircle = (lat != null || lon != null || radius != null);

       /* When no parameters are supplied, return all stops. */
       if (minLat == null && minLon == null && maxLat == null && maxLon == null && lat == null
               && lon == null && radius == null) {
           index.clusterStopsAsNeeded();
           stops = Lists.newArrayList();
           for (Stop stop : index.stopForId.values()) {
               if (agencyId != null && !(stop.getId().getAgencyId().equals(agencyId)) || stop == null) continue;
               if (traverseMode != null &&
                       !(index.routesForStop(stop).stream().anyMatch(route ->
                               GtfsLibrary.getTraverseMode(route) == traverseMode))) continue;

               StopCluster sc = index.stopClusterForStop.get(stop);
               if(sc != null)
            	   stops.add(new StopDetail(stop, sc.id));
           }
        }
       else if (expectCircle) {
           if (lat == null || lon == null || radius == null || radius < 0) {
               return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
           }
           if (radius > MAX_STOP_SEARCH_RADIUS){
               radius = MAX_STOP_SEARCH_RADIUS;
           }
           index.clusterStopsAsNeeded();
           stops = Lists.newArrayList();
           Coordinate coord = new Coordinate(lon, lat);
           for (TransitStop stopVertex : streetIndex.getNearbyTransitStops(
                    new Coordinate(lon, lat), radius)) {
               if (traverseMode != null && !stopVertex.getModes().contains(traverseMode)) continue;

               double distance = SphericalDistanceLibrary.fastDistance(stopVertex.getCoordinate(), coord);
               if (distance < radius) {
                   Stop stop = stopVertex.getStop();
                   if (agencyId != null && !(stop.getId().getAgencyId().equals(agencyId))) continue;

                   stops.add(new StopDetail(stop, (int) distance, index.stopClusterForStop.get(stop).id));
               }
           }
       } else {
           /* We're not circle mode, we must be in box mode. */
           if (minLat == null || minLon == null || maxLat == null || maxLon == null) {
               return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
           }
           if (maxLat <= minLat || maxLon <= minLon) {
               return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
           }
           index.clusterStopsAsNeeded();
           stops = Lists.newArrayList();
           Envelope envelope = new Envelope(new Coordinate(minLon, minLat), new Coordinate(maxLon, maxLat));
           for (TransitStop stopVertex : streetIndex.getTransitStopForEnvelope(envelope)) {
               if (traverseMode != null && !stopVertex.getModes().contains(traverseMode)) continue;

               Stop stop = stopVertex.getStop();
               if (agencyId != null && !(stop.getId().getAgencyId().equals(agencyId))) continue;

               stops.add(new StopDetail(stop, index.stopClusterForStop.get(stop).id));
           }
       }
       if (debug != null && debug) {
           for (StopDetail stop : stops) {
               TransitStop tstop = index.stopVertexForStop.get(index.stopForId.get(stop.id));
               if (tstop.shouldLinkToStreet()) {
                   stop.distance = tstop.getDistance();
                   stop.wayId = tstop.getOsmWay();
               }
           }
       }

       return Response.status(Status.OK).entity(stops).build();
   }

   /**
    * Return all routes incident on a stop.
    *
    * @param stopId stop in Agency:Stop ID format
    * @return . .
    */
   @GET
   @Path("/stops/{stopId}/routes")
   @TypeHint(RouteShort[].class)
   public Response getRoutesForStop (@PathParam("stopId") String stopId) {
       Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopId));
       if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       Set<Route> routes = Sets.newHashSet();
       for (TripPattern pattern : index.patternsForStop.get(stop)) {
           routes.add(pattern.route);
       }
       return Response.status(Status.OK).entity(RouteShort.list(routes)).build();
   }

   /**
    * Return all patterns which serve a particular stop.
    *
    * @param stopIdString stop in Agency:Stop ID format
    * @return . .
    */
   @GET
   @Path("/stops/{stopId}/patterns")
   @TypeHint(PatternShort[].class)
   public Response getPatternsForStop (@PathParam("stopId") String stopIdString) {
       AgencyAndId id = GtfsLibrary.convertIdFromString(stopIdString);
       Stop stop = index.stopForId.get(id);
       if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       Collection<TripPattern> patterns = index.patternsForStop.get(stop);
       return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
   }

    /**
     * Return upcoming vehicle arrival/departure times at the given stop.
     *
     * @param stopIdString Stop ID in Agency:Stop ID format
     * @param startTime Start time for the search. Seconds from UNIX epoch
     * @param timeRange Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures Number of departures to fetch per pattern
     * @param omitNonPickups Omit arrival/departures at which the vehicle does not pick up
     *                       passengers.
     * @return . .
     */
    @GET
    @Path("/stops/{stopId}/stoptimes")
    @TypeHint(StopTimesInPattern[].class)
    public Response getStoptimesForStop (@PathParam("stopId") String stopIdString,
                                         @QueryParam("startTime") long startTime,
                                         @QueryParam("timeRange") @DefaultValue("86400") int timeRange,
                                         @QueryParam("numberOfDepartures") @DefaultValue("2") int numberOfDepartures,
                                         @QueryParam("omitNonPickups") boolean omitNonPickups) {
        Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopIdString));
        if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        return Response.status(Status.OK).entity(index.stopTimesForStop(stop, startTime, timeRange, numberOfDepartures, omitNonPickups )).build();
    }

    /**
     * Return upcoming vehicle arrival/departure times at the given stop.
     *
     * @param date in YYYYMMDD format
     * @param stopIdString Stop ID in Agency:Stop ID format
     * @param omitNonPickups Omit arrival/departures at which the vehicle does not pick up
     *                       passengers.
     * @return . .
     */
    @GET
    @Path("/stops/{stopId}/stoptimes/{date}")
    @TypeHint(StopTimesInPattern[].class)
    public Response getStoptimesForStopAndDate (@PathParam("stopId") String stopIdString,
                                                @PathParam("date") String date,
                                                @QueryParam("omitNonPickups") boolean omitNonPickups) {
        Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopIdString));
        if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        ServiceDate sd;
        try {
            sd = ServiceDate.parseString(date);
        }
        catch (ParseException e){
            return Response.status(Status.BAD_REQUEST).entity(MSG_400).build();
        }

        List<StopTimesInPattern> ret = index.getStopTimesForStop(stop, sd, omitNonPickups, false);
        return Response.status(Status.OK).entity(ret).build();
    }
    
    /**
     * @param stopIdString stop in Agency:Stop ID format.
     */
    @GET
    @Path("/stops/{stopId}/equipment")
    public Response getEquipment(@PathParam("stopId") String stopIdString) {
    	Set<PathwayEdge> equipmentHere = index.equipmentEdgesForStationId.get(stopIdString);

    	if (equipmentHere != null) {
    		Set<EquipmentShort> result = new HashSet<EquipmentShort>();
    		
    	    for(PathwayEdge equipmentEdge : equipmentHere) {
    	    	String equipmentId = equipmentEdge.getElevatorId();

    	    	EquipmentShort resultItem = new EquipmentShort();
    	    	resultItem.isCurrentlyAccessible = true;
    	    	resultItem.equipmentId = equipmentId;
    	    	
    	    	Set<Alert> alerts = new HashSet<Alert>();
        	   	for (AlertPatch alert : graph.getAlertPatches(equipmentEdge)) {
        	   		if(alert.getStop().equals(GtfsLibrary.convertIdFromString(stopIdString)) 
        	   				&& alert.getElevatorId().equals(equipmentId)) {
        	   			alerts.add(alert.getAlert());

        	   			if(alert.isRoutingConsequence())
        	    	    	resultItem.isCurrentlyAccessible = false;
        	   		}
        	   	}
        	   	
        	    resultItem.alerts = alerts;    	 
    	    	
    	    	result.add(resultItem);
    	    } 
        	
            return Response.status(Status.OK).entity(result).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }
    
    @GET
    @Path("/agency/{agencyId}/equipment")
    public Response getEquipmentForAgency(@PathParam("agencyId") String agencyIdString) {
		Set<EquipmentShort> result = new HashSet<EquipmentShort>();

    	for(Vertex v : graph.getVertices()) {
    		if(!(v instanceof TransitStationStop))
    			continue;
    		
    		TransitStationStop tss = (TransitStationStop)v;
    		
    		if(!tss.getStopId().getAgencyId().equals(agencyIdString))
    			continue;

        	Set<PathwayEdge> equipmentHere = 
        			index.equipmentEdgesForStationId.get(GtfsLibrary.convertIdToString(tss.getStopId()));

        	if(equipmentHere == null || equipmentHere.isEmpty())
        		continue;
        	
    	    for(PathwayEdge equipmentEdge : equipmentHere) {
    	    	String equipmentId = equipmentEdge.getElevatorId();

    	    	EquipmentShort resultItem = new EquipmentShort();
    	    	resultItem.isCurrentlyAccessible = true;
    	    	resultItem.equipmentId = equipmentId;
    	    	
    	    	Set<Alert> alerts = new HashSet<Alert>();
        	   	for (AlertPatch alert : graph.getAlertPatches(equipmentEdge)) {
        	   		if(alert.getStop().equals(tss.getStopId())
        	   				&& alert.getElevatorId().equals(equipmentId)) {
        	   			alerts.add(alert.getAlert());

        	   			if(alert.isRoutingConsequence())
        	    	    	resultItem.isCurrentlyAccessible = false;
        	   		}
        	   	}
        	   	
        	    resultItem.alerts = alerts;    	 
    	    	
    	    	result.add(resultItem);
    	    } 
    	}        	
         
    	return Response.status(Status.OK).entity(result).build();
    }
    
    /**
     * @param stopIdString stop in Agency:Stop ID format.
     */
    @GET
    @Path("/stops/{stopId}/accessibility")
    public Response getAccessibility(@PathParam("stopId") String stopIdString) {

        Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopIdString));
        ArrayList<RouteStopTag> tags = graph.routeStopTagsByStopId.get(stopIdString);
        if(stop != null && tags == null) 
        	tags = graph.routeStopTagsByStopId.get(stop.getId().getAgencyId() + GtfsLibrary.ID_SEPARATOR + stop.getParentStation());
        
        if (stop != null && tags != null && !tags.isEmpty()) {
            return Response.status(Status.OK).entity(new AccessibilityShort(stop, tags, graph)).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
        
    }
    
    
    /**
     * Return the generated transfers a stop in the graph, by stop ID
     *
     * @param stopIdString stop in Agency:Stop ID format.
     * @return . .
     */
    @GET
    @Path("/stops/{stopId}/transfers")
    @TypeHint(TransferShort[].class)
    public Response getTransfers(@PathParam("stopId") String stopIdString) {
        Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopIdString));
        
        if (stop != null) {
            Collection<TransferShort> out = Sets.newHashSet();

            // get the transfers for the stop
            TransitStop v = index.stopVertexForStop.get(stop);
            for (Edge edge : v.getOutgoing()) {
                if (edge instanceof TransferEdge) {
                    out.add(new TransferShort((TransferEdge) edge));
                }
            }

            return Response.status(Status.OK).entity(out).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

   /**
    *  Return a list of all routes in the graph.
    *
    * @param mode only return routes of these modes. Comma-separated list. Valid values are TRAM, SUBWAY, RAIL, BUS, FERRY, CABLE_CAR, GONDOLA, FUNICULAR. (optional)
    * @param stopIds only return routes which include the stops (optional)
    * @return . .
    */
   @GET
   @Path("/routes")
   @TypeHint(RouteShort[].class)
   public Response getRoutes (@QueryParam("mode") String mode, @QueryParam("hasStop") List<String> stopIds) {

       Collection<Route> routes = index.routeForId.values();

       // Protective copy, we are going to calculate the intersection destructively when we filter Modes and Stops
       routes = Lists.newArrayList(routes);

       // Filter routes to include only those that pass through all given stops
       if (stopIds != null) {
           for (String stopId : stopIds) {
               Stop stop = index.stopForId.get(GtfsLibrary.convertIdFromString(stopId));
               if (stop == null) return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
               Set<Route> routesHere = Sets.newHashSet();
               for (TripPattern pattern : index.patternsForStop.get(stop)) {
                   routesHere.add(pattern.route);
               }
               routes.retainAll(routesHere);
           }
       }

       // Filter routes that don't include the modes that we care about
       if (mode != null) {
           List<String> modeList = Arrays.asList(mode.split("\\s*,\\s*"));
           Set<Route> routesHere = Sets.newHashSet();
           for( Route route : routes ){
               if(modeList.contains(GtfsLibrary.getTraverseMode(route).name())){
                   routesHere.add(route);
               }
           }
           routes.retainAll(routesHere);
       }

       return Response.status(Status.OK).entity(RouteShort.list(routes)).build();
   }

   /**
    * Return specific route in the graph, for the given ID.
    *
    * @param routeIdString route in Agency:Route ID format
    * @return . .
    */
   @GET
   @Path("/routes/{routeId}")
   @TypeHint(Route.class)
   public Response getRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           return Response.status(Status.OK).entity(route).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /**
    * Return all stop patterns used by trips on the given route.
    *
    * @param routeIdString route in Agency:Route ID format.
    * @return . .
    */
   @GET
   @Path("/routes/{routeId}/patterns")
   @TypeHint(PatternShort[].class)
   public Response getPatternsForRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           Collection<TripPattern> patterns = index.patternsForRoute.get(route);
           return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /**
    * Return all stops in any pattern on a given route.
    *
    * @param routeIdString route in Agency:Route ID format
    * @return . .
    */
   @GET
   @Path("/routes/{routeId}/stops")
   @TypeHint(StopShort[].class)
   public Response getStopsForRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           Set<Stop> stops = Sets.newHashSet();
           Collection<TripPattern> patterns = index.patternsForRoute.get(route);
           for (TripPattern pattern : patterns) {
               stops.addAll(pattern.getStops());
           }
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /**
    * Return all trips in any pattern on the given route.
    *
    * @param routeIdString route in Agency:Route ID format
    * @return . .
    */
   @GET
   @Path("/routes/{routeId}/trips")
   @TypeHint(TripShort[].class)
   public Response getTripsForRoute (@PathParam("routeId") String routeIdString) {
       AgencyAndId routeId = GtfsLibrary.convertIdFromString(routeIdString);
       Route route = index.routeForId.get(routeId);
       if (route != null) {
           List<Trip> trips = Lists.newArrayList();
           Collection<TripPattern> patterns = index.patternsForRoute.get(route);
           for (TripPattern pattern : patterns) {
               trips.addAll(pattern.getTrips());
           }
           return Response.status(Status.OK).entity(TripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }
   
   
    // Not implemented, results would be too voluminous.
    // @Path("/trips")

   /**
    * Return trip information by trip ID.
    *
    * @param tripIdString trip in Agency:Trip ID format
    * @return . .
    */
   @GET
   @Path("/trips/{tripId}")
   @TypeHint(Trip.class)
   public Response getTrip (@PathParam("tripId") String tripIdString) {
       AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
       Trip trip = index.getTripForId(tripId);
       if (trip != null) {
           return Response.status(Status.OK).entity(trip).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /**
    * Return stops for a trip.
    *
    * @param tripIdString trip in Agency:Trip ID format
    * @return . .
    */
   @GET
   @Path("/trips/{tripId}/stops")
   @TypeHint(StopShort[].class)
   public Response getStopsForTrip (@PathParam("tripId") String tripIdString) {
       AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
       TripPattern pattern = index.getTripPatternForTripId(tripId);
       if (pattern != null) {
           Collection<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }


    /**
     * Return the semantic hash of a trip
     *
     * @param tripIdString trip in Agency:Trip ID format
     * @return . .
     */
    @GET
    @Path("/trips/{tripId}/semanticHash")
    @TypeHint(String.class)
    public Response getSemanticHashForTrip (@PathParam("tripId") String tripIdString) {
        AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
        Trip trip = index.tripForId.get(tripId);
        if (trip != null) {
            TripPattern pattern = index.patternForTrip.get(trip);
            String hashString = pattern.semanticHashString(trip);
            return Response.status(Status.OK).entity(hashString).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    /**
     * Return arrival/departures for a trip.
     *
     * @param serviceDay service day to look up scheduled for, in UNIX epoch time
     * @param tripIdString trip in Agency:Trip ID format.
     * @return . .
     */
    @GET
    @Path("/trips/{tripId}/stoptimes")
    @TypeHint(TripTimeShort[].class)
    public Response getStoptimesForTrip(@PathParam("tripId") String tripIdString, @QueryParam("serviceDay") Long serviceDay) {
        AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
        TripPattern pattern = index.getTripPatternForTripId(tripId);
        Trip trip = index.getTripForId(tripId);
        if (pattern != null) {
            // Note, we need the updated timetable not the scheduled one (which contains no real-time updates).
            Timetable table;
            if (serviceDay != null) {
                table = index.currentUpdatedTimetableForTripPattern(pattern, new ServiceDate(new Date(serviceDay * 1000)));
            } else {
                table = index.currentUpdatedTimetableForTripPattern(pattern);
            }
            return Response.status(Status.OK).entity(TripTimeShort.fromTripTimes(table, trip)).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    /**
     * Return geometry for the trip as a packed coordinate sequence
     *
     * @param tripIdString trip in Agency:Trip ID format
     * @return . .
     */
    @GET
    @Path("/trips/{tripId}/geometry")
    @TypeHint(EncodedPolylineBean.class)
    public Response getGeometryForTrip (@PathParam("tripId") String tripIdString) {
        AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
        Trip trip = index.tripForId.get(tripId);
        if (trip != null) {
            TripPattern tripPattern = index.patternForTrip.get(trip);
            return getGeometryForPattern(tripPattern.code);
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    /**
     * Return detailed trip information, including stoptimes and geometry.
     *
     * @param serviceDay service day to look up scheduled for, in UNIX epoch time
     * @param tripIdString trip in Agency:Trip ID format
     * @return . .
     */
    @GET
    @Path("/trips/{tripId}/detail")
    @TypeHint(TripDetail.class)
    public Response getTripDetails (@PathParam("tripId") String tripIdString, @QueryParam("serviceDay") Long serviceDay) {
        AgencyAndId tripId = GtfsLibrary.convertIdFromString(tripIdString);
        TripPattern pattern = index.getTripPatternForTripId(tripId);
        Trip trip = index.getTripForId(tripId);
        if (trip != null) {
            Timetable table;
            if (serviceDay != null) {
                table = index.currentUpdatedTimetableForTripPattern(pattern, new ServiceDate(new Date(serviceDay * 1000)));
            } else {
                table = index.currentUpdatedTimetableForTripPattern(pattern);
                serviceDay = (new Date().getTime() / 1000);
            }
            List<TripTimeShort> stopTimes = TripTimeShort.fromTripTimes(table, trip, serviceDay, index.graph.getTimeZone());
            VehicleInfo vehicleInfo = getVehicleInfoForTrip(tripId, pattern);
            TripDetail detail = new TripDetail();
            detail.setTrip(trip);
            if (pattern.geometry != null) {
                EncodedPolylineBean geometry = PolylineEncoder.createEncodings(pattern.geometry);
                detail.setGeometry(geometry);
            }
            detail.setStopTimes(stopTimes);
            detail.setVehicleInfo(vehicleInfo);
            return Response.status(Status.OK).entity(detail).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

   /**
    * Return all patterns in the graph.
    * @return . .
    */
   @GET
   @Path("/patterns")
   @TypeHint(PatternShort[].class)
   public Response getPatterns () {
       Collection<TripPattern> patterns = index.patternForId.values();
       return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
   }

   /**
    * Return details about a pattern.
    *
    * @param patternIdString pattern in Agency:Pattern ID format
    * @return . .
    */
   @GET
   @Path("/patterns/{patternId}")
   @TypeHint(PatternDetail.class)
   public Response getPattern (@PathParam("patternId") String patternIdString) {
       TripPattern pattern = index.patternForId.get(patternIdString);
       if (pattern != null) {
           return Response.status(Status.OK).entity(new PatternDetail(pattern)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /**
    * Return all trips in a pattern.
    *
    * @param patternIdString pattern in Agency:Pattern ID format.
    * @return . .
    */
   @GET
   @Path("/patterns/{patternId}/trips")
   @TypeHint(TripShort[].class)
   public Response getTripsForPattern (@PathParam("patternId") String patternIdString) {
       TripPattern pattern = index.patternForId.get(patternIdString);
       if (pattern != null) {
           List<Trip> trips = pattern.getTrips();
           return Response.status(Status.OK).entity(TripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /**
    * Return all stops for a pattern.
    *
    * @param patternIdString pattern in Agency:Pattern ID format
    * @return . .
    */
   @GET
   @Path("/patterns/{patternId}/stops")
   @TypeHint(StopShort.class)
   public Response getStopsForPattern (@PathParam("patternId") String patternIdString) {
       // Pattern names are graph-unique because we made them that way (did not read them from GTFS).
       TripPattern pattern = index.patternForId.get(patternIdString);
       if (pattern != null) {
           List<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

    /**
     * Return semantic hash for a pattern.
     * @param patternIdString .
     * @return .
     */
    @GET
    @Path("/patterns/{patternId}/semanticHash")
    @TypeHint(String.class)
    public Response getSemanticHashForPattern (@PathParam("patternId") String patternIdString) {
        // Pattern names are graph-unique because we made them that way (did not read them from GTFS).
        TripPattern pattern = index.patternForId.get(patternIdString);
        if (pattern != null) {
            String semanticHash = pattern.semanticHashString(null);
            return Response.status(Status.OK).entity(semanticHash).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    /**
     * Return geometry for the pattern as a packed coordinate sequence
     *
     * @param patternIdString pattern in Agency:Pattern ID format
     * @return . .
     */
    @GET
    @Path("/patterns/{patternId}/geometry")
    @TypeHint(EncodedPolylineBean.class)
    public Response getGeometryForPattern (@PathParam("patternId") String patternIdString) {
        TripPattern pattern = index.patternForId.get(patternIdString);
        if (pattern != null) {
            EncodedPolylineBean geometry = PolylineEncoder.createEncodings(pattern.geometry);
            return Response.status(Status.OK).entity(geometry).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }    

    // TODO include pattern ID for each trip in responses

    /** List basic information about all service IDs.
     * @return . .
     * */
    @GET
    @Path("/services")
    @Ignore
    public Response getServices() {
        index.serviceForId.values(); // TODO complete
        return Response.status(Status.OK).entity("NONE").build();
    }

    /** List details about a specific service ID including which dates it runs on. Replaces the old /calendar.
     * @param serviceId .
     * @return . .
     * */
    @GET
    @Path("/services/{serviceId}")
    @Ignore
    public Response getServices(@PathParam("serviceId") String serviceId) {
        index.serviceForId.get(serviceId); // TODO complete
        return Response.status(Status.OK).entity("NONE").build();
    }

    /** Return all clusters of stops.
    *
    * @param mode only return clusters with this mode. Valid values are TRAM, SUBWAY, RAIL, BUS, FERRY, CABLE_CAR, GONDOLA, FUNICULAR. (optional)
    * @param agencyId only return clusters for this agency (optional)
     * @return . .
    */

    @GET
    @Path("/clusters")
    @TypeHint(StopClusterDetail[].class)
    public Response getAllStopClusters (@QueryParam("mode") String mode, @QueryParam("agencyId") String agencyId) {
        index.clusterStopsAsNeeded();
        Collection<StopCluster> clusters = index.stopClusterForId.values();
        if (agencyId != null) {
            clusters = clusters.stream()
                    .filter(cluster ->
                            cluster.children.stream().anyMatch(stop -> stop.getId().getAgencyId().equals(agencyId)))
                    .collect(Collectors.toList());
        }
        if (mode != null) {
            TraverseMode traverseMode = TraverseMode.valueOf(mode);

            clusters = clusters.stream()
                    .filter(cluster ->
                            cluster.children.stream().anyMatch(stop ->
                                    index.routesForStop(stop).stream()
                            .anyMatch(route ->
                                    GtfsLibrary.getTraverseMode(route) == traverseMode)))
                    .collect(Collectors.toList());
        }
        // use 'detail' field common to all API methods in this class
        List<StopClusterDetail> scl = StopClusterDetail.list(clusters, detail);
        return Response.status(Status.OK).entity(scl).build();
    }

    /**
     * Return a cluster of stops by its ID.
     *
     * @param clusterIdString cluster to return stop data for
     * @return . .
     */
    @GET
    @Path("/clusters/{clusterId}")
    @TypeHint(StopClusterDetail.class)
    public Response getStopCluster (@PathParam("clusterId") String clusterIdString) {
        index.clusterStopsAsNeeded();
        StopCluster cluster = index.stopClusterForId.get(clusterIdString);
        if (cluster != null) {
            return Response.status(Status.OK).entity(new StopClusterDetail(cluster, true)).build();
        } else {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
    }

    /** Get all landmark names
     * @return . .
     * */
    @GET
    @Path("/landmarks")
    @TypeHint(String[].class)
    public Response getLandmarks() {
        Set<String> landmarks = index.graph.landmarksByName.keySet();
        return Response.status(Status.OK).entity(landmarks).build();
    }

    /**
     * Get all stops associated with a given landmark.
     *
     * @param name landmark name
     * @return . .
     */
    @GET
    @Path("/landmarks/{landmark}")
    @TypeHint(StopShort[].class)
    public Response getLandmarkStops(@PathParam("landmark") String name) {

        Landmark landmark = index.graph.landmarksByName.get(name);
        if (landmark == null) {
            return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
        }
        List<StopShort> response = new ArrayList<>();
        for (TransitStationStop stop : landmark.getStops()) {
            response.add(new StopShort(stop.getStop()));
        }
        return Response.status(Status.OK).entity(response).build();
    }

    private VehicleInfo getVehicleInfoForTrip(AgencyAndId tripId, TripPattern pattern) {
        AlertPatch[] alertPatches = index.graph.getAlertPatches(pattern.boardEdges[0]);
        for (AlertPatch patch : alertPatches) {
            if (patch.hasVehicleInfo() && patch.getTrip().equals(tripId)) {
                return patch.getVehicleInfo();
            }
        }
        return null;
    }
}
