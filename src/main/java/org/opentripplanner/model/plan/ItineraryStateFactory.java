package org.opentripplanner.model.plan;

import org.opentripplanner.routing.api.request.RoutingRequest;

/**
 * Factory for ItineraryState.
 */
public class ItineraryStateFactory {

  public ItineraryState getFromRequest(RoutingRequest req) {
    return new ItineraryState(req.dateTime,
            req.waitWeight,
            req.transferWeight,
            req.walkingWeight,
            (float)req.walkSpeed);
  }

  public ItineraryState getForTest() {
    return new ItineraryState(0,
            1.5f,
            1.5f,
            1.5f,
            1.3f);
  }


}
