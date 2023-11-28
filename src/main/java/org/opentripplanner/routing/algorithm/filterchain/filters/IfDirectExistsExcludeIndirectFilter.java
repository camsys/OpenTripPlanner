package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IfDirectExistsExcludeIndirectFilter implements ItineraryFilter {
    @Override
    public String name() {
        return null;
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        Set<String> excludeCriteriaTrips = getDirectTrips(itineraries);
        return itineraries.stream().filter(itn->{
            if(itnHasOneLeg(itn)){return true;}
            boolean hasExcludedTrip = itn.legs.stream().anyMatch(
                    leg-> excludeCriteriaTrips.contains(getLegId(leg)));
            return !hasExcludedTrip;}
            ).collect(Collectors.toList());
    }

    public Set<String> getDirectTrips(List<Itinerary> itineraries){
        Set<String> excludeCriteriaTrips = new HashSet<>();
        itineraries.stream().forEach(itn->{
                if(itnHasOneLeg(itn)){
                    excludeCriteriaTrips.add(getLegId(itn.legs.get(0)));
                }});
        excludeCriteriaTrips.remove(null);
        return excludeCriteriaTrips;
    }

    public String getLegId(Leg leg){
        if(leg.getTrip()!=null){
            return leg.getTrip().getId().toString();
        }
        return null;
    }

    public boolean itnHasOneLeg(Itinerary itn){
        return itn.legs.size()==1;
    }

    @Override
    public boolean removeItineraries() {
        return false;
    }
}
