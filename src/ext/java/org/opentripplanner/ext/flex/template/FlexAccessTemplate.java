package org.opentripplanner.ext.flex.template;

import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.spt.GraphPath;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FlexAccessTemplate extends FlexAccessEgressTemplate {
	
	public FlexAccessTemplate(NearbyStop accessEgress, FlexTrip trip, int fromStopTime, int toStopTime,
			StopLocation transferStop, FlexServiceDate serviceDate, FlexPathCalculator calculator, RoutingRequest request) {
		super(accessEgress, trip, fromStopTime, toStopTime, transferStop, serviceDate, calculator, request);
	}
	  
	public Itinerary createDirectItinerary(NearbyStop egress, boolean arriveBy, int time,
			ZonedDateTime departureServiceDate, FlexIndex flexIndex) {

		List<Edge> egressEdges = egress.edges;
				  
		Vertex flexToVertex = egress.state.getVertex();
		FlexTripEdge flexEdge = getFlexEdge(flexToVertex, egress.stop, flexIndex);
		
		State state = flexEdge.traverse(accessEgress.state);
	    if(state == null) 
	    	return null;

	    for (Edge e : egressEdges) {
			state = e.traverse(state);
		    if(state == null) 
		    	return null;
		}
	    
		Itinerary itinerary = GraphPathToItineraryMapper.generateItinerary(new GraphPath(state), Locale.ENGLISH);

		// change trips that can occur within a range to a specific time that makes sense given the optimization
		// requested
		if(trip instanceof UnscheduledTrip) {
		    if (arriveBy) {
		    	FlexTripStopTime ftst = this.trip.getStopTime(this.toStopIndex);
		    	int newTime = time - itinerary.durationSeconds;
				if(newTime > ftst.flexWindowStart && newTime + itinerary.durationSeconds < ftst.flexWindowEnd) {
					ZonedDateTime zdt = departureServiceDate.plusSeconds(newTime);
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
					c.setTimeInMillis(zdt.toInstant().toEpochMilli());
					itinerary.timeShiftToStartAt(c);
					itinerary.generalizedCost += Math.abs(time - newTime);
				} else{
					return null;
				}
		    } else {		
		    	FlexTripStopTime ftst = this.trip.getStopTime(this.fromStopIndex);
	
		    	if(time > ftst.flexWindowStart && time + itinerary.durationSeconds < ftst.flexWindowEnd) {
			    	ZonedDateTime zdt = departureServiceDate.plusSeconds(time);
					Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
					c.setTimeInMillis(zdt.toInstant().toEpochMilli());
					itinerary.timeShiftToStartAt(c);	 

				// outside travel window
		    	} else {
		    		return null;
		    	}
			}
		}

		return itinerary;
	}
	  
	protected List<Edge> getTransferEdges(SimpleTransfer simpleTransfer) {
		return simpleTransfer.getEdges();
	}

	protected Stop getFinalStop(SimpleTransfer simpleTransfer) {
		return simpleTransfer.to instanceof Stop ? (Stop) simpleTransfer.to : null;
	}

	protected Collection<SimpleTransfer> getTransfersFromTransferStop(Graph graph) {
		return graph.transfersByStop.get(transferStop);
	}

	protected Vertex getFlexVertex(Edge edge) {
		return edge.getFromVertex();
	}

	protected int[] getFlexTimes(FlexTripEdge flexEdge, State state) {
		int preFlexTime = (int) accessEgress.state.getElapsedTimeSeconds();
		int edgeTimeInSeconds = (int)flexEdge.getTripTimeInSeconds();
		int postFlexTime = (int) state.getElapsedTimeSeconds() - preFlexTime - edgeTimeInSeconds;
		return new int[] { preFlexTime, edgeTimeInSeconds, postFlexTime };
	}
	
	protected FlexTripEdge getFlexEdge(Vertex flexToVertex, StopLocation transferStop, FlexIndex flexIndex) {
		boolean allowPickup = flexIndex.hasStopThatAllowsPickup(trip, trip.getStopTime(fromStopIndex).stop);
		boolean allowDropoff = flexIndex.hasStopThatAllowsDropoff(trip, trip.getStopTime(toStopIndex).stop);

		return new FlexTripEdge(accessEgress.state.getVertex(), 
				flexToVertex, 
				accessEgress.stop,
				transferStop,
				this.fromStopIndex,
				this.toStopIndex,
				this, 
				calculator,
				allowPickup,
				allowDropoff);
	}
}
