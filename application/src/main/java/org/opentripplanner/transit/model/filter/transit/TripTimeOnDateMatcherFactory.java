package org.opentripplanner.transit.model.filter.transit;

import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.selector.SelectorBasedMatcherFactory;

/**
 * A factory for creating matchers for TripOnServiceDates.
 * <p>
 * This factory is used to create matchers for {@link org.opentripplanner.model.TripTimeOnDate}
 * objects based on a request. The resulting matcher can be used to filter a list of TripOnServiceDate
 * objects.
 */
public class TripTimeOnDateMatcherFactory {

  /**
   * Creates a matcher for TripTimeOnDate.
   * <p>
   * Applies selector-based transit filters (when present) AND flat include/exclude filters.
   */
  public static Matcher<TripTimeOnDate> of(TripTimeOnDateRequest request) {
    ExpressionBuilder<TripTimeOnDate> expr = ExpressionBuilder.of();

    if (!request.transitFilters().isEmpty()) {
      expr.matches(
        SelectorBasedMatcherFactory.of(
          request.transitFilters(),
          TripTimeOnDateMatcherFactory::buildSelectorMatcher
        )
      );
    }

    expr.atLeastOneMatch(request.includeAgencies(), TripTimeOnDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(request.includeRoutes(), TripTimeOnDateMatcherFactory::routeId);
    expr.atLeastOneMatch(request.includeModes(), TripTimeOnDateMatcherFactory::mode);
    expr.atLeastOneMatch(
      request.includeRunningTimePeriods(),
      TripTimeOnDateMatcherFactory::runningTimePeriod
    );
    expr.matchesNone(request.excludeAgencies(), TripTimeOnDateMatcherFactory::agencyId);
    expr.matchesNone(request.excludeRoutes(), TripTimeOnDateMatcherFactory::routeId);
    expr.matchesNone(request.excludeModes(), TripTimeOnDateMatcherFactory::mode);

    if (request.cancellationPolicy().onlyCancellations()) {
      expr.matches(
        new GenericUnaryMatcher<>("canceledEffectively", TripTimeOnDate::isCanceledEffectively)
      );
    }

    return expr.build();
  }

  /**
   * Builds a matcher from a single {@link TripTimeOnDateSelectRequest}, combining its
   * agencies, routes, and transport modes with AND logic.
   */
  private static Matcher<TripTimeOnDate> buildSelectorMatcher(
    TripTimeOnDateSelectRequest selector
  ) {
    ExpressionBuilder<TripTimeOnDate> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(selector.agencies(), TripTimeOnDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(selector.routes(), TripTimeOnDateMatcherFactory::routeId);

    if (!selector.transportModes().includeEverything()) {
      var transportModeFilter = AllowTransitModeFilter.of(
        selector.transportModes().get().stream().map(NarrowedTransitMode::of).toList()
      );
      expr.matches(
        new GenericUnaryMatcher<>("transportMode", (TripTimeOnDate tripTime) ->
          transportModeFilter.match(
            tripTime.getTrip().getMode(),
            tripTime.getTrip().getNetexSubMode()
          )
        )
      );
    }

    return expr.build();
  }

  private static Matcher<TripTimeOnDate> agencyId(FeedScopedId id) {
    return new EqualityMatcher<>("agency", id, t -> t.getTrip().getRoute().getAgency().getId());
  }

  private static Matcher<TripTimeOnDate> routeId(FeedScopedId id) {
    return new EqualityMatcher<>("route", id, t -> t.getTrip().getRoute().getId());
  }

  private static Matcher<TripTimeOnDate> mode(TransitMode mode) {
    return new EqualityMatcher<>("mode", mode, t -> t.getTrip().getMode());
  }

  /**
   * Matches calls of trips which are running during the given period, according to their schedule.
   * A trip is running from the scheduled departure from its first stop until the scheduled arrival
   * at its last stop. Calls whose trip schedule cannot be resolved never match.
   */
  private static Matcher<TripTimeOnDate> runningTimePeriod(TimePeriod period) {
    return new GenericUnaryMatcher<>("runningTimePeriod", call -> {
      var runningTime = scheduledRunningTime(call);
      return runningTime != null && period.overlaps(runningTime);
    });
  }

  /**
   * Resolves the period of time the trip of the given call is running on its service date, according
   * to its schedule.
   *
   * @return {@code null} if the schedule of the trip cannot be resolved.
   */
  @Nullable
  private static TimePeriod scheduledRunningTime(TripTimeOnDate call) {
    var tripTimes = call.getTripTimes();
    if (tripTimes == null || tripTimes.getNumStops() == 0) {
      return null;
    }
    int departure = tripTimes.getScheduledDepartureTime(0);
    int arrival = tripTimes.getScheduledArrivalTime(tripTimes.getNumStops() - 1);
    if (departure == StopTime.MISSING_VALUE || arrival == StopTime.MISSING_VALUE) {
      return null;
    }
    long midnight = call.getServiceDayMidnight();
    return TimePeriod.of(
      Instant.ofEpochSecond(midnight + departure),
      Instant.ofEpochSecond(midnight + arrival)
    );
  }
}
