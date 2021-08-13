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

  private Date requestTime;
	
  public FlexFilter(Date requestTime) {
	  this.requestTime = requestTime;
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
		  String routeKey = String.join(",",itin.legs.stream()
				  .map(it -> it.getRoute() != null ? it.getRoute().getId().toString() : null)
				  .filter(it -> it != null)
				  .collect(Collectors.toList())
				  .toArray(new String[] {}));
				  				  
		  Leg flexLeg = itin.legs.stream()
				  .filter(it -> it.flexibleTrip == true)
				  .findFirst()
				  .orElse(null);
	
		  if(flexLeg != null) {
			  itinerariesByRoutePath.put(routeKey, itin);

		  // no flex leg, so just pass through
		  } else {
			  newList.add(itin);
		  }
	  }
	  
	  newList.addAll(itinerariesByRoutePath.values());
	  
	  // remove walking all the way
      return newList
              .stream().filter(it -> !it.isWalkingAllTheWay())
              .collect(Collectors.toList());	  
  }

  @Override
  public boolean removeItineraries() {
    return false;
  }

}
