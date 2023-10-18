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
    public String id;
    public String name;
    public ApiMoney amount;
    public ApiFareQualifier container;
    public ApiFareQualifier category;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ApiMoney getAmount() {
        return amount;
    }

    public void setAmount(ApiMoney amount) {
        this.amount = amount;
    }

    public ApiFareQualifier getContainer() {
        return container;
    }

    public void setContainer(ApiFareQualifier container) {
        this.container = container;
    }

    public ApiFareQualifier getCategory() {
        return category;
    }

    public void setCategory(ApiFareQualifier category) {
        this.category = category;
    }

    public ApiFareProduct(
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
