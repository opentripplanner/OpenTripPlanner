package org.opentripplanner.transit.model.filter.transit;

import java.util.function.Predicate;
import org.opentripplanner.transit.api.request.RegularStopRequest;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A factory for creating matchers for {@link RegularStop} objects.
 * <p/>
 * This factory is used to create matchers for {@link RegularStop} objects based on a request. The
 * resulting matcher can be used to filter a list of {@link RegularStop} objects.
 */
public class RegularStopMatcherFactory {

  public static Matcher<RegularStop> of(
    RegularStopRequest request,
    Predicate<RegularStop> inUseProvider
  ) {
    ExpressionBuilder<RegularStop> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(request.feedIds(), RegularStopMatcherFactory::feedIds);
    if (request.filterByInUse()) {
      expr.matches(inUseMatcher(inUseProvider));
    }
    return expr.build();
  }

  static Matcher<RegularStop> feedIds(String feedIds) {
    return new EqualityMatcher<>("feedId", feedIds, stop -> stop.getId().getFeedId());
  }

  static Matcher<RegularStop> inUseMatcher(Predicate<RegularStop> inUseProvider) {
    return new GenericUnaryMatcher<>("inUse", inUseProvider);
  }
}
