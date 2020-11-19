package org.opentripplanner.routing.mta.comparison.test_file_format;

import java.util.Comparator;

import org.opentripplanner.routing.mta.comparison.QualitativeMultiDimInstanceComparison.platformDim;

public class ItinerarySummary {

	public int itineraryNumber;
	
	public double walkDistance;
	
	public int transitTime;
	
	public String routes = "";
	
	public platformDim platform;

	public Boolean approveOfResult = true;
	
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
			return o1.routes.compareTo(o2.routes);
		}
	};	
	
	public String toString() {
		return itineraryNumber + ": Walk=" + walkDistance + ", transit=" + transitTime + ", Routes = " + routes;
	}
	
	private String removeTrailingTransfer(String s) {
	    StringBuilder sb = new StringBuilder(s);
	    while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '>') {
	        sb.setLength(sb.length() - 1);
	    }
	    return sb.toString();
	}
	
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
					routes = removeTrailingTransfer(parts[4].trim());

			// both approve/disapprove and routes
			} else {
				routes = removeTrailingTransfer(parts[4].trim());

				if(parts[5].trim().equals("A") || parts[5].trim().equals("D"))
					approveOfResult = new Boolean(parts[5].trim().equals("A"));
			}
		}
	}
}