package org.opentripplanner.transit.model.basic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.lang.IntUtils;

/**
 * Represents an amount of money.
 */
public class Money implements Comparable<Money>, Serializable {

  public static final Currency USD = Currency.getInstance("USD");
  public static final Money ZERO_USD = Money.usDollars(0);
  private final Currency currency;
  private final int amount;

  /**
   *
   * @param currency The currency of the money amount
   * @param minorUnitAmount The amount in the smaller currency unit, so for 1.50 EUR pass 150.
   */
  private Money(@Nonnull Currency currency, int minorUnitAmount) {
    this.currency = Objects.requireNonNull(currency);
    this.amount = minorUnitAmount;
  }

  /**
   * Creates a Euro money object.
   * @param amount Amount in fractional euro, so 1.5 for 1.50 EUR
   */
  public static Money euros(float amount) {
    return Money.ofFractionalAmount(Currency.getInstance("EUR"), amount);
  }

  /**
   * Creates a US dollar money object.
   * @param amount Amount in fractional dollars, so 1.5 for 1.50 USD
   */
  public static Money usDollars(float amount) {
    return Money.ofFractionalAmount(USD, amount);
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
  public static Money ofFractionalAmount(@Nonnull Currency currency, float fractionalAmount) {
    Objects.requireNonNull(currency);
    var fractionDigits = currency.getDefaultFractionDigits();
    int amount = IntUtils.round(fractionalAmount * Math.pow(10, fractionDigits));
    return new Money(currency, amount);
  }

  /**
   * Does this instance contain a non-zero amount.
   */
  public boolean isPositive() {
    return amount > 0f;
  }

  /**
   * Is the mount in this instance zero.
   */
  public boolean isZero() {
    return amount == 0f;
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
  public BigDecimal fractionalAmount() {
    int fractionDigits = currency.getDefaultFractionDigits();
    var divisor = BigDecimal.valueOf(Math.pow(10, fractionDigits));
    return new BigDecimal(amount)
      .setScale(fractionDigits, RoundingMode.HALF_UP)
      .divide(divisor, RoundingMode.HALF_UP);
  }

  /**
   * The amount in the minor currency unit, so 1.50 EUR will be represented as 150.
   * <p>
   * If the currency doesn't have a minor unit (like Japanese Yen) it is just the amount.
   */
  public int minorUnitAmount() {
    return amount;
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
   * Add another money amount to this instance and return it.
   */
  public Money plus(Money other) {
    return op(other, o -> new Money(currency, amount + o.amount));
  }

  /**
   * Returns half this instance's amount
   * Amounts in minor currency unit is rounded to nearest integer, so $0.99/2 becomes $0.50
   */
  public Money half() {
    return new Money(currency, IntUtils.round(amount / 2f));
  }

  /**
   * Multiplies the amount with the multiplicator.
   */
  public Money times(int multiplicator) {
    return new Money(this.currency, amount * multiplicator);
  }

  /**
   * Does this instance represent a larger amount than the one passed in?
   */
  public boolean greaterThan(Money other) {
    return booleanOp(other, amount > other.amount);
  }

  /**
   * Does this instance represent a smaller amount than the one passed in?
   */
  public boolean lessThan(Money other) {
    return booleanOp(other, amount < other.amount);
  }

  /**
   * The currency of this instance.
   */
  public Currency currency() {
    return currency;
  }

  private boolean booleanOp(Money other, boolean result) {
    checkCurrencyOrThrow(other);
    return result;
  }

  @Nonnull
  private Money op(Money other, Function<Money, Money> op) {
    checkCurrencyOrThrow(other);
    return op.apply(other);
  }

  private void checkCurrencyOrThrow(Money other) {
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException(
        "Cannot perform operations on %s and %s because they have unequal currency.".formatted(
            this,
            other
          )
      );
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Money other) {
      return other.amount == this.amount && other.currency == this.currency;
    } else {
      return false;
    }
  }
}
