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
	  return itineraries;
  
  }

  @Override
  public boolean removeItineraries() {
    return false;
  }

}
