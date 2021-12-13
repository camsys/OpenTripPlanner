package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class FlexFilter implements ItineraryFilter {
	
  private Double maxWalkDistance = null;
	
  public FlexFilter(Double maxWalkDistance) {
	  this.maxWalkDistance = maxWalkDistance;
  }
	
  @Override
  public String name() {
    return "flex-filter";
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {

	  Set<Itinerary> newList = new HashSet<>();
	  
	  HashMap<String, Itinerary> itinerariesByRoutePath = new HashMap<>();

	  for(Itinerary itin : itineraries) {
		  String routePathKey = String.join(",",itin.legs.stream()
				  .map(it -> it.getRoute() != null ? it.getRoute().getId().toString() : null)
				  .filter(it -> it != null)
				  .collect(Collectors.toList())
				  .toArray(new String[] {}));

		  List<Leg> walkingLegs = itin.legs.stream()
				  .filter(it -> it.isWalkingLeg())
				  .collect(Collectors.toList());

		  double totalWalk = 0;
		  for(Leg l : walkingLegs) {
			  totalWalk += l.distanceMeters;
		  }

		  if(maxWalkDistance != null && totalWalk > maxWalkDistance)
			  continue;
		  
		  Leg flexLeg = itin.legs.stream()
				  .filter(it -> it.flexibleTrip == true)
				  .findFirst()
				  .orElse(null);
	
		  // no flex legs? just pass through
		  if(flexLeg == null) {
			  newList.add(itin);
			  continue;
		  }

		  Itinerary existingItin = itinerariesByRoutePath.get(routePathKey);
		  
		  // put this one in the map if it's the only one 
		  if(existingItin == null) {
			  itinerariesByRoutePath.put(routePathKey, itin);
			  continue;
		  }
		  
		  if(itin.generalizedCost < existingItin.generalizedCost) {
			  itinerariesByRoutePath.put(routePathKey, itin);
		  	  continue;
	  	  }
	  }	 
	  
	  // remove walking all the way options
      return itinerariesByRoutePath.values()
              .stream()
              .filter(it -> !it.isWalkingAllTheWay())
              .collect(Collectors.toList());            
  }

  @Override
  public boolean removeItineraries() {
    return false;
  }

}
