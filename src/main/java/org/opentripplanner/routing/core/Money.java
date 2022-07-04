package org.opentripplanner.routing.core;

import java.text.NumberFormat;
import java.util.Currency;

/**
 * <strong>Fare support is very, very preliminary.</strong>
 */
public class Money implements Comparable<Money> {

  /**
   * The currency of the money.
   */
  private WrappedCurrency currency;
  /**
   * The actual currency value in decimal fixed-point, with the default number of fraction digits
   * from currency after the decimal point.
   */
  private final int cents;

  public Money(WrappedCurrency currency, int cents) {
    this.currency = currency;
    this.cents = cents;
  }

  @Override
  public int compareTo(Money m) {
    if (m.currency != currency) {
      throw new RuntimeException("Can't compare " + m.currency + " to " + currency);
    }
    return cents - m.cents;
  }

  public WrappedCurrency getCurrency() {
    return currency;
  }

  @Deprecated
  public void setCurrency(Currency currency) {
    this.currency = new WrappedCurrency(currency);
  }

  public int getCents() {
    return cents;
  }

  @Override
  public int hashCode() {
    return currency.hashCode() * 31 + cents;
  }

  public boolean equals(Object other) {
    if (other instanceof Money) {
      Money m = (Money) other;
      return m.currency.equals(currency) && m.cents == cents;
    }
    return false;
  }

  public String toString() {
    NumberFormat nf = NumberFormat.getCurrencyInstance();
    Currency cur = currency.getCurrency();
    if (cur == null) {
      return "Money()";
    }
    nf.setCurrency(cur);
    String c = nf.format(cents / (Math.pow(10, currency.getDefaultFractionDigits())));
    return "Money(" + c + ")";
  }
}
