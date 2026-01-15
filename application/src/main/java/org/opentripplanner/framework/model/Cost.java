package org.opentripplanner.framework.model;

import java.io.Serializable;
import java.time.Duration;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.lang.OtpNumberFormat;

/**
 * A type safe representation of a cost, like generalized-cost. A cost unit is equivalent of riding
 * transit for 1 seconds. Note! The resolution of the cost is 1/100 (centi-seconds) of a second,
 * the same as in Raptor. A cost can not be negative.
 * <p>
 * This is an immutable, thread-safe value-object.
 */
public sealed class Cost implements Serializable, Comparable<Cost> permits NormalizedCost {

  private static final int CENTI_FACTOR = 100;

  public static final Cost ZERO = new Cost(0);

  public static final Cost ONE_HOUR_WITH_TRANSIT = Cost.fromDuration(Duration.ofHours(1));

  /** The unit is centi-seconds (1/100 of a second) */
  private final int value;

  Cost(int value) {
    this.value = IntUtils.requireNotNegative(value);
  }

  public static Cost costOfSeconds(int valueInTransitSeconds) {
    return new Cost(toCentiSeconds(valueInTransitSeconds));
  }

  public static Cost costOfSeconds(double valueInTransitSeconds) {
    return new Cost(IntUtils.round(valueInTransitSeconds * CENTI_FACTOR));
  }

  public static Cost costOfCentiSeconds(int valueInTransitCentiSeconds) {
    return new Cost(valueInTransitCentiSeconds);
  }

  public static NormalizedCost normalizedCost(int valueInTransitSeconds) {
    return costOfSeconds(valueInTransitSeconds).normalize();
  }

  public static Cost costOfMinutes(int value) {
    return costOfSeconds(value * 60);
  }

  public static Cost fromDuration(Duration value) {
    return costOfSeconds(value.toMillis() / 1000.0);
  }

  /**
   * Returns the cost in seconds. The value is rounded to the nearest second.
   */
  public int toSeconds() {
    return roundToSeconds(value);
  }

  public int toCentiSeconds() {
    return value;
  }

  public boolean isZero() {
    return value == 0;
  }

  public Duration asDuration() {
    return isZero() ? Duration.ZERO : Duration.ofMillis(value * 10);
  }

  public Cost plus(Cost other) {
    return new Cost(value + other.value);
  }

  public Cost minus(Cost other) {
    return new Cost(value - other.value);
  }

  public Cost multiply(int factor) {
    return new Cost(value * factor);
  }

  public Cost multiply(double factor) {
    return new Cost(IntUtils.round(value * factor));
  }

  /* Comparason <, >, <=, >= */

  public boolean greaterThan(Cost other) {
    return this.value > other.value;
  }

  public boolean greaterOrEq(Cost other) {
    return this.value >= other.value;
  }

  public boolean lessThan(Cost other) {
    return this.value < other.value;
  }

  public boolean lessOrEq(Cost other) {
    return this.value <= other.value;
  }

  /**
   * This method round the cost to the nearest second, dropping any centi-seconds.
   */
  public NormalizedCost normalize() {
    return new NormalizedCost(value);
  }

  @Override
  public String toString() {
    return OtpNumberFormat.formatCostCenti(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    return (o instanceof Cost c) ? value == c.value : false;
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public int compareTo(Cost o) {
    return value - o.value;
  }

  static int roundToSeconds(int centiSeconds) {
    return (centiSeconds + CENTI_FACTOR / 2) / CENTI_FACTOR;
  }

  static int toCentiSeconds(int seconds) {
    return seconds * CENTI_FACTOR;
  }
}
