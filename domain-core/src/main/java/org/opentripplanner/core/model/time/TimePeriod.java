package org.opentripplanner.core.model.time;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents a half-open period of time {@code [start, end)}.
 * <p>
 * Both bounds are optional: a {@code null} start means that the period has always been valid, and a
 * {@code null} end means that it is valid indefinitely (open-ended).
 */
public final class TimePeriod {

  private static final TimePeriod UNBOUNDED = new TimePeriod(null, null);

  @Nullable
  private final Instant start;

  @Nullable
  private final Instant end;

  private TimePeriod(@Nullable Instant start, @Nullable Instant end) {
    if (start != null && end != null && start.isAfter(end)) {
      throw new IllegalArgumentException(
        "The start of a time period must not be after its end: %s > %s".formatted(start, end)
      );
    }
    this.start = start;
    this.end = end;
  }

  /**
   * Creates a period limited by the given bounds. A {@code null} bound means that the period is
   * unbounded in that direction.
   */
  public static TimePeriod of(@Nullable Instant start, @Nullable Instant end) {
    return new TimePeriod(start, end);
  }

  /**
   * Creates a period which is valid at any point in time.
   */
  public static TimePeriod ofUnbounded() {
    return UNBOUNDED;
  }

  /**
   * The inclusive start of the period, or empty if the period has always been valid.
   */
  public Optional<Instant> start() {
    return Optional.ofNullable(start);
  }

  /**
   * The exclusive end of the period, or empty if the period is valid indefinitely.
   */
  public Optional<Instant> end() {
    return Optional.ofNullable(end);
  }

  /**
   * Returns {@code true} if the period has no defined start, meaning it has always been valid.
   */
  public boolean hasOpenStart() {
    return start == null;
  }

  /**
   * Returns {@code true} if the period has no defined end, meaning it is valid indefinitely.
   */
  public boolean hasOpenEnd() {
    return end == null;
  }

  /**
   * Returns {@code true} if this period overlaps the given {@code other} period. Open bounds on
   * either period are treated as extending indefinitely in that direction.
   */
  public boolean overlaps(TimePeriod other) {
    return (
      (hasOpenStart() || other.hasOpenEnd() || start.isBefore(other.end)) &&
      (hasOpenEnd() || other.hasOpenStart() || other.start.isBefore(end))
    );
  }

  /**
   * Returns {@code true} if the {@code instant} is within this period.
   */
  public boolean contains(Instant instant) {
    return ((hasOpenStart() || !instant.isBefore(start)) && (hasOpenEnd() || end.isAfter(instant)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (
      o instanceof TimePeriod other &&
      Objects.equals(start, other.start) &&
      Objects.equals(end, other.end)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TimePeriod.class)
      .addObj("start", start)
      .addObj("end", end)
      .toString();
  }
}
