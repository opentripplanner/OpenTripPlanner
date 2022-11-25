package org.opentripplanner.model.calendar;

import static java.time.LocalDate.MAX;
import static java.time.LocalDate.MIN;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.time.ServiceDateUtils;

/**
 * Value object which represent a service date interval from a starting date until an end date.
 * Both start and end is inclusive.
 * <p>
 * The {@code start} must be equals or before the {@code end} to form a valid period.
 * <p>
 * {@code null} is used to represent an unbounded interval. One or both the of the {@code start} and
 * {@code end} can be {@code null} (unbounded).
 */
public final class ServiceDateInterval {

  private static final ServiceDateInterval UNBOUNDED = new ServiceDateInterval(MIN, MAX);

  private final LocalDate start;
  private final LocalDate end;

  public ServiceDateInterval(LocalDate start, LocalDate end) {
    this.start = start == null ? MIN : start;
    this.end = end == null ? MAX : end;

    // Guarantee that the start is before or equal the end.
    if (this.end.isBefore(this.start)) {
      throw new IllegalArgumentException(
        "Invalid interval, the end " + end + " is before the start " + start
      );
    }
  }

  /**
   * Return a interval with start or end unbounded ({@code null}).
   */
  public static ServiceDateInterval unbounded() {
    return UNBOUNDED;
  }

  public boolean isUnbounded() {
    return start.equals(MIN) && end.equals(MAX);
  }

  /**
   * Return the interval start, inclusive. If the period start is unbounded the {@link
   * LocalDate#MIN} is returned.
   */
  @Nonnull
  public LocalDate getStart() {
    return start;
  }

  /**
   * Return the interval end, inclusive. If the period start is unbounded the {@link
   * LocalDate#MAX} is returned.
   */
  @Nonnull
  public LocalDate getEnd() {
    return end;
  }

  /**
   * The intervals have at least one day in common.
   *
   * @see #intersection(ServiceDateInterval)
   */
  public boolean overlap(ServiceDateInterval other) {
    if (!start.isAfter(other.end)) {
      return !end.isBefore(other.start);
    }
    return false;
  }

  /**
   * Return a new service interval that contains the period with all dates that exist in both
   * periods (intersection of {@code this} and {@code other}).
   *
   * @throws IllegalArgumentException it the to periods do not overlap.
   * @see #overlap(ServiceDateInterval) for checking an intersection exist.
   */
  public ServiceDateInterval intersection(ServiceDateInterval other) {
    return new ServiceDateInterval(
      ServiceDateUtils.max(start, other.start),
      ServiceDateUtils.min(end, other.end)
    );
  }

  /**
   * Return {@code true} is the given {@code date} exist in this period.
   */
  public boolean include(LocalDate date) {
    return !start.isAfter(date) && !end.isBefore(date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceDateInterval that = (ServiceDateInterval) o;
    return start.equals(that.start) && end.equals(that.end);
  }

  @Override
  public String toString() {
    return "[" + ServiceDateUtils.toString(start) + ", " + ServiceDateUtils.toString(end) + "]";
  }

  /**
   * Number of days in a period from start to end, both stat and end is included.
   */
  public int daysInPeriod() {
    return (int) ChronoUnit.DAYS.between(start, end) + 1;
  }
}
