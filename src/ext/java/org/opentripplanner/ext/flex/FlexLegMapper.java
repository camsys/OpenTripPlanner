package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import java.util.ArrayList;

// TODO: Should flex be of its own type
public class FlexLegMapper {

  static public void fixFlexTripLeg(Leg leg, FlexTripEdge flexTripEdge) {	  
	  leg.from.stopId = flexTripEdge.s1.getId();
		
      leg.from.vertexType = flexTripEdge.s1 instanceof Stop ? VertexType.TRANSIT : VertexType.NORMAL;
      leg.from.stopIndex = flexTripEdge.flexTemplate.fromStopIndex;

      if(flexTripEdge.getFlexTrip() instanceof ScheduledDeviatedTrip) {
    	  ScheduledDeviatedTrip sdt = (ScheduledDeviatedTrip) flexTripEdge.getFlexTrip();
    	  StopLocation stopLocation = sdt.getStopTime(leg.from.stopIndex).stop;
    	  leg.from.isDeviated = stopLocation.isArea() || stopLocation.isLine();
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
