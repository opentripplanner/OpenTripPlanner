package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.transit.api.request.FindStopLocationsRequest;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.expr.NullSafeWrapperMatcher;
import org.opentripplanner.transit.model.site.StopLocation;

public class StopLocationMatcherFactory {

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
