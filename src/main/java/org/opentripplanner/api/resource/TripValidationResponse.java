package org.opentripplanner.api.resource;

public class TripValidationResponse {

    public boolean isValidTripPlan;

    /** This no-arg constructor exists to make JAX-RS happy. */
    @SuppressWarnings("unused")
    public TripValidationResponse() {};

    public boolean isValidTripPlan() {
        return isValidTripPlan;
    }

    public void setValidTripPlan(boolean validTripPlan) {
        isValidTripPlan = validTripPlan;
    }
}
