package org.opentripplanner.api.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class ApiFareComponent {
    public FeedScopedId fareId;
    public String name;
    public ApiMoney price;
    public List<FeedScopedId> routes;

    public FeedScopedId getFareId() {
        return fareId;
    }

    public void setFareId(FeedScopedId fareId) {
        this.fareId = fareId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ApiMoney getPrice() {
        return price;
    }

    public void setPrice(ApiMoney price) {
        this.price = price;
    }

    public List<FeedScopedId> getRoutes() {
        return routes;
    }

    public void setRoutes(List<FeedScopedId> routes) {
        this.routes = routes;
    }

    public ApiFareComponent(
            FeedScopedId fareId,
            String name,
            ApiMoney price,
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

    public ApiMoney price() {
        return price;
    }

    public List<FeedScopedId> routes() {
        return routes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ApiFareComponent) obj;
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
        return "ApiFareComponent[" +
                "fareId=" + fareId + ", " +
                "name=" + name + ", " +
                "price=" + price + ", " +
                "routes=" + routes + ']';
    }
}
