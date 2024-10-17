package org.opentripplanner.framework.model;

import java.io.Serializable;
import java.time.Duration;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.lang.OtpNumberFormat;

/**
 * A type safe representation of a cost, like generalized-cost. A cost unit is equivalent of riding
 * transit for 1 seconds. Note! The resolution of the cost is 1 second here, while it is 1/100
 * (centi-seconds) in Raptor. A cost can not be negative.
 * <p>
 * This is an immutable, thread-safe value-object.
 */
public final class Cost implements Serializable, Comparable<Cost> {

  private static final int CENTI_FACTOR = 100;

  public static final Cost ZERO = Cost.costOfSeconds(0);

  public static final Cost ONE_HOUR_WITH_TRANSIT = Cost.fromDuration(Duration.ofHours(1));

  private final int value;

  private Cost(int value) {
    this.value = IntUtils.requireNotNegative(value);
  }

  public static Cost costOfMinutes(int value) {
    return costOfSeconds(value * 60);
  }

  public static Cost costOfSeconds(int transitSeconds) {
    return new Cost(transitSeconds);
  }

  public static Cost costOfSeconds(double value) {
    return costOfSeconds(IntUtils.round(value));
  }

  public static Cost fromDuration(Duration value) {
    return new Cost(IntUtils.round(value.toMillis() / 1000.0));
  }

  public int toSeconds() {
    return value;
  }

  public int toCentiSeconds() {
    return value * CENTI_FACTOR;
  }

  public boolean isZero() {
    return value == 0;
  }

  public Duration asDuration() {
    return isZero() ? Duration.ZERO : Duration.ofSeconds(value);
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

  @Override
  public String toString() {
    return OtpNumberFormat.formatCost(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (Cost) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public int compareTo(Cost o) {
    return value - o.value;
  }
}
