package org.opentripplanner.routing.algorithm.astar.strategies;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Set;

public class GeoBoundarySkipEdgeStrategy implements SkipEdgeStrategy {

  private final Geometry boundary;

  private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();             

  public GeoBoundarySkipEdgeStrategy(Geometry boundary) {
    this.boundary = boundary;
  }

  @Override
  public boolean shouldSkipEdge(
      Set<Vertex> origins,
      Set<Vertex> targets,
      State current,
      Edge edge, 
      ShortestPathTree spt,
      RoutingRequest traverseOptions
  ) {
	if(!(edge instanceof StreetEdge || edge instanceof TemporaryFreeEdge)) 
		return true;
	  
	boolean shouldSkip = true;
	
    for(Vertex v : targets) {
    	Geometry point = geometryFactory.createPoint(v.getCoordinate());    	
    	if(boundary.contains(point)) {
    		shouldSkip = false;
    		break;
    	}
    }
    return shouldSkip;
  }
}
