package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class FlexFilter implements ItineraryFilter {
	
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
				  				  
		  Leg flexLeg = itin.legs.stream()
				  .filter(it -> it.flexibleTrip == true)
				  .findFirst()
				  .orElse(null);
	
		  // no flex legs, so just pass through
		  if(flexLeg == null) {
			  newList.add(itin);
			  continue;
		  }

		  // put this one in the map if it's the only one 
		  Itinerary existingItin = itinerariesByRoutePath.get(routePathKey);
		  
		  if(existingItin == null) {
			  itinerariesByRoutePath.put(routePathKey, itin);
			  continue;
		  }
		  
		  // ...or if it leaves before what's there
		  Leg existingItinFlexLeg = existingItin.legs.stream()
				  .filter(it -> it.flexibleTrip == true)
				  .findFirst()
				  .orElse(null);

		  if(flexLeg.startTime.before(existingItinFlexLeg.startTime)) {
			  itinerariesByRoutePath.put(routePathKey, itin);
		  	  continue;
	  	  }
	  }
	  
	  newList.addAll(itinerariesByRoutePath.values());
	  
	  // remove walking all the way options
      return newList
              .stream().filter(it -> !it.isWalkingAllTheWay())
              .collect(Collectors.toList());	 
  }

  @Override
  public boolean removeItineraries() {
    return false;
  }

}
