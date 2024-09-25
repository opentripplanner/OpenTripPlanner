package org.opentripplanner.transit.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import org.opentripplanner.apis.gtfs.model.LocalDateRange;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Encapsulates the logic to filter patterns by the service dates that they operate on. It also
 * has a method to filter routes by checking if their patterns operate on the required days.
 * <p>
 * Once a more complete filtering engine is in place in the core data model, this code should be
 * there rather than a separate class in the API package.
 */
public class PatternByServiceDatesFilter {

  private final Function<Route, Collection<TripPattern>> getPatternsForRoute;
  private final Function<Trip, Collection<LocalDate>> getServiceDatesForTrip;
  private final LocalDateRange range;

  /**
   * This method is not private to enable unit testing.
   * <p>
   */
  public PatternByServiceDatesFilter(
    LocalDateRange range,
    Function<Route, Collection<TripPattern>> getPatternsForRoute,
    Function<Trip, Collection<LocalDate>> getServiceDatesForTrip
  ) {
    this.getPatternsForRoute = Objects.requireNonNull(getPatternsForRoute);
    this.getServiceDatesForTrip = Objects.requireNonNull(getServiceDatesForTrip);
    this.range = range;

    if (range.unlimited()) {
      throw new IllegalArgumentException("start and end cannot be both null");
    } else if (range.startBeforeEnd()) {
      throw new IllegalArgumentException("start must be before end");
    }
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
  public Collection<Route> filterRoutes(Collection<Route> routeStream) {
    return routeStream
      .stream()
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

        return dates.stream().anyMatch(range::contains);
      });
  }
}
