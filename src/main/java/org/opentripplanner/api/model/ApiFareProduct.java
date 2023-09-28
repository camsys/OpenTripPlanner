package org.opentripplanner.api.model;

import java.util.Objects;

/**
 * A Fares V2 product. This is a type of ticket or monthly pass that customers can buy.
 *
 * @param id
 * @param name      Name of the product, like "One-way single","Monthly pass"
 * @param amount    The money amount
 * @param container The fare containers, ie. a smart card or an app.
 * @param category  The rider category like senior, youth, veteran.
 */
public final class ApiFareProduct {
    private final String id;
    private final String name;
    private final ApiMoney amount;
    private final ApiFareQualifier container;
    private final ApiFareQualifier category;

    ApiFareProduct(
            String id,
            String name,
            ApiMoney amount,
            ApiFareQualifier container,
            ApiFareQualifier category
    ) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.container = container;
        this.category = category;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ApiMoney amount() {
        return amount;
    }

    public ApiFareQualifier container() {
        return container;
    }

    public ApiFareQualifier category() {
        return category;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ApiFareProduct) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.amount, that.amount) &&
                Objects.equals(this.container, that.container) &&
                Objects.equals(this.category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, amount, container, category);
    }

    @Override
    public String toString() {
        return "ApiFareProduct[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "amount=" + amount + ", " +
                "container=" + container + ", " +
                "category=" + category + ']';
    }
}
