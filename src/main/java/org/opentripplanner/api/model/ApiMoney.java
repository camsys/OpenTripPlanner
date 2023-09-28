package org.opentripplanner.api.model;

import java.util.Objects;

public final class ApiMoney {
    private final int cents;
    private final ApiCurrency currency;

    ApiMoney(int cents, ApiCurrency currency) {
        this.cents = cents;
        this.currency = currency;
    }

    public int cents() {
        return cents;
    }

    public ApiCurrency currency() {
        return currency;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ApiMoney) obj;
        return this.cents == that.cents &&
                Objects.equals(this.currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cents, currency);
    }

    @Override
    public String toString() {
        return "ApiMoney[" +
                "cents=" + cents + ", " +
                "currency=" + currency + ']';
    }
}