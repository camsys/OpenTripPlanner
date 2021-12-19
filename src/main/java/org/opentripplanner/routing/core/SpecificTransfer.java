package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

/**
 * SpecificTransfer class used by Transfer. Represents a specific transfer between two stops.
 * See the links described at TransferTable for more details about the specifications.
 * @see TransferTable
 */
public class SpecificTransfer implements Serializable {

    private static final long serialVersionUID = 5058028994896044775L;

    /**
     * Constant containing the minimum specificity that is allowed by the specifications
     */
    public static final int MIN_SPECIFICITY = 0;

    /**
     * Constant containing the maximum specificity that is allowed by the specifications
     */
    public static final int MAX_SPECIFICITY = 4;

    /**
     * Required origins of the passenger to be able to use this transfer.
     */
    private List<Stop> requiredStops;

    /**
     * Route id of arriving trip. Is allowed to be null. Is ignored when fromTripId is not null.
     */
    final private AgencyAndId fromRouteId;

    /**
     * Route id of departing trip. Is allowed to be null. Is ignored when toTripId is not null.
     */
    final private AgencyAndId toRouteId;

    /**
     * Trip id of arriving trip. Is allowed to be null.
     */
    final private AgencyAndId fromTripId;

    /**
     * Trip id of departing trip. Is allowed to be null.
     */
    final private AgencyAndId toTripId;

    /**
     * Value indicating the minimum transfer time in seconds. May contain special (negative) values which meaning
     * can be found in the Transfer.*_TRANSFER constants.
     */
    final int transferTime;

    public List<Stop> getRequiredStops() {
        return requiredStops;
    }

    public void addRequiredStop(Stop requiredStop) {
    	if(requiredStops == null)
    		requiredStops = new ArrayList<>();
        requiredStops.add(requiredStop);
    }

    public SpecificTransfer(List<Stop> requiredStops, AgencyAndId fromRouteId, AgencyAndId toRouteId, AgencyAndId fromTripId, AgencyAndId toTripId, int transferTime) {
        this.requiredStops = requiredStops;
        this.fromRouteId = fromRouteId;
        this.toRouteId = toRouteId;
        this.fromTripId = fromTripId;
        this.toTripId = toTripId;
        this.transferTime = transferTime;
    }

    public SpecificTransfer(Stop requiredStop, Route fromRoute, Route toRoute, Trip fromTrip, Trip toTrip, int transferTime) {
        if (requiredStop != null) {
        	addRequiredStop(requiredStop);
        }
        else {
            this.requiredStops = null;
        }

        if (fromRoute != null) {
            this.fromRouteId = fromRoute.getId();
        }
        else {
            this.fromRouteId = null;
        }

        if (toRoute != null) {
            this.toRouteId = toRoute.getId();
        }
        else {
            this.toRouteId = null;
        }

        if (fromTrip != null) {
            this.fromTripId = fromTrip.getId();
        }
        else {
            this.fromTripId = null;
        }

        if (toTrip != null) {
            this.toTripId = toTrip.getId();
        }
        else {
            this.toTripId = null;
        }

        this.transferTime = transferTime;
    }

    public boolean hasTripSpecificity() {
        return fromTripId != null || toTripId != null;
    }

    /**
     * @return specificity as defined in the specifications
     */
    public int getSpecificity() {
        int specificity = getFromSpecificity() + getToSpecificity();
        assert(specificity >= MIN_SPECIFICITY);
        assert(specificity <= MAX_SPECIFICITY);
        return specificity;
    }

    private int getFromSpecificity() {
        int specificity = 0;
        if (fromTripId != null) {
            specificity = 2;
        }
        else if (fromRouteId != null) {
            specificity = 1;
        }
        return specificity;
    }

    private int getToSpecificity() {
        int specificity = 0;
        if (toTripId != null) {
            specificity = 2;
        }
        else if (toRouteId != null) {
            specificity = 1;
        }
        return specificity;
    }

    /**
     * Returns whether this specific transfer is applicable to a transfer between
     * two trips.
     * @param fromTrip is the arriving trip
     * @param toTrip is the departing trip
     * @return true if this specific transfer is applicable to a transfer between
     *   two trips.
     */
    public boolean matches(Trip fromTrip, Trip toTrip) {
        boolean match = matchesFrom(fromTrip) && matchesTo(toTrip);
        return match;
    }

    public boolean matches(Trip fromTrip, Trip toTrip, Route fromRoute, Route toRoute) {
    	boolean fromTripResult = true;
    	boolean toTripResult = true;
    	boolean toRouteResult = true;
    	boolean fromRouteResult = true;
    	
    	if(fromTrip != null) 
    		fromTripResult = (this.fromTripId == fromTrip.getId());
    	if(toTrip != null) 
    		toTripResult = (this.toTripId == toTrip.getId());
    	if(toRoute != null) 
    		toRouteResult = (this.toRouteId == toRoute.getId());
    	if(fromRoute != null) 
    		fromRouteResult = (this.fromRouteId == fromRoute.getId());
    	    	
    	return fromTripResult && toTripResult && toRouteResult && fromRouteResult;
    }

    public boolean matchesFrom(Trip trip) {
        checkNotNull(trip);

        boolean match = false;
        int specificity = getFromSpecificity();
        if (specificity == 0) {
            match = true;
        }
        else if (specificity == 1) {
            if (trip.getRoute().getId().equals(fromRouteId)) {
                match = true;
            }
        }
        else if (specificity == 2) {
            if (trip.getId().equals(fromTripId)) {
                match = true;
            }
        }
        return match;
    }
    
    public AgencyAndId getToTripId() {
    	return toTripId;
    }

    public AgencyAndId getFromTripId() {
    	return fromTripId;
    }

    public boolean isPreferred() {
    	return transferTime == StopTransfer.PREFERRED_TRANSFER;
    }

    public boolean isTimedTransfer() {
    	return transferTime == StopTransfer.TIMED_TRANSFER;
    }

    private boolean matchesTo(Trip trip) {
        checkNotNull(trip);

        boolean match = false;
        int specificity = getToSpecificity();
        if (specificity == 0) {
            match = true;
        }
        else if (specificity == 1) {
            if (trip.getRoute().getId().equals(toRouteId)) {
                match = true;
            }
        }
        else if (specificity == 2) {
            if (trip.getId().equals(toTripId)) {
                match = true;
            }
        }
        return match;
    }
    
    public String toString() {
    	return "SpecificTransfer(" + fromTripId + "->" + toTripId + " requiredStops=" + requiredStops + ")";
    }

}