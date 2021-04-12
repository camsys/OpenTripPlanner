package org.opentripplanner.routing.mta.comparison.test_file_format;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.opentripplanner.routing.mta.comparison.QualitativeMultiDimInstanceComparison;

public class Result {

    public Query query;

    public List<ItinerarySummary> itineraries = new ArrayList<ItinerarySummary>();

	public static List<Result> loadResults(File resultsFile) throws Exception {
		return loadResults(resultsFile, null);
	}
	
	public static List<Result> loadResults(File resultsFile, QualitativeMultiDimInstanceComparison.platformDim platformTag) throws Exception {
    	List<Result> results = new ArrayList<Result>();

    	Scanner resultsReader = new Scanner(resultsFile);

		Query q = null;
		Result r = null;
    	while (resultsReader.hasNextLine()) {
    		String line = resultsReader.nextLine();

    		if(line.startsWith("Q")) {
    	    	if(r != null) {
    				results.add(r);
    	    		r = null;
    	    	}
    			
    			q = new Query(line);
    			r = new Result();    			
    			r.query = q;
    		}

    		if(line.startsWith("S")) {
    			ItinerarySummary s = new ItinerarySummary(line);
    			s.platform = platformTag;
    			r.itineraries.add(s);
    		}
    	}
    	
    	if(r != null) {
			results.add(r);
			r = null;
    	}
    	
    	resultsReader.close();
    	
    	return results;
	}
}