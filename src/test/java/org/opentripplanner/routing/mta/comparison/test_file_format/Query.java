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
		
		if(parts.length < 5 && parts[0].equals("Q")) {
			throw new Exception("unexpected line format=" + line);
		}


		accessible = parts[1].trim().equals("Y");
		time = Long.parseLong(parts[2].trim());
		origin = parts[3].trim();
		destination = parts[4].trim();
		// we silently ignore optimize flag now -- it is no longer supported
	}

    @Override
    public boolean equals(Object o) {
    	return this.hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        return (int)(time * 31) * origin.hashCode() 
        		* destination.hashCode() + 
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
		// TODO!!!! with optimze flag removed, we need to SORT based on this instead
		return builder.build().getQuery();
    }
    
    public String toString() { // TODO: make into BASELINE URLs
    	return origin + " -> " + destination;
    }
}