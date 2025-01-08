package org.opentripplanner.transit.model.filter.transit;

import java.util.function.Predicate;
import org.opentripplanner.transit.api.request.FindRegularStopsByBoundingBoxRequest;
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

  /**
   * Creates a matcher that filters {@link RegularStop} objects with the provided {@code request}
   * and {@code inUseProvider}. The {@code inUseProvider} is used to determine if a {@link RegularStop} is
   * in use. The inUseProvider is an injected function, because the check is done by the transit service
   * which has access to all stops and routes. A stop is used if it has routes visiting the stop.
   */
  public static Matcher<RegularStop> of(
    FindRegularStopsByBoundingBoxRequest request,
    Predicate<RegularStop> inUseProvider
  ) {
    ExpressionBuilder<RegularStop> expr = ExpressionBuilder.of();

    if (request.feedId() != null) {
      expr.matches(feedId(request.feedId()));
    }
    if (request.filterByInUse()) {
      expr.matches(inUseMatcher(inUseProvider));
    }
    return expr.build();
  }

  static Matcher<RegularStop> feedId(String feedId) {
    return new EqualityMatcher<>("feedId", feedId, stop -> stop.getId().getFeedId());
  }

  static Matcher<RegularStop> inUseMatcher(Predicate<RegularStop> inUseProvider) {
    return new GenericUnaryMatcher<>("inUse", inUseProvider);
  }
}
