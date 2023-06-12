package org.opentripplanner.ext.flex.edgetype;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.FlexTripStopTime;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.Date;
import java.util.Locale;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

public class FlexTripEdge extends Edge {

  private static final long serialVersionUID = 3478869956635040033L;
  
  public final StopLocation from; // resolved stops in the case of groups

  public final StopLocation to;  // resolved stops in the case of groups

  public final FlexAccessEgressTemplate flexTemplate;
  
  public FlexPath flexPath = null;

  @SuppressWarnings("serial")
  public FlexTripEdge(
	Vertex v1, Vertex v2, StopLocation s1, StopLocation s2, int fromIndex, int toIndex,
	FlexAccessEgressTemplate flexTemplate, FlexPathCalculator calculator
  ) {	    
    // null graph because we don't want this edge to be added to the edge lists.
    super(new Vertex(null, null, 0.0, 0.0) {}, new Vertex(null, null, 0.0, 0.0) {});
    this.from = s1;
    this.to = s2;
    this.fromv = v1;
    this.tov = v2;
    this.flexTemplate = flexTemplate;
    
    FlexTrip trip = flexTemplate.getFlexTrip();
    if(trip instanceof ScheduledDeviatedTrip) {
        FlexTripStopTime fromST = trip.getStopTime(fromIndex);
        FlexTripStopTime toST = trip.getStopTime(toIndex);

    	int newFromST = 0;
    	int newToST = 0;
    	
    	if(fromST.departureTime != StopTime.MISSING_VALUE)
    		newFromST = fromST.departureTime;
    	else
    		newFromST = fromST.flexWindowEnd - ((fromST.flexWindowEnd - fromST.flexWindowStart) / 2);

    	if(toST.arrivalTime != StopTime.MISSING_VALUE)
    		newToST = toST.arrivalTime;
    	else
    		newToST = toST.flexWindowStart + ((toST.flexWindowEnd - toST.flexWindowStart) / 2);

    	int duration = Math.abs(newToST - newFromST);    	

    	if(duration == 0) 
  		  this.flexPath = calculator.calculateFlexPath(fromv, tov, 
				  this.flexTemplate.fromStopIndex, this.flexTemplate.toStopIndex, getFlexTrip());
    	    	
		this.flexPath = new FlexPath(0, duration, null);
    
    } else if(trip instanceof UnscheduledTrip) {
		  this.flexPath = calculator.calculateFlexPath(fromv, tov, 
				  this.flexTemplate.fromStopIndex, this.flexTemplate.toStopIndex, getFlexTrip());
    }   
  }

  @Override
  public State traverse(State s0) {	  
	StateEditor editor = s0.edit(this);
    editor.setBackMode(TraverseMode.BUS);
    
    // should this be modeled as an edge traversal? 
    int wait = 0;
    if(getFlexTrip() instanceof ScheduledDeviatedTrip) {
    	FlexTripStopTime ftst = getFlexTrip().getStopTime(this.flexTemplate.fromStopIndex);
    	
    	int departureTime = 0;
    	if(ftst.departureTime != StopTime.MISSING_VALUE)
    		departureTime = ftst.departureTime;
    	else
    		departureTime = ftst.flexWindowStart + ((ftst.flexWindowEnd - ftst.flexWindowStart) / 2);
    	
	    long serviceDate = new ServiceDate(new Date(s0.getTimeInMillis())).getAsDate().getTime();
	    int offsetFromMidnight = (int)((s0.getTimeInMillis() - serviceDate) / 1000);
	    
	    wait = departureTime - offsetFromMidnight;    
	    if(wait < 0)
	    	return null; // missed it
    }
    else if(getFlexTrip() instanceof UnscheduledTrip){
        FlexTripStopTime fromStopTime = getFlexTrip().getStopTime(this.flexTemplate.fromStopIndex);
        FlexTripStopTime toStopTime = getFlexTrip().getStopTime(this.flexTemplate.toStopIndex);

        if(fromStopTime.pickupType == PICKDROP_NONE || toStopTime.dropOffType == PICKDROP_NONE){
            return null;
        }
    }
    
    if(getFlexPath() == null)
    	return null; // not routable
    
    editor.incrementWeight(getTripTimeInSeconds() + wait);
    editor.incrementTimeInSeconds((int)getTripTimeInSeconds() + wait);
    
    editor.resetEnteredNoThroughTrafficArea();
    
    return editor.makeState();
  }

  // This method uses the "mean" time from Flex v2 to best reflect the typical travel 
  // scenario in user-facing interfaces vs. using the worst case scenario re: trip length
  public float getTripTimeInSeconds() {
	  return getFlexTrip().getMeanTotalTime(getFlexPath(), 
			  this.flexTemplate.fromStopIndex, this.flexTemplate.toStopIndex);
  }

  @Override
  public double getDistanceMeters() {
	  return getFlexPath().distanceMeters;
  }

  @Override
  public LineString getGeometry() {
      if(flexTemplate.getFlexTrip() instanceof UnscheduledTrip)
          return getFlexPath().geometry;
      else if(flexTemplate.getFlexTrip() instanceof ScheduledDeviatedTrip) {

          LineString geometry = GeometryUtils.makeLineString(((ScheduledDeviatedTrip)getFlexTrip()).geometryCoords);

          if(flexTemplate.request.from.getCoordinate() != null && flexTemplate.request.to.getCoordinate() != null) {

              Coordinate from = null;
              Coordinate to = null;
              if (this.getOriginStop().isArea() || this.getOriginStop().isLine()) {
                  from = getClosestPointOnLine(geometry, flexTemplate.request.from.getCoordinate());
              } else {
                  from = flexTemplate.request.from.getCoordinate();
              }

              if (this.getDestinationStop().isArea() || this.getDestinationStop().isLine()) {
                  to = getClosestPointOnLine(geometry, flexTemplate.request.to.getCoordinate());
              } else {
                  to = flexTemplate.request.to.getCoordinate();
              }

              LineString l = GeometryUtils.getInteriorSegment(geometry, from, to);

              // from needs to be after to for this method to work,
              // this seems like a cheap way to find out? HACK FIXME
              if (l.getLength() > 0)
                  return l;
              else
                  return GeometryUtils.getInteriorSegment(geometry, to, from);
          }
      }
      return null;
  }
  
  private Coordinate getClosestPointOnLine(Geometry line, Coordinate coord) {
      Geometry g = line.getGeometryN(0);
      GeometryFactory gf = g.getFactory();

      Point p = gf.createPoint(coord);

      Coordinate[] nearestCoords = DistanceOp.nearestPoints(g, p.getGeometryN(0));
      return nearestCoords[0];
  }
  
  

  public StopLocation getOriginStop() {
      return this.from;
//	  return flexTemplate.getFlexTrip().getStops().get(flexTemplate.fromStopIndex);
  }

  public StopLocation getDestinationStop() {
      return this.to;
//	  return flexTemplate.getFlexTrip().getStops().get(flexTemplate.toStopIndex);
  }

  public FlexPath getFlexPath() {
      return this.flexPath;
  }
  
  public FlexTrip getFlexTrip() {
    return flexTemplate.getFlexTrip();
  }

  // this is the OTP trip that underlies the FlexTrip, not sure this is used anywhere
  @Override
  public Trip getTrip() {
    return getFlexTrip().getTrip();
  }

  @Override
  public String getName() {
    return getTrip().getId().toString();
  }

  @Override
  public String getName(Locale locale) {
      return getName();
  }
}
