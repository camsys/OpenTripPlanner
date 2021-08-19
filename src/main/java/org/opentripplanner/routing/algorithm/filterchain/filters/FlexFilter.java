package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
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
		  
		  // ...or if it leaves before what's there now
		  Leg existingItinFlexLeg = existingItin.legs.stream()
				  .filter(it -> it.flexibleTrip == true)
				  .findFirst()
				  .orElse(null);

		  if(flexLeg.startTime.before(existingItinFlexLeg.startTime)) {
			  itinerariesByRoutePath.put(routePathKey, itin);
		  	  continue;
	  	  }
	  }
	  
	  // filter out any trips that jump onto fixed route between two flex trips
	  for(Entry<String, Itinerary> entry : itinerariesByRoutePath.entrySet()) {
		  Itinerary itin = entry.getValue();
		  
		  Leg[] legs = itin.legs
				  .stream()
				  .filter(it -> !it.isWalkingLeg())
				  .collect(Collectors.toList())
				  .toArray(new Leg[] {});
		  		  
		  // can't fit our criteria with < 3 items
		  if(legs.length < 3) {
			  newList.add(itin);
			  continue;
		  }

		  boolean skip = false;
		  for(int i = 1; i < legs.length - 1; i++) {
			  Leg first = legs[i - 1];
			  Leg second = legs[i];
			  Leg third = legs[i + 1];
			  
			  if(first.flexibleTrip && !second.flexibleTrip && third.flexibleTrip) {
				  if(second.getDuration() < 60 * 5 * 1000) {
					  skip = true;
					  break;
				  }				  
			  }
		  }
		  
		  if(!skip) 
			  newList.add(itin);
	  }
	  	  
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
