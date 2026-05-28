package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.core.model.id.FeedScopedId;
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
    expr.matchesNone(request.excludeAgencies(), TripTimeOnDateMatcherFactory::agencyId);
    expr.matchesNone(request.excludeRoutes(), TripTimeOnDateMatcherFactory::routeId);
    expr.matchesNone(request.excludeModes(), TripTimeOnDateMatcherFactory::mode);

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
}
