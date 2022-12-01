package org.opentripplanner.routing.mta.comparison.test_file_format;

import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Query {
	
	public long time;
	
	public boolean accessible;

	public String origin;
	
	public String destination;
	
	public String optimizeFlag;
	
	public Query(String line) throws Exception {
		String parts[] = line.split(" ");
		
		if(parts.length != 6 && parts[0].equals("Q"))
			throw new Exception("Nope.");

		accessible = parts[1].trim().equals("Y");
		time = Long.parseLong(parts[2].trim());
		origin = parts[3].trim();
		destination = parts[4].trim();
		optimizeFlag = parts[5].trim();
	}

    @Override
    public boolean equals(Object o) {
    	return this.hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        return (int)(time * 31) * origin.hashCode() 
        		* destination.hashCode() + 
        		optimizeFlag.hashCode() + 
        		(accessible ? 3 : 0);
    }
        
    public String toQueryString() throws Exception {
		String stop1 = origin;
		String stop2 = destination;
		
		String originLat = stop1.split(",")[0].trim();
		String originLon = stop1.split(",")[1].trim();
	
		String destLat = stop2.split(",")[0].trim();
		String destLon = stop2.split(",")[1].trim();
		    		
		// Make request of OTP
		URIBuilder builder = new URIBuilder();
		builder.setParameter("fromPlace", originLat + "," + originLon);
		builder.setParameter("toPlace", destLat + "," + destLon);
		builder.setParameter("wheelchair", accessible + "");
		
		DateTimeFormatter dateF = DateTimeFormat.forPattern("MM-dd-YYYY");
		DateTimeFormatter timeF = DateTimeFormat.forPattern("hh:mm aa");
		builder.setParameter("date", new DateTime(time).toString(dateF));
		builder.setParameter("time", new DateTime(time).toString(timeF));

		builder.setParameter("mode", "TRANSIT,WALK");
		builder.setParameter("maxWalkDistance", "500");
		builder.setParameter("ignoreRealtimeUpdates", "true");
		switch(optimizeFlag) {
			case "W":
				builder.setParameter("optimize",  "WALKING");
				break;
			case "X":
				builder.setParameter("optimize",  "TRANSFERS");
				break;
			case "T":
				builder.setParameter("optimize",  "QUICK");
				break;
		}

		return builder.build().getQuery();
    }
    
    public String toString() { // TODO: make into BASELINE URLs
    	return origin + " -> " + destination;
    }
}