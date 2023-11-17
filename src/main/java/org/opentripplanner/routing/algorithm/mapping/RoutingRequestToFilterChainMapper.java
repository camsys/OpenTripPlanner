package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;

import java.time.Instant;
import java.util.function.Consumer;


import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;

public class RoutingRequestToFilterChainMapper {
  private static final int KEEP_ONE = 1;

  /** Filter itineraries down to this limit, but not below. */
  private static final int MIN_NUMBER_OF_ITINERARIES = 3;

  /** Never return more that this limit of itineraries. */
  private static final int MAX_NUMBER_OF_ITINERARIES = 200;

  public static ItineraryFilter createFilterChain(
      RoutingRequest request,
      Instant filterOnLatestDepartureTime,
      boolean removeWalkAllTheWayResults,
      Consumer<Itinerary> maxLimitReachedSubscriber
  ) {
    var builder = new ItineraryFilterChainBuilder(request.arriveBy);
    var p = request.itineraryFilters;

    // Group by similar legs filter
    if(request.itineraryFilters != null) {

      builder.withMinSafeTransferTimeFactor(p.minSafeTransferTimeFactor);

      if (p.groupSimilarityKeepOne >= 0.5) {
        builder.addGroupBySimilarity(
            new GroupBySimilarity(p.groupSimilarityKeepOne, KEEP_ONE)
        );
      }

      if (p.groupSimilarityKeepNumOfItineraries >= 0.5) {
        int minLimit = request.numItineraries;

        if (minLimit < 0 || minLimit > MIN_NUMBER_OF_ITINERARIES) {
          minLimit = MIN_NUMBER_OF_ITINERARIES;
        }

        builder.addGroupBySimilarity(
            new GroupBySimilarity(p.groupSimilarityKeepNumOfItineraries, minLimit)
        );
      }
    }

    builder
        .withMaxNumberOfItineraries(Math.min(request.numItineraries, MAX_NUMBER_OF_ITINERARIES))
        .withMinSafeTransferTimeFactor(p.minSafeTransferTimeFactor)
        .withTransitGeneralizedCostLimit(p.transitGeneralizedCostLimit)
        .withBikeRentalDistanceRatio(p.bikeRentalDistanceRatio)
        .withParkAndRideDurationRatio(p.parkAndRideDurationRatio)
        .withNonTransitGeneralizedCostLimit(p.nonTransitGeneralizedCostLimit)
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
        .withSortOrder(p.sortOrder);


    //add flex filter if at least one of the modes is FLEX
    //this is breaking up the pretty method chaining because I didn't want to change the order the filters are added
    if (FLEXIBLE.equals(request.modes.directMode) ||
            FLEXIBLE.equals(request.modes.accessMode) ||
            FLEXIBLE.equals(request.modes.egressMode)) {

      builder.withFlexFilter(request.maxWalkDistance);
    }

    //finish adding filters
//        builder.withLatestDepartureTimeLimit(filterOnLatestDepartureTime)
    builder.withMaxLimitReachedSubscriber(maxLimitReachedSubscriber)
        .withRemoveWalkAllTheWayResults(removeWalkAllTheWayResults)
        .withDebugEnabled(p.debug);

    return builder.build();
  }

  public static ItineraryFilter postFilterSort(
          RoutingRequest request
  ) {
    var builder = new ItineraryFilterChainBuilder(request.arriveBy, request.dateTime);
    builder.waitWeight = request.waitWeight;
    var p = request.itineraryFilters;
    builder.withResultsOrder(p.resultsOrder);
    return builder.sortOnly();
  }
}
