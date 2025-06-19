package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A factory for creating matchers for TripOnServiceDates.
 * <p>
 * This factory is used to create matchers for {@link org.opentripplanner.model.TripTimeOnDate} objects based on a request.
 * The resulting matcher can be used to filter a list of TripOnServiceDate objects.
 */
public class TripTimeOnDateMatcherFactory {

  /**
   * Creates a matcher for TripOnServiceDates.
   *
   * @param request the criteria for filtering TripOnServiceDates.
   * @return a matcher for filtering TripOnServiceDates.
   */
  public static Matcher<TripTimeOnDate> of(TripTimeOnDateRequest request) {
    ExpressionBuilder<TripTimeOnDate> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(request.includeAgencies(), TripTimeOnDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(request.includeRoutes(), TripTimeOnDateMatcherFactory::routeId);
    expr.atLeastOneMatch(request.includeModes(), TripTimeOnDateMatcherFactory::mode);
    expr.matchesNone(request.excludeAgencies(), TripTimeOnDateMatcherFactory::agencyId);
    expr.matchesNone(request.excludeRoutes(), TripTimeOnDateMatcherFactory::routeId);
    expr.matchesNone(request.excludeModes(), TripTimeOnDateMatcherFactory::mode);
    return expr.build();
  }

  private static Matcher<TripTimeOnDate> agencyId(FeedScopedId id) {
    return new EqualityMatcher<>("agency", id, t -> t.getTrip().getRoute().getAgency().getId());
  }

  private static Matcher<TripTimeOnDate> routeId(FeedScopedId id) {
    return new EqualityMatcher<>("route", id, t -> t.getTrip().getRoute().getId());
  }

  private static Matcher<TripTimeOnDate> mode(TransitMode mode) {
    return new EqualityMatcher<>("mode", mode, t -> t.getTrip().getRoute().getMode());
  }
}
