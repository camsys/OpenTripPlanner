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

package org.opentripplanner.routing.comparator;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.GraphPath;

public class MtaPathComparator extends PathComparator {

    private int maxPreferredBoardings = -1;
    
    public static List<GraphPath> filter(List<GraphPath> paths, int maxPreferredBoardings) {
    	List<GraphPath> newPaths = new ArrayList<GraphPath>();
    	
		for(GraphPath p : paths) {
			State finalState = p.states.getLast();

			// if we have any preferred boarding trips, show only those, if not, show everything
	    	if(maxPreferredBoardings > 0) {
				if(finalState.getNumPreferredBoardings() > 0) {
			    	p.addPlanAlert(Alert.createSimpleAlerts("Results Filtered", "Results were filtered to show only options using approved connections."));
					newPaths.add(p);					
				}
	    	} else 
				newPaths.add(p);
		}

		return newPaths;
    }
    
    public MtaPathComparator(boolean compareStartTimes, boolean specialOrder, int maxPreferredBoardings) {
        super(compareStartTimes);
        this.maxPreferredBoardings = maxPreferredBoardings;
    }

    private boolean hasLIRR(GraphPath p) {
    	for(AgencyAndId a : p.getTrips()) {
    		if(a.getAgencyId().equals("LI"))
    			return true;
    	}    	
    	return false;
    }
    
    // Walking trips should appear last in results
    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        boolean o1NoTransit = o1.getTrips().isEmpty();
        boolean o2NoTransit = o2.getTrips().isEmpty();
        if (o1NoTransit && !o2NoTransit)
            return 1;
        if (!o1NoTransit && o2NoTransit)
            return -1;

        return (int)(weight(o1) - weight(o2));
    }

    private double weight(GraphPath path) {
        RoutingRequest options = path.states.iterator().next().getOptions();
        long startTime = path.getStartTime();
        long lirrPreferredFlag = 1;
        long waitTime;

        if(hasLIRR(path)) {
        	int numPreferredBoardingsThisPath = path.states.getLast().getNumPreferredBoardings();
        	if(numPreferredBoardingsThisPath > 0 && maxPreferredBoardings > 0)
        		lirrPreferredFlag = -1 * (numPreferredBoardingsThisPath / maxPreferredBoardings); 
        }
        
        waitTime = startTime - options.dateTime;

        return (waitTime * options.waitAtBeginningFactor) + (lirrPreferredFlag * path.getWeight());
    }

}
