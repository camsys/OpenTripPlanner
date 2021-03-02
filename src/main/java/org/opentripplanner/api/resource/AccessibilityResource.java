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
package org.opentripplanner.api.resource;

import org.joda.time.DateTime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.model.PairwiseAccessibilityShort;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.consequences.ConsequencesStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge.Mode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("routers/{routerId}/accessibility") 
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class AccessibilityResource {

    private static final Logger LOG = LoggerFactory.getLogger(AccessibilityResource.class);

    private static final String MSG_404 = "FOUR ZERO FOUR";
    
    private static final String MSG_400 = "FOUR HUNDRED";

    /** The date that the trip should depart (or arrive, for requests where arriveBy is true). For example: <code>09/01/2017</code> */
    @QueryParam("date")
    public String date;
    
    /**
     * If true, realtime updates are ignored during this search. Defaults to false.
     */
    @QueryParam("ignoreRealtimeUpdates")
    public Boolean ignoreRealtimeUpdates;
    
    private GraphIndex index;
    
    private Graph graph;
    
    public AccessibilityResource (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        this.index = router.graph.index;
        this.graph = router.graph;
    }

    public AccessibilityResource (Router router, GraphIndex index) {
        this.index = router.graph.index;
        this.graph = router.graph;
    }

    
    /**
     * @param stopIdString stop in Agency:Stop ID format.
     * @throws Exception 
     * @throws ParameterException 
     */
    @GET
    @Path("/stop/{stopId}")
    public Response stopAccessibility (@PathParam("stopId") String stopIdString) throws Exception {
    	ArrayList<PairwiseAccessibilityShort> result = getStopAccessibility(stopIdString);
    	
    	if(result != null) 
    		return Response.status(Status.OK).entity(result).build();
    	else
    		return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
    }
    

    public ArrayList<PairwiseAccessibilityShort> getStopAccessibility(String fromStopIdString) throws Exception {
    	ArrayList<PairwiseAccessibilityShort> result = new ArrayList<PairwiseAccessibilityShort>();
       	
    	Stop fromGTFSStop = index.stopForId.get(AgencyAndId.convertFromString(fromStopIdString));

    	TransitStationStop fromStop = 
   				(TransitStationStop)index.stopVertexForStop.get(fromGTFSStop);

        Set<Vertex> connectionsTo = index.connectionsFromMap.get(fromStopIdString);

        for(Vertex connectionTo : connectionsTo) {
       		TransitStationStop toStop = (TransitStationStop)connectionTo;

    		if(toStop.getStopId().equals(fromStop.getStopId()))
   				continue;
    		
        	result.addAll(getStopAccessibility(fromStop.getStop(), toStop.getStop()));
       	}
           	
        return result;
    }
    
    public ArrayList<PairwiseAccessibilityShort> getPairwiseAccessibility(List<AgencyAndId> ids) throws Exception {
       	ArrayList<PairwiseAccessibilityShort> result = new ArrayList<PairwiseAccessibilityShort>();
    	
       	HashSet<String> finishedQueries = new HashSet<String>();
    	for(AgencyAndId from : ids) {
        	for(AgencyAndId to : ids) {
        		if(finishedQueries.contains(from + "" + to) || finishedQueries.contains(to + "" + from))
        			continue;
        		
        		Stop fromStop = 
           				index.stopForId.get(from);

        		Stop toStop = 
           				index.stopForId.get(to);

            	result.addAll(getStopAccessibility(fromStop, toStop));
            	
            	finishedQueries.add(fromStop.getId() + "" + toStop.getId());
        	}
    	}

    	return result;
    }
    
    public ArrayList<PairwiseAccessibilityShort> getStopAccessibility(Stop fromStop, Stop toStop) throws Exception {
	   	ArrayList<PairwiseAccessibilityShort> result = new ArrayList<PairwiseAccessibilityShort>();
		
        ServiceDate sd = new ServiceDate();
        if(this.date != null) 
        	sd = new ServiceDate(new DateTime(date).toDate());
        
        if(ignoreRealtimeUpdates == null)
        	ignoreRealtimeUpdates = false;
       
        TransitStationStop fromStopV = 
       			(TransitStationStop)index.stopVertexForStop.get(fromStop);

        TransitStationStop toStopV = 
   				(TransitStationStop)index.stopVertexForStop.get(toStop);

	    RoutingRequest request = new RoutingRequest();
		GenericDijkstra algo = new GenericDijkstra(request);
		algo.setSkipEdgeStrategy(new SkipNonPathwayEdgeStrategy());
	    request.setMode(TraverseMode.WALK);
	    request.numberOfDepartures = 1;
	    request.wheelchairAccessible = true;
	    request.dominanceFunction = new DominanceFunction.MinimumWeight(); // FORCING the dominance function to weight only
	
	   	request.setRoutingContext(graph, fromStopV, toStopV);

        ConsequencesStrategy consequencesStrategy = request.rctx.graph.consequencesStrategy.create(request);

    	State s0 = State.stateAllowingTransfer(fromStopV, request);
        ShortestPathTree spt = algo.getShortestPathTree(s0);

        PairwiseAccessibilityShort resultItem = new PairwiseAccessibilityShort();
        resultItem.from = fromStop;
        resultItem.to = toStop;
        resultItem.isCurrentlyAccessible = false;

        if(toStop.getLocationType() == Stop.LOCATION_TYPE_STATION || toStop.getLocationType() == Stop.LOCATION_TYPE_STOP) {
        	resultItem.service = new HashSet<RouteShort>();
        	for(StopTimesInPattern st : index.getStopTimesForStop(toStop, sd, true, ignoreRealtimeUpdates)) {
        		resultItem.service.add(st.route);
        	}
        }
        
        List<GraphPath> paths = spt.getPaths();
        if(!paths.isEmpty()) {
            resultItem.dependsOnEquipment = new HashSet<String>();
            resultItem.isCurrentlyAccessible = true;

            resultItem.alerts = new HashSet<Alert>();
            if(!ignoreRealtimeUpdates) {
            	resultItem.alerts.addAll(consequencesStrategy.getConsequences(paths));
            }
                                
            for(GraphPath path : paths) {
            	for(Edge edge : path.edges) {
            		PathwayEdge pe = (PathwayEdge)edge;
            	                    		
            		if(!pe.isWheelchairAccessible()) 
                        resultItem.isCurrentlyAccessible = false;

            		if(pe.getElevatorId() != null) {
            			resultItem.dependsOnEquipment.add(pe.getElevatorId());

                        if(!ignoreRealtimeUpdates) {
                	        for (AlertPatch alert : graph.getAlertPatches(pe)) {
                	            if (alert.displayDuring(path.states.getLast()) 
                	            		&& alert.getElevatorId() != null && pe.getPathwayMode() == Mode.ELEVATOR) {
                	            	resultItem.alerts.add(alert.getAlert());
                	            	
                	            	if(alert.isRoutingConsequence())
                	            		resultItem.isCurrentlyAccessible = false;
                	            }
                	        }
                        }
            		}
            	}
            } // foreach paths
        }
           		
        result.add(resultItem);
        	
        return result;
     }
    
  	 class SkipNonPathwayEdgeStrategy implements SkipEdgeStrategy {
			@Override
			public boolean shouldSkipEdge(Vertex origin, Vertex target, State current, Edge edge, ShortestPathTree spt,
					RoutingRequest traverseOptions) {
				
				if(!(edge instanceof PathwayEdge))
					return true;
				else
					return false;
			}
    		
    	}

}
