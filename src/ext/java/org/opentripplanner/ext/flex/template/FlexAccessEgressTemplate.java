package org.opentripplanner.ext.flex.template;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class FlexAccessEgressTemplate {
  
  public final RoutingRequest request;
  
  protected final NearbyStop accessEgress;

  protected final StopLocation transferStop;
  
  protected final FlexTrip trip;
  
  public final int fromStopIndex;
  
  public final int toStopIndex;
  
  public final FlexServiceDate serviceDate;
  
  protected final FlexPathCalculator calculator;

  /**
   *
   * @param accessEgress  Path from origin to the point of boarding for this flex trip
   * @param trip          The FlexTrip used for this Template
   * @param fromStopIndex Stop sequence index where this FlexTrip is boarded
   * @param toStopIndex   The stop where this FlexTrip alights
   * @param transferStop  The stop location where this FlexTrip alights
   * @param serviceDate   The service date of this FlexTrip
   * @param calculator    Calculates the path and duration of the FlexTrip
   */
  FlexAccessEgressTemplate(
      NearbyStop accessEgress,
      FlexTrip trip,
      int fromStopIndex,
      int toStopIndex,
      StopLocation transferStop,
      FlexServiceDate serviceDate,
      FlexPathCalculator calculator,
      RoutingRequest request
  ) {
    this.accessEgress = accessEgress;
    this.trip = trip;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.transferStop = transferStop;
    this.serviceDate = serviceDate;
    this.calculator = calculator;
    this.request = request;
  }

  public StopLocation getTransferStop() {
    return transferStop;
  }

  public StopLocation getAccessEgressStop() {
    return accessEgress.stop;
  }

  public NearbyStop getAccessEgress() {
	return this.accessEgress;
  }

  public FlexTrip getFlexTrip() {
    return trip;
  }

  /**
   * Get a list of edges used for transferring to and from the scheduled transit network. The edges
   * should be in the order of traversal of the state in the NearbyStop
   * */
  abstract protected List<Edge> getTransferEdges(SimpleTransfer simpleTransfer);

  /**
   * Get the {@Link Stop} where the connection to the scheduled transit network is made.
   */
  abstract protected Stop getFinalStop(SimpleTransfer simpleTransfer);

  /**
   * Get the transfers to/from stops in the scheduled transit network from the beginning/end of the
   * flex ride for the access/egress.
   */
  abstract protected Collection<SimpleTransfer> getTransfersFromTransferStop(Graph graph);

  /**
   * Get the {@Link Vertex} where the flex ride ends/begins for the access/egress.
   */
  abstract protected Vertex getFlexVertex(Edge edge);

  /**
   * Get the times in seconds, before, during and after the flex ride.
   */
  abstract protected int[] getFlexTimes(FlexTripEdge flexEdge, State state);
  
  /**
   * Get the FlexTripEdge for the flex ride.
   */
  abstract protected FlexTripEdge getFlexEdge(Vertex flexFromVertex, StopLocation transferStop, FlexIndex flexIndex);

  public Stream<FlexAccessEgress> createFlexAccessEgressStream(Graph graph) {
    FlexIndex flexIndex = graph.index.getFlexIndex();
    if (transferStop instanceof Stop) {
      TransitStopVertex flexVertex = graph.index.getStopVertexForStop().get(transferStop);
      return Stream.of(getFlexAccessEgress(new ArrayList<>(), flexVertex, (Stop) transferStop, flexIndex));

    } else {
      // transferStop is Location Area/Line
      List<FlexAccessEgress> r = getTransfersFromTransferStop(graph)
          .parallelStream()
          .filter(simpleTransfer -> getFinalStop(simpleTransfer) != null && getTransferEdges(simpleTransfer).size() > 0)
          .map(simpleTransfer -> {
            List<Edge> edges = getTransferEdges(simpleTransfer);
            return getFlexAccessEgress(edges,
                getFlexVertex(edges.get(0)),
                getFinalStop(simpleTransfer),
                flexIndex
            );
          })
          .collect(Collectors.toList());
          
      return r.stream();
    }
  }

  protected FlexAccessEgress getFlexAccessEgress(List<Edge> transferEdges, Vertex flexVertex, Stop stop, FlexIndex flexIndex) {
    FlexTripEdge flexEdge = getFlexEdge(flexVertex, transferStop, flexIndex);

    State state = flexEdge.traverse(accessEgress.state);
    if(state == null) return null;

    for (Edge e : transferEdges) {
      state = e.traverse(state);
      if(state == null) return null;
    }
    
    return new FlexAccessEgress(
        stop,
        getFlexTimes(flexEdge, state),
        fromStopIndex,
        toStopIndex, 
        trip,
        state,
        transferEdges.isEmpty()
    );
  }

  @Override
  public boolean equals(Object o) {
      if (o == this)
          return true;
      
      if (!(o instanceof FlexAccessEgressTemplate))
          return false;
      
      FlexAccessEgressTemplate other = (FlexAccessEgressTemplate)o;

      return serviceDate.serviceDate.equals(other.serviceDate.serviceDate) && 
    		  accessEgress.stop.getId().equals(other.accessEgress.stop.getId()) && 
    		  trip.getId().equals(other.trip.getId()) && 
    		  transferStop.getId().equals(other.transferStop.getId());
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + serviceDate.serviceDate.getAsDate().hashCode();
    result = prime * result + accessEgress.stop.getId().hashCode();
    result = prime * result + trip.getId().hashCode();
    result = prime * result + transferStop.getId().hashCode();
    return result;
  }
}
