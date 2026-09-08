package org.opentripplanner.apis.gtfs.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.time.TimePeriod;

/**
 * A range of time with inclusive start and exclusive end {@code [start, end)}. Both bounds are
 * optional.
 * <p>
 * A {@code null} start means that the range extends indefinitely into the past and a {@code null}
 * end means that it extends indefinitely into the future.
 * <p>
 * This is a general purpose API model which is not tied to any specific domain concept, so it can
 * be used whenever an unbounded range of instants needs to be returned by the GTFS GraphQL API.
 * <p>
 * TODO we can consider using this for use cases where the range is always required to be bounded in one or both directions.
 */
public record OffsetDateTimeRange(@Nullable OffsetDateTime start, @Nullable OffsetDateTime end) {
  /**
   * Orders the ranges chronologically: ranges with an unbounded start come first and ranges with an
   * unbounded end come last.
   */
  public static final Comparator<OffsetDateTimeRange> CHRONOLOGICAL_ORDER = Comparator.comparing(
    OffsetDateTimeRange::start,
    Comparator.nullsFirst(Comparator.naturalOrder())
  ).thenComparing(OffsetDateTimeRange::end, Comparator.nullsLast(Comparator.naturalOrder()));

  /**
   * Converts a {@link TimePeriod} into a range using the given time zone for the offsets.
   */
  public static OffsetDateTimeRange of(TimePeriod period, ZoneId zoneId) {
    return new OffsetDateTimeRange(
      period
        .start()
        .map(start -> OffsetDateTime.ofInstant(start, zoneId))
        .orElse(null),
      period
        .end()
        .map(end -> OffsetDateTime.ofInstant(end, zoneId))
        .orElse(null)
    );
  }
}
