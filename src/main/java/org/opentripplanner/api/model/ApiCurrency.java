package org.opentripplanner.api.model;

import java.util.Objects;

public final class ApiCurrency {
    public String currency;
    public int defaultFractionDigits;
    public String currencyCode;
    public String symbol;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getDefaultFractionDigits() {
        return defaultFractionDigits;
    }

    public void setDefaultFractionDigits(int defaultFractionDigits) {
        this.defaultFractionDigits = defaultFractionDigits;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public ApiCurrency(
            String currency,
            int defaultFractionDigits,
            String currencyCode,
            String symbol
    ) {
        this.currency = currency;
        this.defaultFractionDigits = defaultFractionDigits;
        this.currencyCode = currencyCode;
        this.symbol = symbol;
    }

    public String currency() {
        return currency;
    }

    public int defaultFractionDigits() {
        return defaultFractionDigits;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public String symbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ApiCurrency) obj;
        return Objects.equals(this.currency, that.currency) &&
                this.defaultFractionDigits == that.defaultFractionDigits &&
                Objects.equals(this.currencyCode, that.currencyCode) &&
                Objects.equals(this.symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, defaultFractionDigits, currencyCode, symbol);
    }

    @Override
    public String toString() {
        return "ApiCurrency[" +
                "currency=" + currency + ", " +
                "defaultFractionDigits=" + defaultFractionDigits + ", " +
                "currencyCode=" + currencyCode + ", " +
                "symbol=" + symbol + ']';
    }
}