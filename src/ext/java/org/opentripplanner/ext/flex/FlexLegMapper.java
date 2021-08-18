package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

// TODO: Should flex be of its own type
public class FlexLegMapper {

  static public void fixFlexTripLeg(Leg leg, FlexTripEdge flexTripEdge, State[] states) {
      leg.from.stopId = flexTripEdge.s1.getId();
      
      leg.startTime = makeCalendar(
    		  flexTripEdge.flexTemplate.getAccessEgress().state.getContext().graph.getTimeZone(), 
    		  flexTripEdge.flexTemplate.getAccessEgress().state.getTimeInMillis()); //  flexTripEdge.flexTemplate.getAccessEgress().state.getElapsedTimeSeconds());      
      leg.endTime = makeCalendar(
    		  flexTripEdge.flexTemplate.getAccessEgress().state.getContext().graph.getTimeZone(),
    		  leg.startTime.getTimeInMillis() + flexTripEdge.getTripTimeInSeconds() * 1000); //, flexTripEdge.flexTemplate.getAccessEgress().state.getElapsedTimeSeconds());

      /*
      int preFlexTime = -1;
      int edgeTimeInSeconds = -1;
      int postFlexTime = -1;
      if(flexTripEdge.flexTemplate instanceof FlexAccessTemplate) {      
		preFlexTime = (int) flexTripEdge.flexTemplate.getAccessEgress().state.getElapsedTimeSeconds();
		edgeTimeInSeconds = flexTripEdge.getTripTimeInSeconds();
		postFlexTime = (int) flexTripEdge.flexTemplate.getAccessEgress().state.getElapsedTimeSeconds() - preFlexTime - edgeTimeInSeconds;

      } else if(flexTripEdge.flexTemplate instanceof FlexEgressTemplate) {
    	postFlexTime = (int) flexTripEdge.flexTemplate.getAccessEgress().state.getElapsedTimeSeconds();
    	edgeTimeInSeconds = flexTripEdge.getTripTimeInSeconds();
    	preFlexTime = (int) flexTripEdge.flexTemplate.getAccessEgress().state.getElapsedTimeSeconds() - postFlexTime - edgeTimeInSeconds;
      }
      */
		
      leg.from.vertexType = flexTripEdge.s1 instanceof Stop ? VertexType.TRANSIT : VertexType.NORMAL;
      leg.from.stopIndex = flexTripEdge.flexTemplate.fromStopIndex;
      leg.to.stopId = flexTripEdge.s2.getId();
      leg.to.vertexType = flexTripEdge.s2 instanceof Stop ? VertexType.TRANSIT : VertexType.NORMAL;
      leg.to.stopIndex = flexTripEdge.flexTemplate.toStopIndex;
            
      leg.intermediateStops = new ArrayList<>();
      leg.distanceMeters = flexTripEdge.getDistanceMeters();

      leg.serviceDate = flexTripEdge.flexTemplate.serviceDate.serviceDate;
      leg.headsign = flexTripEdge.getTrip().getTripHeadsign();
      leg.walkSteps = new ArrayList<>();

      leg.boardRule = GraphPathToItineraryMapper.getBoardAlightMessage(2);
      leg.alightRule = GraphPathToItineraryMapper.getBoardAlightMessage(3);

      leg.dropOffBookingInfo = flexTripEdge.getFlexTrip().getDropOffBookingInfo(leg.from.stopIndex);
      leg.pickupBookingInfo = flexTripEdge.getFlexTrip().getPickupBookingInfo(leg.from.stopIndex);
  }

  private static Calendar makeCalendar(TimeZone timeZone, long t) { //, long offset) {
      Calendar calendar = Calendar.getInstance(timeZone);
      calendar.setTimeInMillis(t); 
      return calendar;
  }

}
