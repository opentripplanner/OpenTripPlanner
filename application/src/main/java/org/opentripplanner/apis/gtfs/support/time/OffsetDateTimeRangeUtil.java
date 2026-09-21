package org.opentripplanner.apis.gtfs.support.time;

import java.time.OffsetDateTime;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.time.TimePeriod;

/**
 * Utilities for mapping the GraphQL {@code OffsetDateTimeRangeInput} to the domain type
 * {@link TimePeriod}.
 */
public class OffsetDateTimeRangeUtil {

  /**
   * Maps a list of GraphQL time range inputs to {@link TimePeriod}s. Returns {@code null} when the
   * input is {@code null}. An empty list throws an exception.
   */
  @Nullable
  public static List<TimePeriod> mapRanges(
    @Nullable List<GraphQLTypes.GraphQLOffsetDateTimeRangeInput> ranges,
    String fieldName
  ) {
    if (ranges == null) {
      return null;
    }
    requireNonEmpty(ranges, fieldName);
    return ranges
      .stream()
      .map(range -> mapRange(range.getGraphQLStart(), range.getGraphQLEnd(), fieldName))
      .toList();
  }

  /**
   * Maps a single GraphQL time range to a {@link TimePeriod}. The start is inclusive and the end
   * exclusive and both of them are optional.
   */
  public static TimePeriod mapRange(
    @Nullable OffsetDateTime start,
    @Nullable OffsetDateTime end,
    String fieldName
  ) {
    try {
      return TimePeriod.of(
        start == null ? null : start.toInstant(),
        end == null ? null : end.toInstant()
      );
    } catch (IllegalArgumentException e) {
      throw new InvalidInputException(
        "The start of the time range '%s' must not be after its end.".formatted(fieldName)
      );
    }
  }

  /**
   * Throws an exception if the given list of ranges is empty as an empty filter is ambiguous.
   */
  public static void requireNonEmpty(List<?> ranges, String fieldName) {
    if (ranges.isEmpty()) {
      throw new InvalidInputException(
        "Time range filter '%s' must be either null or have at least one entry.".formatted(
          fieldName
        )
      );
    }
  }
}
