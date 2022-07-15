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
  private Currency currency;
  /**
   * The actual currency value in decimal fixed-point, with the default number of fraction digits
   * from currency after the decimal point.
   */
  private final int cents;

  public Money(Currency currency, int cents) {
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

  public Currency getCurrency() {
    return currency;
  }

  @Deprecated
  public void setCurrency(Currency currency) {
    this.currency = currency;
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
    if (currency == null) {
      return "Money()";
    }
    nf.setCurrency(currency);
    String c = nf.format(cents / (Math.pow(10, currency.getDefaultFractionDigits())));
    return "Money(" + c + ")";
  }

  public static Money euros(int cents) {
    return new Money(Currency.getInstance("EUR"), cents);
  }

  public static Money usDollars(int cents) {
    return new Money(Currency.getInstance("USD"), cents);
  }
}
