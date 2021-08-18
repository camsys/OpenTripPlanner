package org.opentripplanner.ext.flex.template;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class FlexAccessTemplate extends FlexAccessEgressTemplate {
	
	private static final Logger LOG = LoggerFactory.getLogger(FlexAccessTemplate.class);

	public FlexAccessTemplate(NearbyStop accessEgress, FlexTrip trip, int fromStopTime, int toStopTime,
			StopLocation transferStop, FlexServiceDate serviceDate, FlexPathCalculator calculator) {
		super(accessEgress, trip, fromStopTime, toStopTime, transferStop, serviceDate, calculator);
	}

	public Itinerary createDirectItinerary(NearbyStop egress, boolean arriveBy, int departureTime,
			ZonedDateTime departureServiceDate) {

	    LOG.debug("Trip: " + this.getFlexTrip() + 
	    		" From: " + this.getFlexTrip().getStops().toArray()[this.fromStopIndex] + 
	    		" To:" + this.getFlexTrip().getStops().toArray()[this.toStopIndex]);

		List<Edge> egressEdges = egress.edges;

		Vertex flexToVertex = egress.state.getVertex();
		FlexTripEdge flexEdge = getFlexEdge(flexToVertex, egress.stop);
		
		State state = flexEdge.traverse(accessEgress.state);
	    if(state == null) return null;

	    for (Edge e : egressEdges) {
			state = e.traverse(state);
		    if(state == null) return null;
		}
	    
	    // check that we can make this trip re: pickup/dropoff time restrictions
	    int timeAsOffsetSinceServiceDate = 
	    		(int)(((arriveBy ? egress.state : accessEgress.state).getTimeInMillis() - this.serviceDate.serviceDate.getAsDate().getTime()) / 1000);

//	    if(!this.getFlexTrip().isBoardingPossible(accessEgress.stop, timeAsOffsetSinceServiceDate))
//	        return null;
	    	
//	    if(!this.getFlexTrip().isAlightingPossible(egress.stop, timeAsOffsetSinceServiceDate + flexEdge.getTripTimeInSeconds()))
//			return null;	    
	    
	    int timeShift = 0;
	    int[] flexTimes = getFlexTimes(flexEdge, state);
	    
		if (arriveBy) {
			int flexWindowEnd = trip.latestArrivalTime(departureTime - flexTimes[2], fromStopIndex, toStopIndex);
			if(flexWindowEnd == -1) return null;

			timeShift = flexWindowEnd - flexTimes[1] - flexTimes[0];
		} else {
			int flexWindowStart = trip.earliestDepartureTime(departureTime + flexTimes[0], fromStopIndex, toStopIndex);
			if(flexWindowStart == -1) return null;
			
			timeShift =  flexWindowStart - flexTimes[0];
		}

		Itinerary itinerary = GraphPathToItineraryMapper.generateItinerary(new GraphPath(state), Locale.ENGLISH);
		
		ZonedDateTime zdt = departureServiceDate.plusSeconds(timeShift);
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
		c.setTimeInMillis(zdt.toInstant().toEpochMilli());
		itinerary.timeShiftToStartAt(c);
	    
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
		int edgeTimeInSeconds = flexEdge.getTripTimeInSeconds();
		int postFlexTime = (int) state.getElapsedTimeSeconds() - preFlexTime - edgeTimeInSeconds;
		return new int[] { preFlexTime, edgeTimeInSeconds, postFlexTime };
	}
	
	protected FlexTripEdge getFlexEdge(Vertex flexToVertex, StopLocation transferStop) {
		return new FlexTripEdge(accessEgress.state.getVertex(), 
				flexToVertex, 
				accessEgress.stop,
				transferStop,
				this, 
				calculator);
	}
}
