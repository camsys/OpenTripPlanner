package org.opentripplanner.ext.flex.edgetype;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
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

import com.vividsolutions.jts.geom.Geometry;

import java.util.Date;
import java.util.Locale;

public class FlexTripEdge extends Edge {

  private static final long serialVersionUID = 3478869956635040033L;

  public final FlexPathCalculator calculator;
  
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
    this.calculator = calculator;
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
		    
		  LineString geometry = 
				  GeometryUtils.makeLineString(((ScheduledDeviatedTrip)getFlexTrip()).geometryCoords.);		    

		  P2<LineString> lineString = 
				  GeometryUtils.splitGeometryAtPoint(geometry, flexTemplate.request.from.getCoordinate());
		  P2<LineString> lineString2 = 
				  GeometryUtils.splitGeometryAtPoint(lineString.first, flexTemplate.request.to.getCoordinate());
		  
		  return lineString2.second;
	  } else
		  return null;
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
	  if(this.flexPath == null)
		  this.flexPath = calculator.calculateFlexPath(fromv, tov, 
				  this.flexTemplate.fromStopIndex, this.flexTemplate.toStopIndex, getFlexTrip());

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
