package org.opentripplanner.routing.mta.comparison.test_file_format;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.mta.comparison.QualitativeMultiDimInstanceComparison.platformDim;

import java.util.Comparator;

public class ItinerarySummary {

	public int itineraryNumber;
	
	public double walkDistance;
	
	public int transitTime;
	
	public String routes = "";
	
	public platformDim platform;

	public Boolean approveOfResult = true;
	
	public static ItinerarySummary fromItinerary(Itinerary itin) {
		ItinerarySummary is = new ItinerarySummary();
		is.walkDistance = (itin.nonTransitDistanceMeters/1000f);
		is.transitTime = (int)((float)itin.transitTimeSeconds/60f);
		is.routes = "";
		
		for(Leg leg : itin.legs) {
			if(leg.mode.equals(TraverseMode.WALK))
				continue;
			if(!is.routes.isEmpty()) 
				is.routes += ">";
			is.routes += (leg.getRoute() != null) ? leg.getRoute().getName() : leg.getRoute();
		}
		
		return is;		
	}
	
	public static Comparator<ItinerarySummary> RANKER_TIME = new Comparator<ItinerarySummary>() {
		@Override
		public int compare(ItinerarySummary o1, ItinerarySummary o2) {
			if(o1.transitTime == o2.transitTime) {
				return 0;
			} else if(o1.transitTime < o2.transitTime) {
				return -1;
			} else {
				return 1;
			}
		}
	};
	
	public static Comparator<ItinerarySummary> RANKER_WALKING = new Comparator<ItinerarySummary>() {
		@Override
		public int compare(ItinerarySummary o1, ItinerarySummary o2) {
			if(o1.walkDistance == o2.walkDistance) {
				return 0;
			} else if(o1.walkDistance < o2.walkDistance) { 
				return -1;
			} else {
				return 1;
			}
		}
	};

	public static Comparator<ItinerarySummary> RANKER_XFERS = new Comparator<ItinerarySummary>() {
		@Override
		public int compare(ItinerarySummary o1, ItinerarySummary o2) {
			int o1x = o1.routes.split(">").length;
			int o2x = o2.routes.split(">").length;

			if(o1x == o2x) {
				return 0;
			} else if(o1x < o2x) {
				return -1;
			} else {
				return 1;
			}
		}
	};

	public static Comparator<ItinerarySummary> RANKER_EQUAL = new Comparator<ItinerarySummary>() {
		@Override
		public int compare(ItinerarySummary o1, ItinerarySummary o2) {
			//System.out.println(o1.routes + "?" + o2.routes);
			return o1.routes.compareTo(o2.routes);
		}
	};	
	
	public String toString() {
		return itineraryNumber + ": Walk=" + walkDistance + ", transit=" + transitTime + ", Routes = " + routes;
	}
	
	private String normalize(String s) {
	    StringBuilder sb = new StringBuilder(s);
	    while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '>') {
	        sb.setLength(sb.length() - 1);
	    }
	    
	    String r = sb.toString().toUpperCase();
	    
	    // assume LTD routes are always equivalent to their non limited counterparts
	    r = r.replaceAll("-LTD", "");
	    
	    // remove leading zeros in route names, e.g. Q07 -> Q7
	    r = r.replaceAll("^([A-Za-z])0+(?!$)", "$1");	    		
	    
	    return r;
	}
	
	public ItinerarySummary() {}
	
	public ItinerarySummary(String line) throws Exception {
		String parts[] = line.split(" ");
		
		if(parts.length < 4 && parts[0].equals("S"))
			throw new Exception("Nope.");
		
		itineraryNumber = Integer.parseInt(parts[1].trim());
		walkDistance = Double.parseDouble(parts[2].trim());
		transitTime = (int)Double.parseDouble(parts[3].trim());

		// some results have no routes
		if(parts.length > 4) {
			// case: approve/disapprove no routes
			if(parts.length == 5) {
				if(parts[4].trim().equals("A") || parts[4].trim().equals("D"))
					approveOfResult = new Boolean(parts[4].trim().equals("A"));
				else
					routes = normalize(parts[4].trim());

			// both approve/disapprove and routes
			} else {
				routes = normalize(parts[4].trim());

				if(parts[5].trim().equals("A") || parts[5].trim().equals("D"))
					approveOfResult = new Boolean(parts[5].trim().equals("A"));
			}
		}
	}
}