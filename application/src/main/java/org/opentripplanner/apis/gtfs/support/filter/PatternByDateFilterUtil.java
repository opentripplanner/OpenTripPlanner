package org.opentripplanner.apis.gtfs.support.filter;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.transit.service.PatternByServiceDatesFilter;
import org.opentripplanner.transit.service.TransitService;

/**
 * Utility methods for instantiating a {@link PatternByServiceDatesFilter}.
 */
public class PatternByDateFilterUtil {

  public static PatternByServiceDatesFilter ofGraphQL(
    GraphQLTypes.GraphQLLocalDateRangeInput range,
    TransitService transitService
  ) {
    return new PatternByServiceDatesFilter(
      LocalDateRange.ofExclusiveEnd(range.getGraphQLStart(), range.getGraphQLEnd()),
      transitService::findPatterns,
      trip -> transitService.getTripCalendars().listServiceDates(trip.getServiceId())
    );
  }
}
