package org.opentripplanner.routing.core;

import java.util.Currency;

/**
 * @param currency The currency of the money.
 * @param cents    The actual currency value in decimal fixed-point, with the default number of
 *                 fraction digits from currency after the decimal point.
 */
public record Money(Currency currency, int cents) implements Comparable<Money> {
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

  public Money withCurrency(Currency updatedCurrency) {
    return new Money(updatedCurrency, cents);
  }
}
