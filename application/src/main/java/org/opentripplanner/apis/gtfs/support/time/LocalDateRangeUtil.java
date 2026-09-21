package org.opentripplanner.apis.gtfs.support.time;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.time.LocalDateRange;

public class LocalDateRangeUtil {

  /**
   * Checks if a service date filter input has at least one filter set. If both start and end are
   * null then no filtering is necessary and this method returns false.
   */
  public static boolean hasServiceDateFilter(GraphQLTypes.GraphQLLocalDateRangeInput dateRange) {
    return (
      dateRange != null &&
      !LocalDateRange.ofExclusiveEnd(
        dateRange.getGraphQLStart(),
        dateRange.getGraphQLEnd()
      ).isUnbounded()
    );
  }

  /**
   * Maps a list of GraphQL date range inputs to {@link LocalDateRange}s. Returns {@code null} when
   * the input is {@code null}. Empty ranges throws an exception.
   */
  @Nullable
  public static List<LocalDateRange> mapRanges(
    @Nullable List<GraphQLTypes.GraphQLLocalDateRangeInput> ranges
  ) {
    if (ranges == null) {
      return null;
    }
    if (ranges.isEmpty()) {
      throw new InvalidInputException(
        "Service date range filter must be either null or have at least one entry."
      );
    }
    return ranges
      .stream()
      .map(range -> LocalDateRange.ofExclusiveEnd(range.getGraphQLStart(), range.getGraphQLEnd()))
      .toList();
  }
}
