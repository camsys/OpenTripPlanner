package org.opentripplanner.routing.core;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.Stop;

public class TransferDetail {

    public int transferTime;
    public List<Stop> requiredStops;

    public int getTransferTime(){
        return transferTime;
    }

    public List<Stop> getRequiredStops() {
        return requiredStops;
    }

    public void setTransferTime(int transferTime) {
        this.transferTime = transferTime;
    }

    public void setRequiredStops(List<Stop> requiredStops) {
        this.requiredStops = requiredStops;
    }

    public void addRequiredStop(Stop requiredStop) {
    	if(requiredStop == null) {
    		this.requiredStops = null;
    	} else {
	    	if(this.requiredStops == null)
	    		this.requiredStops = new ArrayList<>();
	        this.requiredStops.add(requiredStop);
    	}
    }

}
