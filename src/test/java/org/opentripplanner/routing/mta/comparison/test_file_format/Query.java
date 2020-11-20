package org.opentripplanner.routing.mta.comparison.test_file_format;

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
    
    public String toString() { // TODO: make into BASELINE URLs
    	return origin + " -> " + destination;
    }
}