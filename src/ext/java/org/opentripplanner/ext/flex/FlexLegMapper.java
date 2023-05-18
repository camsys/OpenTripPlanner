package org.opentripplanner.ext.flex;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.core.State;

import org.opentripplanner.routing.graph.Graph;

import java.util.*;
import java.util.stream.Collectors;

// TODO: Should flex be of its own type
public class FlexLegMapper {

  static public void fixFlexTripLeg(Graph graph, Leg leg, FlexTripEdge flexTripEdge, State[] states) {

	  // if the from location is an area or line, return the location the user searched for *on* that
	  // line or *within* that location vs. the centroid of the line/area.
	  if(flexTripEdge.getOriginStop().isArea() || flexTripEdge.getOriginStop().isLine()) {
		  GenericLocation from = states[1].getOptions().from;
		  leg.from = new Place(from.lat, from.lng, from.label != null ? from.label : "Origin");
	  }

	  leg.from.stopId = flexTripEdge.getOriginStop().getId();
	  leg.from.vertexType = flexTripEdge.getOriginStop() instanceof Stop ? VertexType.TRANSIT : VertexType.NORMAL;
      leg.from.stopIndex = flexTripEdge.flexTemplate.fromStopIndex;

	  leg.intermediateStops = new ArrayList<>();
	  leg.interStopGeometry = new ArrayList<>();
      
      if(flexTripEdge.getFlexTrip() instanceof ScheduledDeviatedTrip) {
		  ScheduledDeviatedTrip sdt = (ScheduledDeviatedTrip) flexTripEdge.getFlexTrip();
		  List<String> stIds = Arrays.stream(flexTripEdge.flexTemplate.getFlexTrip().getStopTimes()).map(st->st.stop.getId().getId()).collect(Collectors.toList());
		  //fix stop ids here
		  leg.from.stopIndex = stIds.indexOf(flexTripEdge.getOriginStop().getId().getId());
		  StopLocation stopLocation = sdt.getStopTime(leg.from.stopIndex).stop;
		  leg.from.isDeviated = stopLocation.isArea() || stopLocation.isLine();

		  ServiceDate serviceDate = new ServiceDate(new Date(states[1].getTimeInMillis()));
		  Calendar calendar = new GregorianCalendar();

		  FlexTripStopTime st = flexTripEdge.flexTemplate.getFlexTrip().getStopTime(flexTripEdge.flexTemplate.fromStopIndex);
		  calendar.setTimeInMillis(serviceDate.getAsDate().getTime() +
				  ((st.arrivalTime != StopTime.MISSING_VALUE ? st.arrivalTime : st.flexWindowStart) * 1000));
		  leg.startTime = calendar;
      }

	  if(flexTripEdge.getDestinationStop().isArea() || flexTripEdge.getDestinationStop().isLine()) {
		  GenericLocation to = states[1].getOptions().to;
		  leg.to = new Place(to.lat, to.lng, to.label != null ? to.label : "Destination");
	  }

      leg.to.stopId = flexTripEdge.getDestinationStop().getId();
      leg.to.vertexType = flexTripEdge.getDestinationStop() instanceof Stop ? VertexType.TRANSIT : VertexType.NORMAL;
      leg.to.stopIndex = flexTripEdge.flexTemplate.toStopIndex;

      if(flexTripEdge.getFlexTrip() instanceof ScheduledDeviatedTrip) {
		  ScheduledDeviatedTrip sdt = (ScheduledDeviatedTrip) flexTripEdge.getFlexTrip();
		  List<String> stIds = Arrays.stream(flexTripEdge.flexTemplate.getFlexTrip().getStopTimes()).map(st->st.stop.getId().getId()).collect(Collectors.toList());
		  //fix stop ids here
		  leg.to.stopIndex = stIds.indexOf(flexTripEdge.getDestinationStop().getId().getId());
		  StopLocation stopLocation = sdt.getStopTime(leg.to.stopIndex).stop;
		  leg.to.isDeviated = stopLocation.isArea() || stopLocation.isLine();

		  ServiceDate serviceDate = new ServiceDate(new Date(states[1].getTimeInMillis()));
		  Calendar calendar = new GregorianCalendar();

		  FlexTripStopTime st = flexTripEdge.flexTemplate.getFlexTrip().getStopTime(flexTripEdge.flexTemplate.toStopIndex);
		  calendar.setTimeInMillis(serviceDate.getAsDate().getTime() +
				  ((st.departureTime != StopTime.MISSING_VALUE ? st.departureTime : st.flexWindowEnd)  * 1000));

		  // duration can never be zero, so add 1 to ensure this
		  if(leg.startTime.equals(leg.endTime))
			  calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 1);

		  leg.endTime = calendar;

		  FlexTripStopTime[] sts = sdt.getStopTimes();
		  int interStopGeometriesShift = leg.from.stopIndex > leg.to.stopIndex ? 1 : 0;
		  List<FlexTripStopTime> nonFlexStopTimes = Arrays.stream(sts).filter(s -> s.flexWindowStart == StopTime.MISSING_VALUE && s.flexWindowEnd == StopTime.MISSING_VALUE).collect(Collectors.toList());
		  if (leg.to.stopIndex >= leg.from.stopIndex) {
			  for (int i=leg.from.stopIndex; i < leg.to.stopIndex; i++) {
				  FlexTripStopTime target = sts[i];
				  if (target.flexWindowStart == StopTime.MISSING_VALUE && target.flexWindowEnd == StopTime.MISSING_VALUE) {
					  addFixedDeviatedIntermediateProps(leg,target,nonFlexStopTimes,graph,flexTripEdge,interStopGeometriesShift);
				  }
			  }
		  } else {
			  for (int i=leg.from.stopIndex; i >= leg.to.stopIndex; i--) {
				  FlexTripStopTime target = sts[i];
				  if (target.flexWindowStart == StopTime.MISSING_VALUE && target.flexWindowEnd == StopTime.MISSING_VALUE) {
					  addFixedDeviatedIntermediateProps(leg,target,nonFlexStopTimes,graph,flexTripEdge,interStopGeometriesShift);
				  }
			  }
		  }
		  //remove extra interstop geometry
		  if (leg.interStopGeometry.size() > 1) {
			  leg.interStopGeometry.remove(leg.interStopGeometry.size()-1);
		  }
      }

      FlexTrip t = flexTripEdge.flexTemplate.getFlexTrip();
      
      leg.requiresReservation = t.getStopTime(flexTripEdge.flexTemplate.fromStopIndex).pickupType == 2 || t.getStopTime(flexTripEdge.flexTemplate.toStopIndex).dropOffType == 2;

	  leg.distanceMeters = flexTripEdge.getDistanceMeters();

      leg.serviceDate = flexTripEdge.flexTemplate.serviceDate.serviceDate;
      leg.headsign = flexTripEdge.getTrip().getTripHeadsign();
      leg.walkSteps = new ArrayList<>();

      leg.boardRule = GraphPathToItineraryMapper.getBoardAlightMessage(2);
      leg.alightRule = GraphPathToItineraryMapper.getBoardAlightMessage(3);

      leg.dropOffBookingInfo = flexTripEdge.getFlexTrip().getDropOffBookingInfo(leg.from.stopIndex);
      leg.pickupBookingInfo = flexTripEdge.getFlexTrip().getPickupBookingInfo(leg.from.stopIndex);
  }

  static private void addFixedDeviatedIntermediateProps(Leg leg, FlexTripStopTime target, List<FlexTripStopTime> nonFlexStopTimes, Graph graph, FlexTripEdge flexTripEdge, int interStopGeometryShift) {
	  int nonFlexStopIndex = nonFlexStopTimes.indexOf(target);
	  double latitude = target.stop.getCoordinate().latitude();
	  double longitude = target.stop.getCoordinate().longitude();
	  String targetStopName = target.stop.getName();
	  Place interStopPlace = new Place(latitude,longitude,targetStopName);
	  interStopPlace.stopId = target.stop.getId();
	  //for now, we don't need arrival times below for intermediate stops
	  leg.intermediateStops.add(new StopArrival(interStopPlace,null,null));
	  if (nonFlexStopIndex - interStopGeometryShift < 0) {
		  leg.interStopGeometry.add(null);//add dummy geom
	  } else {
		  leg.interStopGeometry.add(graph.index.getPatternForTrip().get(flexTripEdge.getTrip()).getHopGeometry(nonFlexStopIndex - interStopGeometryShift));
	  }
  }

}
