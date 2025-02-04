package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.transit.api.request.FindStopLocationsRequest;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.expr.NullSafeWrapperMatcher;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A factory for creating matchers for {@link StopLocation} objects.
 * <p/>
 * This factory is used to create matchers for {@link StopLocation} objects based on a request. The
 * resulting matcher can be used to filter a list of {@link StopLocation} objects.
 */
public class StopLocationMatcherFactory {

  /**
   * Creates a matcher that filters {@link StopLocation} objects with the provided {@code request}.
   *
   * @param request - a {@link FindStopLocationsRequest} object that contains the criteria for the
   *                matcher.
   * @return a {@link Matcher<StopLocation>} to be used for filtering {@link StopLocation} objects.
   */
  public static Matcher<StopLocation> of(FindStopLocationsRequest request) {
    ExpressionBuilder<StopLocation> expr = ExpressionBuilder.of();

    if (request.name() != null) {
      expr.matches(name(request.name()));
    }

    return expr.build();
  }

  static Matcher<StopLocation> name(String name) {
    return new NullSafeWrapperMatcher<>(
      "name",
      StopLocation::getName,
      new EqualityMatcher<>("name", name, s -> s.getName().toString())
    );
  }
}
