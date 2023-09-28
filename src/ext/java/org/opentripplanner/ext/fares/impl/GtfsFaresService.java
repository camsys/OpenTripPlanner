package org.opentripplanner.ext.fares.impl;

import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.fares.FareService;

public final class GtfsFaresService
        implements FareService {
  private final DefaultFareServiceImpl faresV1;
  private final GtfsFaresV2Service faresV2;

  GtfsFaresService(DefaultFareServiceImpl faresV1, GtfsFaresV2Service faresV2) {
    this.faresV1 = faresV1;
    this.faresV2 = faresV2;
  }

  public DefaultFareServiceImpl faresV1() {
    return faresV1;
  }

  public GtfsFaresV2Service faresV2() {
    return faresV2;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (GtfsFaresService) obj;
    return Objects.equals(this.faresV1, that.faresV1) &&
            Objects.equals(this.faresV2, that.faresV2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(faresV1, faresV2);
  }

  @Override
  public String toString() {
    return "GtfsFaresService[" +
            "faresV1=" + faresV1 + ", " +
            "faresV2=" + faresV2 + ']';
  }

  @Override
  public ItineraryFares getCost(Itinerary itinerary) {
    var fare = Objects.requireNonNullElse(faresV1.getCost(itinerary), ItineraryFares.empty());
    var products = faresV2.getProducts(itinerary);
    fare.addItineraryProducts(products.itineraryProducts);
    if (products.itineraryProducts.isEmpty()) {
      fare.addLegProducts(products.legProducts);
    }
    return fare;
  }
}
