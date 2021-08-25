package org.opentripplanner.ext.flex.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import java.util.Locale;

public class FlexTripEdge extends Edge {

  private static final long serialVersionUID = 1L;

  public final StopLocation s1;
  public final StopLocation s2;
  
  public final FlexAccessEgressTemplate flexTemplate;
  public final FlexPath flexPath;

  @SuppressWarnings("serial")
  public FlexTripEdge(
      Vertex v1, Vertex v2, StopLocation s1, StopLocation s2, int fromIndex, int toIndex,
      FlexAccessEgressTemplate flexTemplate, FlexPathCalculator calculator
  ) {
	  
    // null graph because we don't want this edge to be added to the edge lists.
    super(new Vertex(null, null, 0.0, 0.0) {}, new Vertex(null, null, 0.0, 0.0) {});
    
    this.s1 = s1;
    this.s2 = s2;
    this.flexTemplate = flexTemplate;
    this.fromv = v1;
    this.tov = v2;
    this.flexPath = calculator.calculateFlexPath(fromv, tov, fromIndex, toIndex, flexTemplate.getFlexTrip());
  }

  @Override
  public State traverse(State s0) {
	if(this.flexPath == null)
		return null; // = not routable
	  
	StateEditor editor = s0.edit(this);
    editor.setBackMode(TraverseMode.BUS);
    editor.incrementTimeInSeconds((int)getTripTimeInSeconds());
    editor.incrementWeight(getTripTimeInSeconds()/100);
    
    editor.resetEnteredNoThroughTrafficArea();
    
    return editor.makeState();
  }

  // This method uses the "mean" time from Flex v2 to best reflect the typical travel 
  // scenario in user-facing interfaces vs. using the worst case scenario re: trip length
  public float getTripTimeInSeconds() {
    return getFlexTrip().getMeanTotalTime(flexPath, flexTemplate.fromStopIndex, 
    		flexTemplate.toStopIndex);
  }

  @Override
  public double getDistanceMeters() {
    return flexPath.distanceMeters;
  }

  @Override
  public LineString getGeometry() {
    return flexPath.geometry;
  }

  @Override
  public Trip getTrip() {
    return getFlexTrip().getTrip();
  }

  public FlexTrip getFlexTrip() {
    return flexTemplate.getFlexTrip();
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
