package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to adapt the FlexAccessEgress into a time-dependent multi-leg AccessEgress.
 */
public class FlexAccessEgressAdapter extends AccessEgress implements RaptorTransfer {
  
  private static final Logger LOG = LoggerFactory.getLogger(FlexAccessEgressAdapter.class);

  private final FlexAccessEgress flexAccessEgress;

  public FlexAccessEgressAdapter(
          FlexAccessEgress flexAccessEgress, boolean isEgress, StopIndexForRaptor stopIndex
  ) {
    super(
        stopIndex.indexByStop.get(flexAccessEgress.stop),
        isEgress ? flexAccessEgress.lastState.reverse() : flexAccessEgress.lastState
    );

    this.flexAccessEgress = flexAccessEgress;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return flexAccessEgress.earliestDepartureTime(requestedDepartureTime);
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return flexAccessEgress.latestArrivalTime(requestedArrivalTime);
  }

  @Override
  public int numberOfRides() {
    // We only support one flex leg at the moment
    return 1;
  }

  @Override
  public boolean stopReachedOnBoard() {
    return flexAccessEgress.directToStop;
  }

  @Override
  public String toString() {
    return asString();
  }
}
