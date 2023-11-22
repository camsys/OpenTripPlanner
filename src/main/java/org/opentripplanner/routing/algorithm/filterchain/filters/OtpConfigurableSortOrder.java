package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.util.CompositeComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Configure the precedence of statically defined sorting elements.
 */
public class OtpConfigurableSortOrder extends OtpDefaultSortOrder {


  /**
   * This comparator will sort on wait time plus generalized cost from start of trip
   */
  public static final Comparator<Itinerary> DEPART_AT_WAIT_TIME_AND_GENERALIZED_COST = Comparator.comparingDouble(
          a -> ((
                  (a.startTime().getTimeInMillis()/1000f) - a.getDateTime())
                  * a.getWaitWeight()
          ) + a.generalizedCost
  );

  /**
   * This comparator will sort on wait time plus generalized cost from end of trip
   */
  public static final Comparator<Itinerary> ARRIVE_BY_WAIT_TIME_AND_GENERALIZED_COST = Comparator.comparingDouble(
          a -> ((
                  a.getDateTime() - (a.endTime().getTimeInMillis()/1000f))
                  * a.getWaitWeight()
          ) + a.generalizedCost
  );

  public static final Comparator<Itinerary> DEPART_AT_WAIT_TIME_AND_GENERALIZED_COST_AND_WALKING =
          Comparator.comparingDouble(
                  a -> ((
                          (a.startTime().getTimeInMillis()/1000f) - a.getDateTime())
                          * a.getWaitWeight()
                          + ((a.nonTransitDistanceMeters / a.getWalkingSpeed()) * a.getWalkingWeight())
                  ) + a.generalizedCost
          );

  public static final Comparator<Itinerary> ARRIVE_BY_WAIT_TIME_AND_GENERALIZED_COST_AND_WALKING =
          Comparator.comparingDouble(
                  a -> ((
                          a.getDateTime() - (a.endTime().getTimeInMillis()/1000f))
                          * a.getWaitWeight()
                          + ((a.nonTransitDistanceMeters / a.getWalkingSpeed()) * a.getWalkingWeight())
                  ) + a.generalizedCost
          );

  public static final Comparator<Itinerary> DEPART_AT_WAIT_TIME_AND_GENERALIZED_COST_AND_TRANSFERS =
          Comparator.comparingDouble(
                  a -> ((
                          (a.startTime().getTimeInMillis()/1000f) - a.getDateTime())
                          * a.getWaitWeight()
                          + (a.nTransfers * a.getTransferWeight())
                  ) + a.generalizedCost
          );

  public static final Comparator<Itinerary> ARRIVE_BY_WAIT_TIME_AND_GENERALIZED_COST_AND_TRANSFERS =
          Comparator.comparingDouble(
                  a -> ((
                          a.getDateTime() - (a.endTime().getTimeInMillis()/1000f))
                          * a.getWaitWeight()
                          + (a.nTransfers * a.getTransferWeight())
                  ) + a.generalizedCost
          );

  public OtpConfigurableSortOrder(boolean arriveBy, String defaultSortOrder) {
    super(createComparator(arriveBy, defaultSortOrder));
  }

  private static Comparator<Itinerary> createComparator(boolean arriveBy, String defaultSortOrder) {
    List<Comparator<Itinerary>> chain = new ArrayList<>();
    int adCount = 0;

    if ("default".equalsIgnoreCase(defaultSortOrder)) {
      // if the sortOrder isn't specified in configuration use:
      //ARRIVAL_DEPARTURE_TIME,GENERALIZED_COST,NUM_OF_TRANSFERS,ARRIVAL_DEPARTURE_TIME
      if (arriveBy) {
        chain.add(DEPARTURE_TIME);
      } else {
        chain.add(ARRIVAL_TIME);
      }
      chain.add(GENERALIZED_COST);
      chain.add(NUM_OF_TRANSFERS);
      if (arriveBy) {
        chain.add(ARRIVAL_TIME);
      } else {
        chain.add(DEPARTURE_TIME);
      }
      return new CompositeComparator<>(chain.toArray(new Comparator[0]));
    }

    String[] sortDirectives = defaultSortOrder.split(",");
    for (String sortDirective : sortDirectives) {
      if (sortDirective == null) continue;
      sortDirective = sortDirective.trim();
      if (sortDirective == null || sortDirective.length() == 0) continue;
      if (sortDirective.equals("WAIT_TIME_AND_GENERALIZED_COST")) {
        if (arriveBy) {
          chain.add(ARRIVE_BY_WAIT_TIME_AND_GENERALIZED_COST);
        } else {
          chain.add(DEPART_AT_WAIT_TIME_AND_GENERALIZED_COST);
        }
      } else if (sortDirective.equals("WAIT_TIME_AND_GENERALIZED_COST_AND_WALKING")) {
        if (arriveBy) {
          chain.add(ARRIVE_BY_WAIT_TIME_AND_GENERALIZED_COST_AND_WALKING);
        } else {
          chain.add(DEPART_AT_WAIT_TIME_AND_GENERALIZED_COST_AND_WALKING);
        }
      } else if (sortDirective.equals("WAIT_TIME_AND_GENERALIZED_COST_AND_TRANSFERS")) {
        if (arriveBy) {
          chain.add(ARRIVE_BY_WAIT_TIME_AND_GENERALIZED_COST_AND_TRANSFERS);
        } else {
          chain.add(DEPART_AT_WAIT_TIME_AND_GENERALIZED_COST_AND_TRANSFERS);
        }
      } else if (sortDirective.equals("STREET_ONLY")) {
        chain.add(STREET_ONLY_FIRST);
      } else if (sortDirective.equals("ARRIVAL_TIME")) {
        chain.add(ARRIVAL_TIME);
      } else if (sortDirective.equals("DEPARTURE_TIME")) {
        chain.add(DEPARTURE_TIME);
      } else if (sortDirective.equals("ARRIVAL_DEPARTURE_TIME")) {
        adCount++;
        boolean iterationArriveByNegation = (adCount % 2 == 0);
        boolean iterationArriveBy = arriveBy ^ iterationArriveByNegation;
        if (iterationArriveBy) {
          chain.add(DEPARTURE_TIME);
        } else {
          chain.add(ARRIVAL_TIME);
        }
      } else if (sortDirective.equals("GENERALIZED_COST")) {
        chain.add(GENERALIZED_COST);
      } else if (sortDirective.equals("NUM_OF_TRANSFERS")) {
        chain.add(NUM_OF_TRANSFERS);
      } else {
        throw new IllegalStateException("Unsupported sort order "
        + sortDirective + " in configuration " + Arrays.asList(sortDirectives));
      }
    }
    if (chain.isEmpty()) {
      throw new IllegalStateException("at least one sort order required");
    }
    return new CompositeComparator<>(chain.toArray(new Comparator[0]));
  }
}
