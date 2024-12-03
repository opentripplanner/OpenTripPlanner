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
 * A factory for creating matchers for {@link Trip} objects.
 * <p/>
 * This factory is used to create matchers for {@link Trip} objects based on a request. The
 * resulting matcher can be used to filter a list of {@link Trip} objects.
 */
public class TripMatcherFactory {

  /**
   * Creates a matcher for {@link Trip} objects based on the given request.
   *
   * @param request - a {@link TripRequest} object that contains the criteria for the matcher.
   * @param serviceDateProvider a function that provides the service dates for a given {@link FeedScopedId} of a {@link Trip}.
   * @return a {@link Matcher<Trip>} to be used for filtering {@link Trip} objects.
   */
  public static Matcher<Trip> of(
    TripRequest request,
    Function<FeedScopedId, Set<LocalDate>> serviceDateProvider
  ) {
    ExpressionBuilder<Trip> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(request.agencies(), TripMatcherFactory::agencyId);
    expr.atLeastOneMatch(request.routes(), TripMatcherFactory::routeId);
    expr.atLeastOneMatch(
      request.netexInternalPlanningCodes(),
      TripMatcherFactory::netexInternalPlanningCode
    );
    expr.atLeastOneMatch(
      request.serviceDates(),
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
        "serviceDates",
        t -> serviceDateProvider.apply(t.getServiceId()),
        new EqualityMatcher<>("serviceDate", date, (dateToMatch -> dateToMatch))
      );
  }
}
