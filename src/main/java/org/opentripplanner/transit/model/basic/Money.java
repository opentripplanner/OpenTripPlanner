package org.opentripplanner.transit.model.basic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * @param currency The currency of the money.
 * @param amount   The actual currency value in the minor unit, so 1 Euro is represented as 100.
 */
public record Money(Currency currency, int amount) implements Comparable<Money> {
  public static final Currency USD = Currency.getInstance("USD");
  public Money {
    Objects.requireNonNull(currency);
  }
  public static Money euros(int amount) {
    return new Money(Currency.getInstance("EUR"), amount);
  }

  public static Money usDollars(int amount) {
    return new Money(USD, amount);
  }

  /**
   * Take two money instances and return the higher one.
   */
  public static Money max(Money first, Money second) {
    if (!first.currency.equals(second.currency)) {
      throw new RuntimeException("Can't operate on %s and %s as the currencies are not equal.");
    }
    if (first.greaterThan(second)) {
      return first;
    } else {
      return second;
    }
  }

  /**
   * Take a fractional amount of money, ie 1.5 and convert it to amount using the number of default
   * fraction digits of the currency.
   */
  public static Money ofFractionalAmount(Currency currency, float cost) {
    int fractionDigits = 2;
    if (currency != null) {
      fractionDigits = currency.getDefaultFractionDigits();
    }
    int amount = (int) Math.round(cost * Math.pow(10, fractionDigits));
    return new Money(currency, amount);
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

  /**
   * Subtract the money amount from this instance and return the result.
   */
  public Money minus(Money other) {
    return op(other, o -> new Money(currency, amount - o.amount));
  }

  /**
   * Add another money amoutn to this instance and return it.
   */
  public Money plus(Money other) {
    return op(other, o -> new Money(currency, amount + o.amount));
  }

  public boolean greaterThan(Money other) {
    return booleanOp(other, amount > other.amount);
  }

  public boolean lessThan(Money other) {
    return booleanOp(other, amount < other.amount);
  }

  private boolean booleanOp(Money other, boolean amount) {
    if (currency.equals(other.currency)) {
      return amount;
    } else {
      throw new IllegalArgumentException(
        "Cannot perform operations on %s and %s because they have unequal currency.".formatted(
            this,
            other
          )
      );
    }
  }

  @Nonnull
  private Money op(Money other, Function<Money, Money> op) {
    if (currency.equals(other.currency)) {
      return op.apply(other);
    } else {
      throw new IllegalArgumentException(
        "Cannot perform operations on %s and %s because they have unequal currency.".formatted(
            this,
            other
          )
      );
    }
  }
}
