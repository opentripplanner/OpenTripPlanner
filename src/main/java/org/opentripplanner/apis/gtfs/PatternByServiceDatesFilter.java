package org.opentripplanner.apis.gtfs;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLServiceDateFilterInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

/**
 * Encapsulates the logic to filter patterns by the service dates that they operate on. It also
 * has a method to filter routes by checking if their patterns operate on the required days
 * <p>
 * Once a more complete filtering engine is in place in the core data model, this code should be
 * there rather than a separate class in the API package.
 */
public class PatternByServiceDatesFilter {

  private final LocalDate startInclusive;
  private final LocalDate endExclusive;
  private final Function<Route, Collection<TripPattern>> getPatternsForRoute;
  private final Function<Trip, Collection<LocalDate>> getServiceDatesForTrip;

  /**
   * This method is not private to enable unit testing.
   * <p>
   * See the API documentation for a discussion of {@code startInclusive} and {@code endExclusive}.
   */
  PatternByServiceDatesFilter(
    @Nullable LocalDate startInclusive,
    @Nullable LocalDate endExclusive,
    Function<Route, Collection<TripPattern>> getPatternsForRoute,
    Function<Trip, Collection<LocalDate>> getServiceDatesForTrip
  ) {
    this.getPatternsForRoute = Objects.requireNonNull(getPatternsForRoute);
    this.getServiceDatesForTrip = Objects.requireNonNull(getServiceDatesForTrip);
    // optional, but one must be defined
    this.startInclusive = startInclusive;
    this.endExclusive = endExclusive;

    if (startInclusive == null && endExclusive == null) {
      throw new IllegalArgumentException("startInclusive and endExclusive cannot be both null");
    } else if (
      startInclusive != null && endExclusive != null && startInclusive.isAfter(endExclusive)
    ) {
      throw new IllegalArgumentException("start must be before end");
    }
  }

  public PatternByServiceDatesFilter(
    GraphQLServiceDateFilterInput filterInput,
    TransitService transitService
  ) {
    this(
      filterInput.getGraphQLStart(),
      filterInput.getGraphQLEnd(),
      transitService::getPatternsForRoute,
      trip -> transitService.getCalendarService().getServiceDatesForServiceId(trip.getServiceId())
    );
  }

  /**
   * Filter the patterns by the service dates that it operates on.
   */
  public Collection<TripPattern> filterPatterns(Collection<TripPattern> tripPatterns) {
    return tripPatterns.stream().filter(this::hasServicesOnDate).toList();
  }

  /**
   * Filter the routes by listing all their patterns' service dates and checking if they
   * operate on the specified dates.
   */
  public Collection<Route> filterRoutes(Stream<Route> routeStream) {
    return routeStream
      .filter(r -> {
        var patterns = getPatternsForRoute.apply(r);
        return !this.filterPatterns(patterns).isEmpty();
      })
      .toList();
  }

  private boolean hasServicesOnDate(TripPattern pattern) {
    return pattern
      .scheduledTripsAsStream()
      .anyMatch(trip -> {
        var dates = getServiceDatesForTrip.apply(trip);

        return dates
          .stream()
          .anyMatch(date ->
            (
              startInclusive == null || date.isEqual(startInclusive) || date.isAfter(startInclusive)
            ) &&
            (endExclusive == null || date.isBefore(endExclusive))
          );
      });
  }
}
