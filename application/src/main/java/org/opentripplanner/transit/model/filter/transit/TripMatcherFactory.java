package org.opentripplanner.transit.model.filter.transit;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model.filter.expr.ContainsMatcher;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A factory for creating matchers for Trip objects.
 * <p>
 * This factory is used to create matchers for {@link Trip} objects based on a request. The
 * resulting matcher can be used to filter a list of Trips.
 */
public class TripMatcherFactory {

  /**
   * Creates a matcher that filters Trips.
   * <p>
   * The {@code serviceDateProvider} is a function that provides the service dates for a given Trip.
   * It is injected because the service dates are determined by the transit service which has access
   * to service dates for a Trip.
   *
   * @param request the criteria for filtering Trips.
   * @param serviceDateProvider a function that provides the service dates for a given Trip.
   * @return a matcher for filtering Trips.
   */
  public static Matcher<Trip> of(
    TripRequest request,
    Function<FeedScopedId, Set<LocalDate>> serviceDateProvider
  ) {
    ExpressionBuilder<Trip> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(request.includeAgencies(), TripMatcherFactory::agencyId);
    expr.atLeastOneMatch(request.includeRoutes(), TripMatcherFactory::routeId);
    expr.matchesNone(request.excludeAgencies(), TripMatcherFactory::agencyId);
    expr.matchesNone(request.excludeRoutes(), TripMatcherFactory::routeId);
    expr.atLeastOneMatch(
      request.includeNetexInternalPlanningCodes(),
      TripMatcherFactory::netexInternalPlanningCode
    );
    expr.atLeastOneMatch(
      request.includeServiceDates(),
      TripMatcherFactory.serviceDate(serviceDateProvider)
    );

    return expr.build();
  }

  static Matcher<Trip> agencyId(FeedScopedId id) {
    return new EqualityMatcher<>("agency", id, t -> t.getRoute().getAgency().getId());
  }

  static Matcher<Trip> routeId(FeedScopedId id) {
    return new EqualityMatcher<>("route", id, t -> t.getRoute().getId());
  }

  static Matcher<Trip> netexInternalPlanningCode(String code) {
    return new EqualityMatcher<>(
      "netexInternalPlanningCode",
      code,
      Trip::getNetexInternalPlanningCode
    );
  }

  static Function<LocalDate, Matcher<Trip>> serviceDate(
    Function<FeedScopedId, Set<LocalDate>> serviceDateProvider
  ) {
    return date ->
      new ContainsMatcher<>(
        "serviceDate",
        t -> serviceDateProvider.apply(t.getServiceId()),
        new EqualityMatcher<>("serviceDate", date, (dateToMatch -> dateToMatch))
      );
  }
}
