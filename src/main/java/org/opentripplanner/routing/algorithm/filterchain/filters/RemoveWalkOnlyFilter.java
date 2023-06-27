package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter itineraries and remove all itineraries where all legs is walking.
 */
public class RemoveWalkOnlyFilter implements ItineraryFilter {

    private final boolean preventEmptyItineraries;

    public RemoveWalkOnlyFilter() {
        this.preventEmptyItineraries = false;
    }

    public RemoveWalkOnlyFilter(boolean preventEmptyItineraries) {
        this.preventEmptyItineraries = preventEmptyItineraries;
    }

    @Override
    public String name() {
        return "remove-walk-only-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        List<Itinerary> filteredItineraries =  itineraries
                                                .stream().filter(it -> !it.isWalkingAllTheWay())
                                                .collect(Collectors.toList());
        if(preventEmptyItineraries && filteredItineraries.size() == 0){
            return itineraries;
        }
        return filteredItineraries;
    }

    @Override
    public boolean removeItineraries() {
        return true;
    }
}
