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
	  
	  // filter out any trips that jump onto fixed route for short periods 
	  for(Entry<String, Itinerary> entry : itinerariesByRoutePath.entrySet()) {
		  Itinerary itin = entry.getValue();
		  
		  Leg[] legs = itin.legs
				  .stream()
				  .filter(it -> !it.isWalkingLeg())
				  .collect(Collectors.toList())
				  .toArray(new Leg[] {});
		  		  
		  // can't fit our criteria with < 3 items
		  if(legs.length < 2) {
			  newList.add(itin);
			  continue;
		  }

		  boolean skip = false;
		  for(int i = 1; i < legs.length - 1; i++) {
			  Leg prevLeg = legs[i - 1];
			  Leg thisLeg = legs[i];
			  Leg nextLeg = legs[i + 1];

			  // skip flex legs right after one another
			  if(thisLeg.flexibleTrip) {
				  if(prevLeg.flexibleTrip || nextLeg.flexibleTrip) {
					  skip = true;
				  	  break;
				  }
			  }

			  // skip flex legs right short transit legs right before/after
			  if(thisLeg.flexibleTrip) {
				  if(!prevLeg.flexibleTrip && nextLeg.isTransitLeg()) {
					  if(prevLeg.getDuration() < 60 * 5 * 1000) {
						  skip = true;
						  break;
					  }				  

				  } else if(!nextLeg.flexibleTrip && nextLeg.isTransitLeg()) {
					  if(nextLeg.getDuration() < 60 * 5 * 1000) {
						  skip = true;
						  break;
					  }				  
				  }
			  }
			  		  
			  if(!skip) 
				  newList.add(itin);
		  }
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
