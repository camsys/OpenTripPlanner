package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;

/**
 * This is an adapter that gives the RAPTOR algorithm access to the data
 * structures used here. 
 *
 */
public class FlexAccessEgress {
  /**
   * (description borrowed from AcessEgress)
   * "To stop" in the case of access, "from stop" in the case of egress.
   */
  public final Stop stop;
  public final int[] flexTimes; // pre, flex, post
  public final int fromStopIndex;
  public final int toStopIndex;
  public final FlexTrip trip;
  public final State lastState;
  public final boolean directToStop;

  public FlexAccessEgress(
      Stop stop, // used by FlexAccessEgressAdapter
      int[] flexTimes, // pre, flex, post
      int fromStopIndex,
      int toStopIndex,
      FlexTrip trip,
      State lastState,
      boolean directToStop // used by FlexAccessEgressAdapter
  ) {
    this.stop = stop;
    this.flexTimes = flexTimes;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.trip = trip;
    this.lastState = lastState;
    this.directToStop = directToStop;
  }

  public double getSafeTotalTime() {
	  State s = this.lastState;
	  Edge e = this.lastState.backEdge;
	  while(s != null && !(e instanceof FlexTripEdge)) {
		  e = s.backEdge;
		  s = s.getBackState();
	  }
	  
	  return this.trip.getSafeTotalTime(((FlexTripEdge)e).getFlexPath(), this.fromStopIndex, this.toStopIndex);
  }
  
  public State getAccessEgressState() {
	  return this.lastState;
  }
  
  public int earliestDepartureTime(int departureTime) {
	int requestedTransitDepartureTime = departureTime + flexTimes[0]; 
    int earliestAvailableTransitDepartureTime = trip.earliestDepartureTime(
        requestedTransitDepartureTime,
        fromStopIndex,
        toStopIndex
    );
    if (earliestAvailableTransitDepartureTime == -1) { return -1; }
    return earliestAvailableTransitDepartureTime - flexTimes[0]; 
  }

  public int latestArrivalTime(int arrivalTime) {
    int requestedTransitArrivalTime = arrivalTime - flexTimes[2];
    int latestAvailableTransitArrivalTime = trip.latestArrivalTime(
        requestedTransitArrivalTime,
        fromStopIndex,
        toStopIndex
    );
    if (latestAvailableTransitArrivalTime == -1) { return -1; }
    return latestAvailableTransitArrivalTime + flexTimes[2];
  }

  @Override
  public boolean equals(Object o) {
      if (o == this)
          return true;
      
      if (!(o instanceof FlexAccessEgress))
          return false;
      
      FlexAccessEgress other = (FlexAccessEgress)o;

      return trip.getId().equals(other.trip.getId()) && 
      		stop.getId().equals(other.stop.getId());
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + trip.getId().hashCode();
    result = prime * result + stop.getId().hashCode();
    return result;
  }

  @Override
  public String toString(){
    String out = stop.toString();
    out+=flexTimes.toString();
    out+=String.valueOf(fromStopIndex);
    out+=String.valueOf(toStopIndex);
    out+=trip.toString();
    out+=lastState.toStringVerbose();
    out+=String.valueOf(directToStop);
    return out;
  }
  
}
