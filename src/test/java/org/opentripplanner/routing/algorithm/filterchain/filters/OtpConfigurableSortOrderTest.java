package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

/**
 * Confirm Default OtpConfigurableSortOrder performs equivalent to
 * OtpDefaultSortOrder,
 * then confirm different configuration perform as expected.
 */
public class OtpConfigurableSortOrderTest extends SortOrderTest {

  private static final String DEFAULT_SORT_ORDER = "STREET_ONLY,ARRIVAL_DEPARTURE_TIME,GENERALIZED_COST,NUM_OF_TRANSFERS,ARRIVAL_DEPARTURE_TIME";
  private static final String PREFERRED_SORT_ORDER = "STREET_ONLY,ARRIVAL_DEPARTURE_TIME,NUM_OF_TRANSFERS,GENERALIZED_COST,ARRIVAL_DEPARTURE_TIME";

  protected SortFilter arriveBySort() {
    return new OtpConfigurableSortOrder(true, DEFAULT_SORT_ORDER);
  }
  protected SortFilter arriveByPreferredSort() {
    return new OtpConfigurableSortOrder(true, PREFERRED_SORT_ORDER);
  }
  protected SortFilter departAfterSort() {
    return new OtpConfigurableSortOrder(false, DEFAULT_SORT_ORDER);
  }
  protected SortFilter departAfterPreferredSort() {
    return new OtpConfigurableSortOrder(false, PREFERRED_SORT_ORDER);
  }

  @Test
  public void sortStreetBeforeTransitThenTime() {
    runSortStreetBeforeTransitThenTime();
  }
  @Test
  public void sortOnTime() {
    runSortOnTime();
  }
  @Test
  public void sortOnGeneralizedCostVsTime() {
    runSortOnGeneralizedCostVsTime();
  }
  @Test
  public void sortOnGeneralizedCostVsNumberOfTransfers() {
    runSortOnGeneralizedCostVsNumberOfTransfers();
  }

  @Test
  public void sortOnTransfersVsTime() {
    runSortOnTransfersVsTime();
  }
  @Test
  public void runMultiSort() {
    int streetOnlyCost = 3400;
    Itinerary streetOnly = newItinerary(A).bicycle(0, 0+streetOnlyCost, G).build();
    streetOnly.generalizedCost = streetOnlyCost;

    // Same cost, more transfers (2 transfers)
    Itinerary shortTripManyTransfers = newItinerary(B)
            .subway(11, 0, 1, C)
            .subway(21, 2, 3, D)
            .subway(31, 4, 5, G)
            .build();
    shortTripManyTransfers.generalizedCost = 5;

    int shortTripFarAwayCost = 1;
    Itinerary shortTripFarAway = newItinerary(C).rail(21, 200000, 200000+shortTripFarAwayCost, G).build();
    shortTripFarAway.generalizedCost = shortTripFarAwayCost;

    int longerTripAfterShortCost = 10;
    Itinerary longerTripAfterShort = newItinerary(D).bus(41, 122000, 122000+longerTripAfterShortCost, G).build();
    longerTripAfterShort.generalizedCost = longerTripAfterShortCost;

    assertEquals(toStr(streetOnly,shortTripManyTransfers,longerTripAfterShort,shortTripFarAway),
            toStr(departAfterPreferredSort().filter(List.of(streetOnly, longerTripAfterShort, shortTripManyTransfers, shortTripFarAway))));
  }

}
