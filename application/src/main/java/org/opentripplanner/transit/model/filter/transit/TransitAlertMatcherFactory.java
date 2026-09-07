package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.selector.SelectorBasedMatcherFactory;

/**
 * A factory for creating matchers for {@link TransitAlert}s.
 * <p>
 * This factory builds a matcher from a {@link TransitAlertRequest} that can be used to filter a
 * collection of {@link TransitAlert}s. The filters of the request are combined with OR logic, while
 * the criteria within a single selector are combined with AND logic.
 */
public class TransitAlertMatcherFactory {

  /**
   * Creates a matcher for {@link TransitAlert}s.
   *
   * @param request the criteria for filtering alerts.
   * @return a matcher for filtering alerts.
   */
  public static Matcher<TransitAlert> of(TransitAlertRequest request) {
    ExpressionBuilder<TransitAlert> expr = ExpressionBuilder.of();

    if (!request.filters().isEmpty()) {
      expr.matches(
        SelectorBasedMatcherFactory.of(
          request.filters(),
          TransitAlertMatcherFactory::buildSelectorMatcher
        )
      );
    }

    return expr.build();
  }

  /**
   * Builds a matcher from a single {@link TransitAlertSelectRequest}, combining its dimensions with
   * AND logic.
   */
  private static Matcher<TransitAlert> buildSelectorMatcher(TransitAlertSelectRequest selector) {
    ExpressionBuilder<TransitAlert> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(selector.feeds(), TransitAlertMatcherFactory::feed);
    expr.atLeastOneMatch(selector.severityLevels(), TransitAlertMatcherFactory::severity);
    expr.atLeastOneMatch(selector.causes(), TransitAlertMatcherFactory::cause);
    expr.atLeastOneMatch(selector.effects(), TransitAlertMatcherFactory::effect);
    expr.atLeastOneMatch(selector.timePeriods(), TransitAlertMatcherFactory::timePeriod);

    return expr.build();
  }

  static Matcher<TransitAlert> feed(String feedId) {
    return new EqualityMatcher<>("feed", feedId, a -> a.getId().getFeedId());
  }

  static Matcher<TransitAlert> severity(AlertSeverity severity) {
    return new EqualityMatcher<>("severity", severity, TransitAlert::severity);
  }

  static Matcher<TransitAlert> cause(AlertCause cause) {
    return new EqualityMatcher<>("cause", cause, TransitAlert::cause);
  }

  static Matcher<TransitAlert> effect(AlertEffect effect) {
    return new EqualityMatcher<>("effect", effect, TransitAlert::effect);
  }

  static Matcher<TransitAlert> timePeriod(TimePeriod timePeriod) {
    return new GenericUnaryMatcher<>("timePeriod", alert ->
      alert.calendar().isActiveDuring(timePeriod)
    );
  }
}
