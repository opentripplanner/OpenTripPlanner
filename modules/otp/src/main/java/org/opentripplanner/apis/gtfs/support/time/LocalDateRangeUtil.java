package org.opentripplanner.apis.gtfs.support.time;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.model.LocalDateRange;

public class LocalDateRangeUtil {

  /**
   * Checks if a service date filter input has at least one filter set. If both start and end are
   * null then no filtering is necessary and this method returns null.
   */
  public static boolean hasServiceDateFilter(GraphQLTypes.GraphQLLocalDateRangeInput dateRange) {
    return (
      dateRange != null &&
      !new LocalDateRange(dateRange.getGraphQLStart(), dateRange.getGraphQLEnd()).unlimited()
    );
  }
}
