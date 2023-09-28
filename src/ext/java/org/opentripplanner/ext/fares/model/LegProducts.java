package org.opentripplanner.ext.fares.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.plan.Leg;

public final class LegProducts {
    private final Leg leg;
    private final List<FareProduct> products;

    public LegProducts(Leg leg, List<FareProduct> products) {
        this.leg = leg;
        this.products = products;
    }

    public Leg leg() {
        return leg;
    }

    public List<FareProduct> products() {
        return products;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LegProducts) obj;
        return Objects.equals(this.leg, that.leg) &&
                Objects.equals(this.products, that.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leg, products);
    }

    @Override
    public String toString() {
        return "LegProducts[" +
                "leg=" + leg + ", " +
                "products=" + products + ']';
    }
}
