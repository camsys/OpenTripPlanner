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

		// There's no way to model wait time in a state as returned from edge traversal,
		// so we need to shift times here so the itinerary object can model the proper start
		// time of the trip.
		int[] flexTimes = getFlexTimes(flexEdge, state);

		int preFlexTime = flexTimes[0];
		int flexTime = flexTimes[1];
		int postFlexTime = flexTimes[2];

		Integer timeShift = null;

		if (arriveBy) {
			int flexWindowEnd = trip.latestArrivalTime(departureTime - postFlexTime, fromStopIndex, toStopIndex);

			// check pickup/dropoff times against constraints
			int arrivalTime = departureTime + preFlexTime + flexTime;			
			if(!trip.isBoardingPossible((StopLocation)trip.getStops().toArray()[fromStopIndex], departureTime)
				&& !trip.isAlightingPossible((StopLocation)trip.getStops().toArray()[toStopIndex], arrivalTime)) 
				return null;

			timeShift = flexWindowEnd - flexTime - preFlexTime;
		} else {
			int flexWindowStart = trip.earliestDepartureTime(departureTime + preFlexTime, fromStopIndex, toStopIndex);

			// check pickup/dropoff times against constraints
			int arrivalTime = departureTime + preFlexTime + flexTime;
			if(!trip.isBoardingPossible((StopLocation)trip.getStops().toArray()[fromStopIndex], departureTime)
				&& !trip.isAlightingPossible((StopLocation)trip.getStops().toArray()[toStopIndex], arrivalTime)) 
				return null;
					
			timeShift =  flexWindowStart - preFlexTime;
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
