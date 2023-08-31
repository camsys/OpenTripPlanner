package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class FutureDepartureTimeOnlyFilter implements ItineraryFilter {

    Calendar targetTime = null;
    long maxHoursBeforeTarget = 6;
    final long SEC_TO_MILL = 1000;
    final long MIN_TO_SEC = 60;
    final long HR_TO_MIN = 60;

    public FutureDepartureTimeOnlyFilter setMaxHoursBeforeTarget(long maxHours){
        maxHoursBeforeTarget = maxHours;
        return this;
    }

    public FutureDepartureTimeOnlyFilter setTargetTime(Long targetTime) {
        //setting target time forward one minute so current
        // requests are not treated as past requests
        this.targetTime = Calendar.getInstance();
        this.targetTime.setTimeInMillis(
                targetTime + 1 * MIN_TO_SEC * SEC_TO_MILL);
        return this;
    }

    @Override
    public String name() {
        return "plausible-departure-time-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        TimeZone zone = null;
        List filteredItineraries = new ArrayList<Itinerary>();
        Calendar earliestAllowedItinEndTime = Calendar.getInstance();
        earliestAllowedItinEndTime.setTimeInMillis(targetTime.getTimeInMillis() -
                (long)maxHoursBeforeTarget * HR_TO_MIN * MIN_TO_SEC * SEC_TO_MILL);


        for (Itinerary itin : itineraries) {
            Calendar itinStart = itin.startTime();
            Calendar now = Calendar.getInstance(itinStart.getTimeZone());
            Calendar itinEnd = itin.endTime();
            if (itinEnd.after(earliestAllowedItinEndTime)) {
                if (now.before(targetTime)) {
                    if (itinStart.after(now)) {
                        filteredItineraries.add(itin);
                    }
                } else {
                    filteredItineraries.add(itin);
                }
            }
        }
        return filteredItineraries;
    }

    @Override
    public boolean removeItineraries() {
        return false;
    }
}
