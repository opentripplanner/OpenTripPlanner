package org.opentripplanner.transit.model.basic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * @param currency The currency of the money.
 * @param amount   The actual currency value in the minor unit, so 1 Euro is represented as 100.
 */
public record Money(Currency currency, int amount) implements Comparable<Money> {
  public Money {
    Objects.requireNonNull(currency);
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
    return amount - m.amount;
  }

  /**
   * The amount in the major currency unit, so USD 3.10 is represented as 3.1 (not 310!).
   */
  public double fractionalAmount() {
    int fractionDigits = currency.getDefaultFractionDigits();
    var divisor = BigDecimal.valueOf(Math.pow(10, fractionDigits));
    return new BigDecimal(amount)
      .setScale(fractionDigits, RoundingMode.HALF_UP)
      .divide(divisor, RoundingMode.HALF_UP)
      .doubleValue();
  }

  public String localize(Locale loc) {
    NumberFormat nf = NumberFormat.getCurrencyInstance(loc);
    nf.setCurrency(currency);
    nf.setMaximumFractionDigits(currency.getDefaultFractionDigits());
    return nf.format(fractionalAmount());
  }

  @Override
  public String toString() {
    return localize(Locale.ENGLISH);
  }
}
