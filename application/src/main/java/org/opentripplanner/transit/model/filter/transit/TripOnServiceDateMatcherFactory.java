package org.opentripplanner.transit.model.filter.transit;

import java.time.LocalDate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.ContainsMatcher;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.selector.SelectorBasedMatcherFactory;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
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

    if (!request.filters().isEmpty()) {
      expr.matches(
        SelectorBasedMatcherFactory.of(
          request.filters(),
          TripOnServiceDateMatcherFactory::buildSelectorMatcher
        )
      );
    }

    expr.atLeastOneMatch(
      request.includeServiceDates(),
      TripOnServiceDateMatcherFactory::serviceDate
    );
    expr.atLeastOneMatch(request.includeAgencies(), TripOnServiceDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(request.includeRoutes(), TripOnServiceDateMatcherFactory::routeId);
    expr.atLeastOneMatch(
      request.includeServiceJourneys(),
      TripOnServiceDateMatcherFactory::serviceJourneyId
    );
    expr.atLeastOneMatch(
      request.includeReplacementFor(),
      TripOnServiceDateMatcherFactory::replacementFor
    );
    expr.atLeastOneMatch(
      request.includeNetexInternalPlanningCodes(),
      TripOnServiceDateMatcherFactory::netexInternalPlanningCode
    );
    expr.atLeastOneMatch(request.includeAlterations(), TripOnServiceDateMatcherFactory::alteration);
    return expr.build();
  }

  /**
   * Builds a matcher from a single {@link TripOnServiceDateSelectRequest}, combining its
   * agencies, routes, and transport modes with AND logic.
   */
  private static Matcher<TripOnServiceDate> buildSelectorMatcher(
    TripOnServiceDateSelectRequest selector
  ) {
    ExpressionBuilder<TripOnServiceDate> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(selector.agencies(), TripOnServiceDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(selector.routes(), TripOnServiceDateMatcherFactory::routeId);

    if (!selector.transportModes().includeEverything()) {
      var transportModeFilter = AllowTransitModeFilter.of(
        selector.transportModes().get().stream().map(NarrowedTransitMode::of).toList()
      );
      expr.matches(
        new GenericUnaryMatcher<>("transportMode", (TripOnServiceDate tripTime) ->
          transportModeFilter.match(
            tripTime.getTrip().getMode(),
            tripTime.getTrip().getNetexSubMode()
          )
        )
      );
    }

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
    return new EqualityMatcher<>("netexInternalPlanningCode", code, t ->
      t.getTrip().getNetexInternalPlanningCode()
    );
  }

  static Matcher<TripOnServiceDate> serviceDate(LocalDate date) {
    return new EqualityMatcher<>("serviceDate", date, TripOnServiceDate::getServiceDate);
  }

  static Matcher<TripOnServiceDate> alteration(TripAlteration alteration) {
    return new EqualityMatcher<>("alteration", alteration, TripOnServiceDate::getTripAlteration);
  }

  static Matcher<TripOnServiceDate> mode(TransitMode mode) {
    return new EqualityMatcher<>("mode", mode, t -> t.getTrip().getMode());
  }
}
