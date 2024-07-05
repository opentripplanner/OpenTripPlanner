package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.framework.model.Units;

/**
 * This is a data-transfer-object representing a linear function(constant and coefficient) of time.
 * This class does not implement the function itself, so the calculated result can be any type,
 * like a duration or a generalized-cost. The class only holds the constant time part and the
 * time-coefficient. A function implementation may transform or scale the result as well.
 * <p>
 * The class is a thread-safe, immutable value-object.
 */
abstract sealed class AbstractLinearFunction<T>
  implements Serializable
  permits CostLinearFunction, TimePenalty {

  private final T constant;

  private final double coefficient;

  protected AbstractLinearFunction(T constant, double coefficient) {
    this.constant = Objects.requireNonNull(constant);
    this.coefficient = Units.normalizedFactor(coefficient, 0.0, 100.0);
  }

  /** The constant part of the function in "centi-second" cost. */
  public final T constant() {
    return constant;
  }

  /**
   * The coefficient part of the function. When multiplied with time in seconds the result
   * is cost in "centi-seconds".
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
   * Return true if the {@code f(x) = x} for all times (constant is zero and the coefficient is one).
   */
  public final boolean isNormal() {
    return isZero(constant) && coefficient == 1.0;
  }

  public final String serialize() {
    return LinearFunctionSerialization.serialize(this);
  }

  @Override
  public final String toString() {
    return serialize();
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
