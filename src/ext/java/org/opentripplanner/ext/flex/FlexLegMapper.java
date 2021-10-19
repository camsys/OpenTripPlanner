package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.core.State;

import java.util.ArrayList;

// TODO: Should flex be of its own type
public class FlexLegMapper {

  static public void fixFlexTripLeg(Leg leg, FlexTripEdge flexTripEdge, State[] states) {	  

	  // if the from location is an area or line, return the location the user searched for *on* that
	  // line or *within* that location vs. the centroid of the line/area. 

	  if(flexTripEdge.s1.isArea() || flexTripEdge.s1.isLine()) {
		  GenericLocation from = states[1].getOptions().from;
		  leg.from = new Place(from.lat, from.lng, from.label);		
		  leg.from.stopId = flexTripEdge.s1.getId();
	  }	  
	  
	  leg.from.stopId = flexTripEdge.s1.getId();		
	  leg.from.vertexType = flexTripEdge.s1 instanceof Stop ? VertexType.TRANSIT : VertexType.NORMAL;
      leg.from.stopIndex = flexTripEdge.flexTemplate.fromStopIndex;

      if(flexTripEdge.getFlexTrip() instanceof ScheduledDeviatedTrip) {
    	  ScheduledDeviatedTrip sdt = (ScheduledDeviatedTrip) flexTripEdge.getFlexTrip();
    	  StopLocation stopLocation = sdt.getStopTime(leg.from.stopIndex).stop;
    	  leg.from.isDeviated = stopLocation.isArea() || stopLocation.isLine();
      }

      
	  if(flexTripEdge.s2.isArea() || flexTripEdge.s2.isLine()) {
		  GenericLocation from = states[1].getOptions().to;
		  leg.to = new Place(from.lat, from.lng, from.label);				  
		  leg.to.stopId = flexTripEdge.s2.getId();
	  }	  

      leg.to.stopId = flexTripEdge.s2.getId();
      leg.to.vertexType = flexTripEdge.s2 instanceof Stop ? VertexType.TRANSIT : VertexType.NORMAL;
      leg.to.stopIndex = flexTripEdge.flexTemplate.toStopIndex;

      if(flexTripEdge.getFlexTrip() instanceof ScheduledDeviatedTrip) {
    	  ScheduledDeviatedTrip sdt = (ScheduledDeviatedTrip) flexTripEdge.getFlexTrip();
    	  StopLocation stopLocation = sdt.getStopTime(leg.to.stopIndex).stop;    	  
    	  leg.to.isDeviated = stopLocation.isArea() || stopLocation.isLine();
      }

      FlexTrip t = flexTripEdge.flexTemplate.getFlexTrip();
      leg.requiresReservation = t.getStopTime(flexTripEdge.fromIndex).pickupType == 2 || t.getStopTime(flexTripEdge.toIndex).dropOffType == 2;     	  
      
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

}
