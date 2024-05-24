package org.opentripplanner.apis.gtfs;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLServiceDayFilterInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitService;

public class PatternByServiceDaysFilter {

  private final TransitService transitService;
  private final LocalDate startInclusive;
  private final LocalDate endInclusive;

  PatternByServiceDaysFilter(
    TransitService transitService,
    @Nullable LocalDate startInclusive,
    @Nullable LocalDate endInclusive
  ) {
    Objects.requireNonNull(transitService);
    this.transitService = transitService;

    // optional
    this.startInclusive = startInclusive;
    this.endInclusive = endInclusive;

    if (startInclusive != null && endInclusive != null && startInclusive.isAfter(endInclusive)) {
      throw new IllegalArgumentException("start must be before end");
    }
  }

  public PatternByServiceDaysFilter(
    TransitService transitService,
    GraphQLServiceDayFilterInput graphQLServiceDays
  ) {
    this(transitService, graphQLServiceDays.getGraphQLStart(), graphQLServiceDays.getGraphQLEnd());
  }

  public Collection<TripPattern> filterPatterns(Collection<TripPattern> tripPatterns) {
    return tripPatterns.stream().filter(this::hasServicesOnDate).toList();
  }

  public Collection<Route> filterRoutes(Stream<Route> routeStream) {
    return routeStream
      .filter(r -> {
        var patterns = transitService.getPatternsForRoute(r);
        return !this.filterPatterns(patterns).isEmpty();
      })
      .toList();
  }

  private boolean hasServicesOnDate(TripPattern pattern) {
    return pattern
      .scheduledTripsAsStream()
      .anyMatch(trip -> {
        var dates = transitService
          .getCalendarService()
          .getServiceDatesForServiceId(trip.getServiceId());

        return dates
          .stream()
          .anyMatch(date ->
            (
              startInclusive == null || date.isEqual(startInclusive) || date.isAfter(startInclusive)
            ) &&
            (endInclusive == null || date.isEqual(endInclusive) || date.isBefore(endInclusive))
          );
      });
  }

  public static boolean hasServiceDayFilter(GraphQLServiceDayFilterInput serviceDays) {
    return (
      serviceDays != null &&
      (serviceDays.getGraphQLStart() != null || serviceDays.getGraphQLEnd() != null)
    );
  }
}
