package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.util.CompositeComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Configure the precendence of statically defined sorting elements.
 */
public class OtpConfigurableSortOrder extends OtpDefaultSortOrder {
  public OtpConfigurableSortOrder(boolean arriveBy, String defaultSortOrder) {
    super(createComparator(arriveBy, defaultSortOrder));
  }

  private static Comparator<Itinerary> createComparator(boolean arriveBy, String defaultSortOrder) {
    List<Comparator<Itinerary>> chain = new ArrayList<>();
    int adCount = 0;
    String[] sortDirectives = defaultSortOrder.split(",");
    for (String sortDirective : sortDirectives) {
      if (sortDirective == null) continue;
      sortDirective = sortDirective.trim();
      if (sortDirective == null || sortDirective.length() == 0) continue;
      if (sortDirective.equals("STREET_ONLY")) {
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
        + sortDirective + " in configuration " + sortDirectives);
      }
    }
    if (chain.isEmpty()) {
      throw new IllegalStateException("at least one sort order required");
    }
    return new CompositeComparator<>(chain.toArray(new Comparator[0]));
  }
}
