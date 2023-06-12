package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

/**
 * This class is used to adapt the FlexAccessEgress into a time-dependent multi-leg AccessEgress.
 */
public class FlexAccessEgressAdapter extends AccessEgress implements RaptorTransfer {
  
  private static final Logger LOG = LoggerFactory.getLogger(FlexAccessEgressAdapter.class);

  private final FlexAccessEgress flexAccessEgress;

  private final boolean allowPickupDropoff;

  public FlexAccessEgressAdapter(
          FlexAccessEgress flexAccessEgress, FlexIndex flexIndex, boolean isEgress, StopIndexForRaptor stopIndex
  ) {
    super(
        stopIndex.indexByStop.get(flexAccessEgress.stop),
        isEgress ? flexAccessEgress.lastState.reverse() : flexAccessEgress.lastState
    );

    FlexTrip trip = flexAccessEgress.trip;

    boolean allowPickup = flexIndex.hasStopThatAllowsPickup(trip,trip.getStopTime(flexAccessEgress.fromStopIndex).stop);
    boolean allowDropoff = flexIndex.hasStopThatAllowsDropoff(trip,trip.getStopTime(flexAccessEgress.toStopIndex).stop);
    this.allowPickupDropoff = allowDropoff && allowPickup;


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
  public boolean allowPickupDropoff() {
    return allowPickupDropoff;
  }

  @Override
  public String toString() {
    return asString();
  }
}
