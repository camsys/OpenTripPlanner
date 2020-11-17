/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.mta.comparison;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class GenerateTestODPairsFromTestGTFS {
	
    private static final String NYCT_SUBWAYS_GTFS = "src/test/resources/mta/nyct_subways_gtfs.zip";

    private static final String PAIRS_TXT = "src/test/resources/mta/test_od_pairs.txt";

    private static final int PAIRS_TO_GENERATE = 50;

    private static final String[] optimizations = new String[] { "W", "X", "T" };
    
    protected static Graph graph;

//    @Test
    public void run() throws IOException {
        graph = ConstantsForTests.buildGraph(NYCT_SUBWAYS_GTFS);
    	
    	ArrayList<TransitStop> stops = new ArrayList<TransitStop>();
    	
    	for(Vertex v : graph.getVertices()) {
    		if(v instanceof TransitStop) {
    			stops.add((TransitStop)v);
    		}
    	}
    	stops.trimToSize();
    	
    	FileWriter testOdPairs = new FileWriter(PAIRS_TXT);
        
    	int i = PAIRS_TO_GENERATE;
    	while(i-- > 0) {
        	int p1 = (int) (Math.random() * stops.size());
    		int p2 = (int) (Math.random() * stops.size());
        		
    		TransitStop s1 = stops.get(p1);
    		TransitStop s2 = stops.get(p2);
    		
    		testOdPairs.write("Q " + ((Math.random() < .5) ? "Y " : "N ") + 
    				new Date().getTime() + " " + 
    				s1.getLat() + "," + s1.getLon() + " " + 
    				s2.getLat() + "," + s2.getLon() + " " +
    				optimizations[(int)Math.round(Math.random() * 2)] + 
    				"\n");

    		System.out.print(".");
    	}
    
    	testOdPairs.close();
    }

}