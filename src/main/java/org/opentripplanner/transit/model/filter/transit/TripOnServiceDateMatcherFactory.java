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
 * A factory for creating matchers for TripOnServiceDate objects.
 * <p/>
 * This factory is used to create matchers for TripOnServiceDate objects based on a request. The
 * resulting matcher can be used to filter a list of TripOnServiceDate objects.
 */
public class TripOnServiceDateMatcherFactory {

  public static Matcher<TripOnServiceDate> of(TripOnServiceDateRequest request) {
    ExpressionBuilder<TripOnServiceDate> expr = ExpressionBuilder.of();

    expr.or(request.getOperatingDays(), TripOnServiceDateMatcherFactory::operatingDay);
    expr.or(request.getAuthorities(), TripOnServiceDateMatcherFactory::authorityId);
    expr.or(request.getLines(), TripOnServiceDateMatcherFactory::routeId);
    expr.or(request.getServiceJourneys(), TripOnServiceDateMatcherFactory::serviceJourneyId);
    expr.or(request.getReplacementFor(), TripOnServiceDateMatcherFactory::replacementFor);
    expr.or(request.getPrivateCodes(), TripOnServiceDateMatcherFactory::privateCode);
    expr.or(request.getAlterations(), TripOnServiceDateMatcherFactory::alteration);
    return expr.build();
  }

  static Matcher<TripOnServiceDate> authorityId(FeedScopedId id) {
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
      "replacementFor",
      id,
      t -> t.getReplacementFor().stream().map(AbstractTransitEntity::getId).toList()
    );
  }

  static Matcher<TripOnServiceDate> privateCode(String code) {
    return new EqualityMatcher<>(
      "privateCode",
      code,
      t -> t.getTrip().getNetexInternalPlanningCode()
    );
  }

  static Matcher<TripOnServiceDate> operatingDay(LocalDate date) {
    return new EqualityMatcher<>("operatingDay", date, TripOnServiceDate::getServiceDate);
  }

  static Matcher<TripOnServiceDate> alteration(TripAlteration alteration) {
    return new EqualityMatcher<>("alteration", alteration, TripOnServiceDate::getTripAlteration);
  }
}
