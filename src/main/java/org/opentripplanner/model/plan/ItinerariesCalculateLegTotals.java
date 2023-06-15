package org.opentripplanner.model.plan;

import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;


/**
 * Calculate derived itinerary fields
 */
class ItinerariesCalculateLegTotals {

    int totalDurationSeconds = 0;
    int transitTimeSeconds = 0;
    int nTransitLegs = 0;
    int nonTransitTimeSeconds = 0;
    double nonTransitDistanceMeters = 0.0;
    int waitingTimeSeconds;
    boolean walkOnly = true;
    boolean streetOnly = true;

    boolean hasFlex = false;

    public ItinerariesCalculateLegTotals(List<Leg> legs) {
        if (legs.isEmpty()) { return; }
        calculate(legs);
    }

    private void calculate(List<Leg> legs) {
        long startTimeMs = legs.get(0).startTime.getTimeInMillis();
        long endTimeMs = legs.get(legs.size()-1).endTime.getTimeInMillis();

        totalDurationSeconds = (int) Math.round((endTimeMs - startTimeMs) / 1000.0);

        for (Leg leg : legs) {
            long dt = leg.getDuration();

            if (leg.isTransitLeg()) {
                transitTimeSeconds += dt;
                ++nTransitLegs;
            }
            else if(leg.isOnStreetNonTransit()) {
                nonTransitTimeSeconds += dt;
                nonTransitDistanceMeters += leg.distanceMeters;
            }
            if(leg.flexibleTrip){
                this.hasFlex = true;
            }
            if (!leg.isWalkingLeg()) {
                this.walkOnly = false;
            }
            if (!leg.isOnStreetNonTransit()) {
                this.streetOnly = false;
            }
        }
        this.waitingTimeSeconds = totalDurationSeconds
                - (transitTimeSeconds + nonTransitTimeSeconds);
    }
    int transfers() { return nTransitLegs == 0 ? 0 : nTransitLegs -1; }
}
