package org.opentripplanner.ext.fares;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.fares.FareService;

public final class FaresFilter implements ItineraryFilter {
    private final FareService fareService;

    public FaresFilter(FareService fareService) {
        this.fareService = fareService;
    }

    public FareService fareService() {
        return fareService;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FaresFilter) obj;
        return Objects.equals(this.fareService, that.fareService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fareService);
    }

    @Override
    public String toString() {
        return "FaresFilter[" +
                "fareService=" + fareService + ']';
    }

    @Override
    public String name() {
        return "Fares-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        return itineraries
                .stream()
                .peek(i -> {
                    var fare = fareService.getCost(i);
                    if (Objects.nonNull(fare)) {
                        i.setFare(fare);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean removeItineraries() {
        return false;
    }


}
