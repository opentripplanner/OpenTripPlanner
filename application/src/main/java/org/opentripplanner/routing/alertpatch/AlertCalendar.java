package org.opentripplanner.routing.alertpatch;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The validity of an alert, expressed as a set of {@link TimePeriod}s. The alert is valid if any of
 * the periods is valid, which means that an empty calendar is never valid.
 */
public final class AlertCalendar {

  private static final AlertCalendar NEVER_ACTIVE = new AlertCalendar(List.of());
  private static final AlertCalendar ALWAYS_ACTIVE = new AlertCalendar(
    List.of(TimePeriod.ofUnbounded())
  );

  private final List<TimePeriod> timePeriods;

  private AlertCalendar(Collection<TimePeriod> timePeriods) {
    this.timePeriods = List.copyOf(timePeriods);
  }

  public static AlertCalendar of(Collection<TimePeriod> timePeriods) {
    return timePeriods.isEmpty() ? NEVER_ACTIVE : new AlertCalendar(timePeriods);
  }

  public static AlertCalendar of(TimePeriod... timePeriods) {
    return of(List.of(timePeriods));
  }

  /**
   * A calendar with a single, unbounded time period, which is always valid.
   */
  public static AlertCalendar ofAlwaysActive() {
    return ALWAYS_ACTIVE;
  }

  /**
   * A calendar without any time periods, which is never valid.
   */
  public static AlertCalendar ofNeverActive() {
    return NEVER_ACTIVE;
  }

  public Collection<TimePeriod> timePeriods() {
    return timePeriods;
  }

  public boolean isNeverActive() {
    return timePeriods.isEmpty();
  }

  /**
   * Returns {@code true} if any of the time periods overlaps the given {@code period}.
   */
  public boolean isActiveDuring(TimePeriod period) {
    return timePeriods.stream().anyMatch(timePeriod -> timePeriod.overlaps(period));
  }

  /**
   * Returns {@code true} if any of the time periods contains the given {@code instant}.
   */
  public boolean isActiveAt(Instant instant) {
    return timePeriods.stream().anyMatch(timePeriod -> timePeriod.contains(instant));
  }

  /**
   * The earliest start of all time periods, or empty if any period has an open start.
   */
  public Optional<Instant> effectiveStart() {
    if (timePeriods.stream().anyMatch(TimePeriod::hasOpenStart)) {
      return Optional.empty();
    }
    return timePeriods
      .stream()
      .map(TimePeriod::start)
      .flatMap(Optional::stream)
      .min(Comparator.naturalOrder());
  }

  /**
   * The latest end of all time periods, or empty if any period is open-ended.
   */
  public Optional<Instant> effectiveEnd() {
    if (timePeriods.stream().anyMatch(TimePeriod::hasOpenEnd)) {
      return Optional.empty();
    }
    return timePeriods
      .stream()
      .map(TimePeriod::end)
      .flatMap(Optional::stream)
      .max(Comparator.naturalOrder());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof AlertCalendar other && timePeriods.equals(other.timePeriods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timePeriods);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AlertCalendar.class).addCol("timePeriods", timePeriods).toString();
  }
}
