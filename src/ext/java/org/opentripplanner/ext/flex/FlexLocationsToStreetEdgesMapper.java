package org.opentripplanner.ext.flex;

import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FlexLocationsToStreetEdgesMapper implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(FlexLocationsToStreetEdgesMapper.class);

  @Override
  public void buildGraph(
      Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore
  ) {
    if (graph.locationsById.isEmpty())
      return;

    StreetVertexIndex streetIndex = graph.getStreetIndex();

    Collection<FlexStopLocation> flexStopLocations = graph.locationsById.values();

    ProgressTracker flexStopMappingProgress = ProgressTracker.track("Mapping flex locations to street vertices", 1, flexStopLocations.size());

    LOG.info(flexStopMappingProgress.startMessage());

    // Go through each FlexStopLocation and get associated Vertices
    // Filter vertices that are StreetVertex AND eligible for Car Pickup/Drop-off
    ConcurrentHashMap<StreetVertex, Set<FlexStopLocation>> flexStopLocationsByVertex = new ConcurrentHashMap();
    flexStopLocations.parallelStream().forEach(flexStopLocation -> {
      List<Vertex> vertices = streetIndex.getVerticesForEnvelope(
              flexStopLocation
              .getGeometry()
              .getEnvelopeInternal());

      vertices.parallelStream().forEach(vertex -> {
        if(!isValidFlexStopVertex(vertex)){
          return;
        }
        StreetVertex streetVertex = (StreetVertex) vertex;
        // Put if absent forces the first value to be an empty hashset
        flexStopLocationsByVertex.putIfAbsent(streetVertex, Collections.synchronizedSet(new HashSet<>()));
        flexStopLocationsByVertex.get(streetVertex).add(flexStopLocation);
      });

      flexStopMappingProgress.step(m -> LOG.info(m));
    });

    LOG.info(flexStopMappingProgress.completeMessage());

    ProgressTracker vertixFlexStopUpdateProgress = ProgressTracker.track("Adding filtered flex stop locations to street vertices", 1, flexStopLocationsByVertex.size());

    LOG.info(vertixFlexStopUpdateProgress.startMessage());

    flexStopLocationsByVertex.entrySet().parallelStream().forEach(map -> {

        StreetVertex streetVertex = map.getKey();
        Set<FlexStopLocation> flexStopLocationsForVertex = map.getValue();

        ConcurrentLinkedQueue<FlexStopLocation> filteredFlexStopLocationsForVertex = new ConcurrentLinkedQueue();

        flexStopLocationsForVertex.parallelStream().forEach(s -> {
            Point p = GeometryUtils.getGeometryFactory().createPoint(streetVertex.getCoordinate());
            if (s.getGeometry().disjoint(p))
              return;
            filteredFlexStopLocationsForVertex.add(s);
        });

        if (streetVertex.flexStopLocations == null) {
          streetVertex.flexStopLocations = new HashSet<>();
        }
        streetVertex.flexStopLocations.addAll(filteredFlexStopLocationsForVertex);

        vertixFlexStopUpdateProgress.step(m -> LOG.info(m));
    });

    LOG.info(vertixFlexStopUpdateProgress.completeMessage());

  }

  private static boolean isValidFlexStopVertex(Vertex vertex){
    if(!(vertex instanceof StreetVertex)){
      return false;
    }
    if (!((StreetVertex)vertex).isEligibleForCarPickupDropoff()) {
      return false;
    }
    return true;
  }

  @Override
  public void checkInputs() {
    // No inputs
  }
}
