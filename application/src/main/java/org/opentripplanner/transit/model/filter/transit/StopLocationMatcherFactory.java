package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.transit.api.request.FindStopLocationsRequest;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.expr.NullSafeWrapperMatcher;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A factory for creating matchers for StopLocations.
 * <p>
 * This factory creates matchers for {@link StopLocation} objects based on the criteria specified
 * in a request. The resulting matcher can be used to filter a list of StopLocations.
 */
public class StopLocationMatcherFactory {

  /**
   * Creates a matcher that filters StopLocations.
   *
   * @param request the criteria for filtering StopLocations.
   * @return a matcher for filtering StopLocations.
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
