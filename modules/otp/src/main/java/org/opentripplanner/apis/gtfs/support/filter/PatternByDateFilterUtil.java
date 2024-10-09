package org.opentripplanner.apis.gtfs.support.filter;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.model.LocalDateRange;
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
      new LocalDateRange(range.getGraphQLStart(), range.getGraphQLEnd()),
      transitService::getPatternsForRoute,
      trip -> transitService.getCalendarService().getServiceDatesForServiceId(trip.getServiceId())
    );
  }
}
