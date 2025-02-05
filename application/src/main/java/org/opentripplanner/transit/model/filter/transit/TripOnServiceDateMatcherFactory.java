package org.opentripplanner.transit.model.filter.transit;

import java.time.LocalDate;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.filter.expr.ContainsMatcher;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A factory for creating matchers for TripOnServiceDates.
 * <p>
 * This factory is used to create matchers for {@link TripOnServiceDate} objects based on a request.
 * The resulting matcher can be used to filter a list of TripOnServiceDate objects.
 */
public class TripOnServiceDateMatcherFactory {

  /**
   * Creates a matcher for TripOnServiceDates.
   *
   * @param request the criteria for filtering TripOnServiceDates.
   * @return a matcher for filtering TripOnServiceDates.
   */
  public static Matcher<TripOnServiceDate> of(TripOnServiceDateRequest request) {
    ExpressionBuilder<TripOnServiceDate> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(request.serviceDates(), TripOnServiceDateMatcherFactory::serviceDate);
    expr.atLeastOneMatch(request.agencies(), TripOnServiceDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(request.routes(), TripOnServiceDateMatcherFactory::routeId);
    expr.atLeastOneMatch(
      request.serviceJourneys(),
      TripOnServiceDateMatcherFactory::serviceJourneyId
    );
    expr.atLeastOneMatch(request.replacementFor(), TripOnServiceDateMatcherFactory::replacementFor);
    expr.atLeastOneMatch(
      request.netexInternalPlanningCodes(),
      TripOnServiceDateMatcherFactory::netexInternalPlanningCode
    );
    expr.atLeastOneMatch(request.alterations(), TripOnServiceDateMatcherFactory::alteration);
    return expr.build();
  }

  static Matcher<TripOnServiceDate> agencyId(FeedScopedId id) {
    return new EqualityMatcher<>("agency", id, t -> t.getTrip().getRoute().getAgency().getId());
  }

  static Matcher<TripOnServiceDate> routeId(FeedScopedId id) {
    return new EqualityMatcher<>("route", id, t -> t.getTrip().getRoute().getId());
  }

  static Matcher<TripOnServiceDate> serviceJourneyId(FeedScopedId id) {
    return new EqualityMatcher<>("serviceJourney", id, t -> t.getTrip().getId());
  }

  static Matcher<TripOnServiceDate> replacementFor(FeedScopedId id) {
    return new ContainsMatcher<>(
      "replacementForContains",
      t -> t.getReplacementFor().stream().map(AbstractTransitEntity::getId).toList(),
      new EqualityMatcher<>("replacementForIdEquals", id, (idToMatch -> idToMatch))
    );
  }

  static Matcher<TripOnServiceDate> netexInternalPlanningCode(String code) {
    return new EqualityMatcher<>(
      "netexInternalPlanningCode",
      code,
      t -> t.getTrip().getNetexInternalPlanningCode()
    );
  }

  static Matcher<TripOnServiceDate> serviceDate(LocalDate date) {
    return new EqualityMatcher<>("serviceDate", date, TripOnServiceDate::getServiceDate);
  }

  static Matcher<TripOnServiceDate> alteration(TripAlteration alteration) {
    return new EqualityMatcher<>("alteration", alteration, TripOnServiceDate::getTripAlteration);
  }
}
