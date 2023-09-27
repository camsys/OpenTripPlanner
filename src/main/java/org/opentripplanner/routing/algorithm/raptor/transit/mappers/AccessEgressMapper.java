package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.FlexAccessEgressAdapter;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccessEgressMapper {

  private static Logger LOG = LoggerFactory.getLogger(AccessEgressMapper.class);

  private final StopIndexForRaptor stopIndex;

  public AccessEgressMapper(StopIndexForRaptor stopIndex) {
    this.stopIndex = stopIndex;
  }

  public AccessEgress mapNearbyStop(NearbyStop nearbyStop, boolean isEgress) {
      if (!(nearbyStop.stop instanceof StopLocation)) {
        return null;
      }
      Integer index = stopIndex.indexByStop.get(nearbyStop.stop);
      if(index == null){
        LOG.info("Nearby Stop {} not found in stopIndex map", nearbyStop.stop.getId());
        return null;
      }
      return new AccessEgress(index, isEgress ? nearbyStop.state.reverse() : nearbyStop.state);
  }

  public List<AccessEgress> mapNearbyStops(Collection<NearbyStop> accessStops, boolean isEgress) {
    return accessStops
        .stream()
        .map(stopAtDistance -> mapNearbyStop(stopAtDistance, isEgress))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public Collection<AccessEgress> mapFlexAccessEgresses(
          Collection<FlexAccessEgress> flexAccessEgresses,
          boolean isEgress
  ) {

    return flexAccessEgresses.stream()
        .map(flexAccessEgress -> new FlexAccessEgressAdapter(flexAccessEgress, isEgress, stopIndex))
        .collect(Collectors.toList());
  }

}
