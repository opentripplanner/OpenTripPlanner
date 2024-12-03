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

  /**
   * Creates a matcher that filters {@link RegularStop} objects with the provided {@link RegularStopRequest}
   * and {@link Predicate}. The {@link Predicate} is used to determine if a {@link RegularStop} is
   * in use. Typically the inUseProvider is a function that checks if the {@link RegularStop} is in
   * a set of used stops.
   * @param request - {@link RegularStopRequest} to filter {@link RegularStop} objects.
   * @param inUseProvider - {@link Predicate} to determine if a {@link RegularStop} is in use.
   * @return - {@link Matcher} for {@link RegularStop} objects.
   */
  public static Matcher<RegularStop> of(
    RegularStopRequest request,
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
