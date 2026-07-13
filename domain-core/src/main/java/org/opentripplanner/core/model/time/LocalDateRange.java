package org.opentripplanner.core.model.time;

import static java.time.LocalDate.MAX;
import static java.time.LocalDate.MIN;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Value object representing a service date range from a starting date until an end date.
 * <p>
 * Internally the range is always stored as [startInclusive, endExclusive). Use the factory
 * methods {@link #ofInclusiveEnd(LocalDate, LocalDate)} and
 * {@link #ofExclusiveEnd(LocalDate, LocalDate)} to construct instances according to the need, but
 * preferable use the exclusive end version unless the inclusivity comes from the input data.
 * {@link #toString()} output mirrors the convention used at construction time.
 * <p>
 * {@code null} is accepted in all factory method parameters and is treated as unbounded (equivalent
 * to {@link LocalDate#MIN} / {@link LocalDate#MAX}).
 */
public final class LocalDateRange {

  private static final LocalDateRange UNBOUNDED = new LocalDateRange(MIN, MAX);

  /**
   * Upper limit on the number of days {@link #asLocalDates(LocalDate, LocalDate)} is allowed to
   * enumerate. This is a guard against accidentally materializing an enormous list of dates.
   */
  private static final int MAX_DAYS_AS_LOCAL_DATES = 10000;

  private final LocalDate inclusiveStart;
  private final LocalDate exclusiveEnd;

  private LocalDateRange(LocalDate inclusiveStart, LocalDate exclusiveEnd) {
    this.inclusiveStart = inclusiveStart;
    this.exclusiveEnd = exclusiveEnd;
    if (exclusiveEnd.isBefore(inclusiveStart)) {
      throw new IllegalArgumentException(
        "Invalid range, the end is before the start: start=" +
          inclusiveStart +
          ", endExclusive=" +
          exclusiveEnd
      );
    }
  }

  /**
   * Create an range with an inclusive start and an inclusive end.
   *
   * @param start inclusive start, or {@code null} for unbounded start
   * @param end   inclusive end, or {@code null} for unbounded end
   */
  public static LocalDateRange ofInclusiveEnd(@Nullable LocalDate start, @Nullable LocalDate end) {
    var startInclusive = start == null ? MIN : start;
    var endInclusive = end == null ? MAX : end;
    if (endInclusive.isBefore(startInclusive)) {
      throw new IllegalArgumentException(
        "Invalid range, the end " + end + " is before the start " + start
      );
    }
    var endExclusive = endInclusive.equals(MAX) ? MAX : endInclusive.plusDays(1);
    return new LocalDateRange(startInclusive, endExclusive);
  }

  /**
   * Create a range with an inclusive start and an exclusive end.
   *
   * @param start inclusive start, or {@code null} for unbounded start
   * @param end   exclusive end (first date outside the range), or {@code null} for unbounded
   *              end
   */
  public static LocalDateRange ofExclusiveEnd(@Nullable LocalDate start, @Nullable LocalDate end) {
    var startInclusive = start == null ? MIN : start;
    var endExclusive = end == null ? MAX : end;
    return new LocalDateRange(startInclusive, endExclusive);
  }

  /**
   * Return the range that covers all dates (both start and end unbounded).
   */
  public static LocalDateRange ofUnbounded() {
    return UNBOUNDED;
  }

  /**
   * Inclusive start date. Returns {@link LocalDate#MIN} when unbounded.
   */
  public LocalDate getStartInclusive() {
    return inclusiveStart;
  }

  /**
   * Inclusive end date. Returns {@link LocalDate#MAX} when unbounded.
   */
  public LocalDate getEndInclusive() {
    return exclusiveEnd.equals(MAX) ? MAX : exclusiveEnd.minusDays(1);
  }

  /**
   * Exclusive end date (first date outside the range). Returns {@link LocalDate#MAX} when
   * unbounded.
   */
  public LocalDate getEndExclusive() {
    return exclusiveEnd;
  }

  public boolean isUnbounded() {
    return isUnboundedStart() && isUnboundedEnd();
  }

  public boolean isUnboundedStart() {
    return inclusiveStart.equals(MIN);
  }

  public boolean isUnboundedEnd() {
    return exclusiveEnd.equals(MAX);
  }

  /**
   * Return {@code true} if the given {@code date} falls within this range.
   */
  public boolean contains(LocalDate date) {
    return !inclusiveStart.isAfter(date) && date.isBefore(exclusiveEnd);
  }

  /**
   * Returns {@code true} if this range and {@code other} share at least one day.
   *
   * @see #intersection(LocalDateRange)
   */
  public boolean overlap(LocalDateRange other) {
    return (
      inclusiveStart.isBefore(other.exclusiveEnd) && other.inclusiveStart.isBefore(exclusiveEnd)
    );
  }

  /**
   * Return a new range containing all dates present in both ranges.
   *
   * @throws IllegalArgumentException if the two ranges do not overlap
   * @see #overlap(LocalDateRange)
   */
  public LocalDateRange intersection(LocalDateRange other) {
    LocalDate newStart = ServiceDateUtils.max(inclusiveStart, other.inclusiveStart);
    LocalDate newEnd = ServiceDateUtils.min(exclusiveEnd, other.exclusiveEnd);
    if (!newStart.isBefore(newEnd)) {
      throw new IllegalArgumentException("ranges do not overlap: " + this + " and " + other);
    }
    return new LocalDateRange(newStart, newEnd);
  }

  /**
   * Enumerate the dates in this range as a list, narrowing each open side to the given defaults.
   * <p>
   * The effective range is the intersection of this range with
   * {@code [defaultInclusiveStart, defaultExclusiveEnd)}: the start is the later of this range's
   * start and {@code defaultInclusiveStart}, and the exclusive end is the earlier of this range's
   * end and {@code defaultExclusiveEnd}. This makes the enumerated range as small as possible. When
   * the two do not overlap an empty list is returned.
   *
   * @param defaultInclusiveStart the start used when this range is unbounded (or wider) at the
   *                              start
   * @param defaultExclusiveEnd   the exclusive end used when this range is unbounded (or wider) at
   *                              the end
   * @throws IllegalArgumentException if the effective range spans more than
   *                                  {@value #MAX_DAYS_AS_LOCAL_DATES} days
   */
  public List<LocalDate> asLocalDates(
    LocalDate defaultInclusiveStart,
    LocalDate defaultExclusiveEnd
  ) {
    LocalDate effectiveInclusiveStart = ServiceDateUtils.max(inclusiveStart, defaultInclusiveStart);
    LocalDate effectiveExclusiveEnd = ServiceDateUtils.min(exclusiveEnd, defaultExclusiveEnd);

    if (!effectiveInclusiveStart.isBefore(effectiveExclusiveEnd)) {
      return List.of();
    }

    long days = ChronoUnit.DAYS.between(effectiveInclusiveStart, effectiveExclusiveEnd);
    if (days > MAX_DAYS_AS_LOCAL_DATES) {
      throw new IllegalArgumentException(
        "The date range [" +
          effectiveInclusiveStart +
          ", " +
          effectiveExclusiveEnd +
          ") spans " +
          days +
          " days, which exceeds the limit of " +
          MAX_DAYS_AS_LOCAL_DATES +
          " days."
      );
    }

    List<LocalDate> dates = new ArrayList<>((int) days);
    for (
      LocalDate date = effectiveInclusiveStart;
      date.isBefore(effectiveExclusiveEnd);
      date = date.plusDays(1)
    ) {
      dates.add(date);
    }
    return dates;
  }

  /**
   * Number of days in the range (inclusive on both sides).
   */
  public int daysInPeriod() {
    return (int) ChronoUnit.DAYS.between(inclusiveStart, exclusiveEnd);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inclusiveStart, exclusiveEnd);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalDateRange that = (LocalDateRange) o;
    return inclusiveStart.equals(that.inclusiveStart) && exclusiveEnd.equals(that.exclusiveEnd);
  }

  @Override
  public String toString() {
    var start = ServiceDateUtils.toString(inclusiveStart);
    var end = ServiceDateUtils.toString(exclusiveEnd);
    return ("[" + start + ", " + end + ")");
  }
}
