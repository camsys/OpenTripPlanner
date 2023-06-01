package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filter itineraries based on generalizedCost, compared with a on-street-all-the-way itinerary(if
 * it exist). If an itinerary is slower than the best all-the-way-on-street itinerary, then the
 * transit itinerary is removed.
 */
public class RemoveTransitIfStreetOnlyIsBetterFilter implements ItineraryFilter {

    public int streetOnlyGenCostBuffer = 0;

    public RemoveTransitIfStreetOnlyIsBetterFilter setStreetOnlyGenCostBuffer(int buffer){
        streetOnlyGenCostBuffer=buffer;
        return this;
    }

    @Override
    public String name() {
        return "transit-vs-street-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        // Find the best walk-all-the-way option
        Optional<Itinerary> bestStreetOp = itineraries.stream()
            .filter(Itinerary::isOnStreetAllTheWay)
            .min(Comparator.comparingInt(l -> l.generalizedCost));

        if(bestStreetOp.isEmpty()) {
            return itineraries;
        }

//        a mild bump to the limit to prevent situations where short trips with a transfer
//        walking to the bus, are a "worse choice" than just walking
        final long limit = bestStreetOp.get().generalizedCost+streetOnlyGenCostBuffer;

        // Filter away itineraries that have higher cost than the best non-transit option.
        return itineraries.stream()
                .filter( it -> it.isOnStreetAllTheWay() || it.generalizedCost < limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean removeItineraries() {
        return true;
    }
}
