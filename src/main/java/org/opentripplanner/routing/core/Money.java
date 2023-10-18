package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * @param currency The currency of the money.
 * @param cents    The actual currency value in decimal fixed-point, with the default number of
 *                 fraction digits from currency after the decimal point.
 */
public final class Money implements Comparable<Money>, Serializable {
  private final Currency currency;
  private final int cents;

  public Money(Currency currency, int cents) {
    this.currency = currency;
    this.cents = cents;
  }

  public Currency currency() {
    return currency;
  }

  public int cents() {
    return cents;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (Money) obj;
    return Objects.equals(this.currency, that.currency) &&
            this.cents == that.cents;
  }

  @Override
  public int hashCode() {
    return Objects.hash(currency, cents);
  }

  public static Money euros(int cents) {
    return new Money(Currency.getInstance("EUR"), cents);
  }

  public static Money usDollars(int cents) {
    return new Money(Currency.getInstance("USD"), cents);
  }

  @Override
  public int compareTo(Money m) {
    if (m.currency != currency) {
      throw new RuntimeException("Can't compare " + m.currency + " to " + currency);
    }
    return cents - m.cents;
  }

  @Override
  public String toString() {
    NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.ENGLISH);
    nf.setCurrency(currency);
    nf.setMaximumFractionDigits(currency.getDefaultFractionDigits());
    return nf.format(cents / (Math.pow(10, currency.getDefaultFractionDigits())));
  }
}
