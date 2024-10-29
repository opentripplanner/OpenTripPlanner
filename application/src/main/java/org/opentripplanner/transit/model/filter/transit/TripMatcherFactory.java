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

public class TripMatcherFactory {

  public static Matcher<Trip> of(
    TripRequest request,
    Function<FeedScopedId, Set<LocalDate>> activeDateProvider
  ) {
    ExpressionBuilder<Trip> expr = ExpressionBuilder.of();

    expr.or(request.authorities(), TripMatcherFactory::authorityId);
    expr.or(request.lines(), TripMatcherFactory::routeId);
    expr.or(request.privateCodes(), TripMatcherFactory::privateCode);

    expr.or(request.activeDates(), TripMatcherFactory.activeDate(activeDateProvider));
    return expr.build();
  }

  static Matcher<Trip> authorityId(FeedScopedId id) {
    return new EqualityMatcher<>("agency", id, t -> t.getRoute().getAgency().getId());
  }

  static Matcher<Trip> routeId(FeedScopedId id) {
    return new EqualityMatcher<>("route", id, t -> t.getRoute().getId());
  }

  static Matcher<Trip> privateCode(String code) {
    return new EqualityMatcher<>("privateCode", code, Trip::getNetexInternalPlanningCode);
  }

  static Function<LocalDate, Matcher<Trip>> activeDate(
    Function<FeedScopedId, Set<LocalDate>> activeDateProvider
  ) {
    return date ->
      new ContainsMatcher<>(
        "activeDatesAmong",
        t -> activeDateProvider.apply(t.getServiceId()),
        new EqualityMatcher<>("activeDateEquals", date, (dateToMatch -> dateToMatch))
      );
  }
}
