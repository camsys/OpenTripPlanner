package org.opentripplanner.api.model;

import java.util.List;
import java.util.Map;

/**
 * @param fare              The Fares V1 fares.
 * @param details           The Fares V1 fare components.
 * @param coveringItinerary The Fares V2 products that are valid for the entire Itinerary.
 * @param legProducts       The Fares V2 products that cover only parts of the legs of the
 *                          itinerary, ie. the customer has to buy more than one ticket.
 */
public final class ApiItineraryFares {

    Map<String, ApiMoney> fare;
    Map<String, List<ApiFareComponent>> details;
    List<ApiFareProduct> coveringItinerary;
    List<ApiLegProducts> legProducts;

    public ApiItineraryFares(Map<String, ApiMoney> fare, Map<String, List<ApiFareComponent>> details, List<ApiFareProduct> coveringItinerary, List<ApiLegProducts> legProducts) {
        this.fare = fare;
        this.details = details;
        this.coveringItinerary = coveringItinerary;
        this.legProducts = legProducts;
    }

    public Map<String, ApiMoney> getFare() {
        return fare;
    }

    public void setFare(Map<String, ApiMoney> fare) {
        this.fare = fare;
    }

    public Map<String, List<ApiFareComponent>> getDetails() {
        return details;
    }

    public void setDetails(Map<String, List<ApiFareComponent>> details) {
        this.details = details;
    }

    public List<ApiFareProduct> getCoveringItinerary() {
        return coveringItinerary;
    }

    public void setCoveringItinerary(List<ApiFareProduct> coveringItinerary) {
        this.coveringItinerary = coveringItinerary;
    }

    public List<ApiLegProducts> getLegProducts() {
        return legProducts;
    }

    public void setLegProducts(List<ApiLegProducts> legProducts) {
        this.legProducts = legProducts;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "ApiItineraryFares[]";
    }
}
