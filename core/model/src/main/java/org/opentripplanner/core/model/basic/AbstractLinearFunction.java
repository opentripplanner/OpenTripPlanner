package org.opentripplanner.core.model.basic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * This is a data-transfer-object representing a linear function(constant and coefficient) of time.
 * This class does not implement the function itself, so the calculated result can be any type, like
 * a duration or a generalized-cost. The class only holds the constant time part and the
 * time-coefficient. A function implementation may transform or scale the result as well.
 * <p>
 * The class is a thread-safe, immutable value-object.
 */
public abstract class AbstractLinearFunction<T> implements Serializable {

  private static final double COEFFICIENT_MIN = 0.0;
  private static final double COEFFICIENT_MAX = 100.0;

  /**
   * The step the coefficient are rounded to, as rows of {@code {upper limit, step}}, see
   * {@link DoubleUtils#roundToStep(double, double[][])}.
   */
  static final double[][] COEFFICIENT_SCALE = {
    { 0.02, 0.001 },
    { 0.05, 0.002 },
    { 0.1, 0.005 },
    { 0.2, 0.01 },
    { 0.5, 0.02 },
    { 1.0, 0.05 },
    { 2.0, 0.1 },
    { 5.0, 0.2 },
    { 10.0, 0.5 },
    { 20.0, 1.0 },
    { 50.0, 2.0 },
    { 100.0, 5.0 },
    { Double.MAX_VALUE, 10.0 },
  };

  private final T constant;

  private final double coefficient;

  protected AbstractLinearFunction(T constant, double coefficient) {
    this.constant = Objects.requireNonNull(constant);
    this.coefficient = normalizeCoefficient(coefficient);
  }

  /**
   * Check that the coefficient is in the range [{@value #COEFFICIENT_MIN},
   * {@value #COEFFICIENT_MAX}] and round it to the step of its bucket in the coefficient scale.
   */
  static double normalizeCoefficient(double coefficient) {
    DoubleUtils.requireInRange(coefficient, COEFFICIENT_MIN, COEFFICIENT_MAX);
    return DoubleUtils.roundToStep(coefficient, COEFFICIENT_SCALE);
  }

  /** The constant part of the function in "centi-second" cost. */
  public final T constant() {
    return constant;
  }

  /**
   * The coefficient part of the function. When multiplied with time in seconds the result is cost
   * in "centi-seconds".
   */
  public final double coefficient() {
    return coefficient;
  }

  /**
   * Return true if the result is zero for all times (constant and coefficient is zero).
   */
  public final boolean isZero() {
    return isZero(constant) && coefficient == 0.0;
  }

  /**
   * Return true if the {@code f(x) = x} for all times (constant is zero and the coefficient is
   * one).
   */
  public final boolean isNormal() {
    return isZero(constant) && coefficient == 1.0;
  }

  public final String serialize() {
    return LinearFunctionSerialization.serialize(this, COEFFICIENT_SCALE);
  }

  @Override
  public final String toString() {
    return serialize();
  }

  /**
   * Same as {@link #serialize()}, but the coefficient is written as a percentage, for example
   * {@code 15s + 2%}. This reads better when the function is a limit relative to the input. The
   * text cannot be parsed back.
   */
  public final String toStringWPercentage() {
    var percentage = BigDecimal.valueOf(coefficient).movePointRight(2).stripTrailingZeros();
    return (
      DurationUtils.durationToStr(constantAsDuration()) + " + " + percentage.toPlainString() + "%"
    );
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (AbstractLinearFunction<?>) o;
    return Objects.equals(that.coefficient, coefficient) && Objects.equals(constant, that.constant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(constant, coefficient);
  }

  protected abstract boolean isZero(T value);

  /** This is used for serialization and toString() */
  protected abstract Duration constantAsDuration();
}
