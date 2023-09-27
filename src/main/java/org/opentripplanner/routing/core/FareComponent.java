package org.opentripplanner.routing.core;

import java.util.List;
import java.util.Objects;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 */
public final class FareComponent {
    private final FeedScopedId fareId;
    private final String name;
    private final Money price;
    private final List<FeedScopedId> routes;

    FareComponent(
            FeedScopedId fareId,
            String name,
            Money price,
            List<FeedScopedId> routes
    ) {
        this.fareId = fareId;
        this.name = name;
        this.price = price;
        this.routes = routes;
    }

    public FeedScopedId fareId() {
        return fareId;
    }

    public String name() {
        return name;
    }

    public Money price() {
        return price;
    }

    public List<FeedScopedId> routes() {
        return routes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FareComponent) obj;
        return Objects.equals(this.fareId, that.fareId) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.price, that.price) &&
                Objects.equals(this.routes, that.routes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fareId, name, price, routes);
    }

    @Override
    public String toString() {
        return "FareComponent[" +
                "fareId=" + fareId + ", " +
                "name=" + name + ", " +
                "price=" + price + ", " +
                "routes=" + routes + ']';
    }
}
