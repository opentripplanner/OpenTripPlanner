package org.opentripplanner.transit.model.filter.transit;

import java.util.function.Predicate;
import org.opentripplanner.transit.api.request.FindRoutesRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.CaseInsensitiveStringPrefixMatcher;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.expr.NullSafeWrapperMatcher;
import org.opentripplanner.transit.model.network.Route;

/**
 * A factory for creating matchers for {@link Route} objects.
 * <p/>
 * This factory is used to create matchers for {@link Route} objects based on a request. The
 * resulting matcher can be used to filter a list of {@link Route} objects.
 */
public class RouteMatcherFactory {

  /**
   * Creates a matcher that filters {@link Route} objects with the provided {@code request}
   * and {@code isFlexRoutePredicate}. The {@code isFlexRoutePredicate} is used to determine if a
   * {@link Route} is a flexible route. The isFlexRoutePredicate is an injected function, because
   * the check is done by the transit service which has access to all stops and routes.
   *
   * @param request - a {@link FindRoutesRequest} object that contains the criteria for the matcher.
   * @param isFlexRoutePredicate a function that determines if a {@link Route} is a flexible route.
   * @return a {@link Matcher<Route>} to be used for filtering {@link Route} objects.
   */
  public static Matcher<Route> of(
    FindRoutesRequest request,
    Predicate<Route> isFlexRoutePredicate
  ) {
    ExpressionBuilder<Route> expr = ExpressionBuilder.of();

    if (request.flexibleOnly()) {
      expr.matches(isFlexRoute(isFlexRoutePredicate));
    }
    expr.atLeastOneMatch(request.agencies(), RouteMatcherFactory::agencies);
    expr.atLeastOneMatch(request.transitModes(), RouteMatcherFactory::transitModes);
    if (request.shortName() != null) {
      expr.matches(shortName(request.shortName()));
    }
    expr.atLeastOneMatch(request.shortNames(), RouteMatcherFactory::shortNames);
    if (request.longName() != null) {
      expr.matches(longName(request.longName()));
    }

    return expr.build();
  }

  static Matcher<Route> agencies(String agencyId) {
    return new NullSafeWrapperMatcher<>(
      "agency",
      Route::getAgency,
      new EqualityMatcher<>("agencyId", agencyId, route -> route.getAgency().getId().getId())
    );
  }

  static Matcher<Route> transitModes(TransitMode transitMode) {
    return new EqualityMatcher<>("transitMode", transitMode, Route::getMode);
  }

  static Matcher<Route> shortName(String publicCode) {
    return new NullSafeWrapperMatcher<>(
      "shortName",
      Route::getShortName,
      new EqualityMatcher<>("shortName", publicCode, Route::getShortName)
    );
  }

  static Matcher<Route> shortNames(String publicCode) {
    return new NullSafeWrapperMatcher<>(
      "shortNames",
      Route::getShortName,
      new EqualityMatcher<>("shortNames", publicCode, Route::getShortName)
    );
  }

  static Matcher<Route> isFlexRoute(Predicate<Route> isFlexRoute) {
    return new GenericUnaryMatcher<>("isFlexRoute", isFlexRoute);
  }

  static Matcher<Route> longName(String name) {
    return new NullSafeWrapperMatcher<>(
      "longName",
      Route::getLongName,
      new CaseInsensitiveStringPrefixMatcher<>(
        "name",
        name,
        route -> route.getLongName().toString()
      )
    );
  }
}
