package org.opentripplanner.api.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @param legIndices The leg indices inside the itinerary that these products are valid for.
 * @param products   The list of products that are valid for the leg referenced by the indices.
 */
public final class ApiLegProducts {
    private final List<Integer> legIndices;
    private final Collection<ApiFareProduct> products;

    public ApiLegProducts(List<Integer> legIndices, Collection<ApiFareProduct> products) {
        this.legIndices = legIndices;
        this.products = products;
    }

    public List<Integer> legIndices() {
        return legIndices;
    }

    public Collection<ApiFareProduct> products() {
        return products;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ApiLegProducts) obj;
        return Objects.equals(this.legIndices, that.legIndices) &&
                Objects.equals(this.products, that.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(legIndices, products);
    }

    @Override
    public String toString() {
        return "ApiLegProducts[" +
                "legIndices=" + legIndices + ", " +
                "products=" + products + ']';
    }
}
