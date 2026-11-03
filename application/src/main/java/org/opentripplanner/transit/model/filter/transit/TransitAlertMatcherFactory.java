package org.opentripplanner.transit.model.filter.transit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.EntitySelector;
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
 * The filters of a request are combined with the select/not semantics of the
 * {@link SelectorBasedMatcherFactory}. A selector matches the alerts which match both its entity
 * criteria, see {@link TransitAlertEntityMatcherFactory}, and its alert-level criteria (feeds,
 * severity levels, causes, effects and time periods). How the entity criteria are matched depends
 * on whether the selector is used to include or exclude:
 * <ul>
 *   <li>A select matches the alerts which affect <b>at least one</b> of the selected entities.</li>
 *   <li>A not matches the alerts which <b>only</b> affect selected entities. An alert which also
 *   affects other entities is not excluded.</li>
 * </ul>
 */
public class TransitAlertMatcherFactory {

  /**
   * Creates a matcher for the given request. A request without filters matches all alerts.
   *
   * @param entityResolver resolves which entities are selected by the entity criteria.
   */
  public static Matcher<TransitAlert> of(
    TransitAlertRequest request,
    TransitAlertEntityResolver entityResolver
  ) {
    if (request.filters().isEmpty()) {
      return new GenericUnaryMatcher<>("everything", alert -> true);
    }
    // A selector can be used several times, for example as both a select and a not, but its
    // entity criteria are only resolved once.
    Map<TransitAlertSelectRequest, Matcher<EntityKey>> entityMatchers = new HashMap<>();
    Function<TransitAlertSelectRequest, Matcher<EntityKey>> entities = selector ->
      entityMatchers.computeIfAbsent(selector, s ->
        TransitAlertEntityMatcherFactory.of(s, entityResolver)
      );

    return SelectorBasedMatcherFactory.of(
      request.filters(),
      selector -> select(selector, entities.apply(selector)),
      selector -> not(selector, entities.apply(selector))
    );
  }

  /**
   * Creates a matcher for a selector used to include alerts. The entity criteria match the alerts
   * which affect at least one of the selected entities.
   */
  static Matcher<TransitAlert> select(
    TransitAlertSelectRequest selector,
    Matcher<EntityKey> entities
  ) {
    return of(selector, affectsAnyOf(entities));
  }

  /**
   * Creates a matcher for a selector used to exclude alerts. The entity criteria match the alerts
   * which only affect selected entities.
   */
  static Matcher<TransitAlert> not(
    TransitAlertSelectRequest selector,
    Matcher<EntityKey> entities
  ) {
    return of(selector, affectsOnly(entities));
  }

  private static Matcher<TransitAlert> of(
    TransitAlertSelectRequest selector,
    Matcher<TransitAlert> entityCriteria
  ) {
    ExpressionBuilder<TransitAlert> expr = ExpressionBuilder.of();

    if (!selector.entities().includeEverything()) {
      expr.matches(entityCriteria);
    }
    expr.atLeastOneMatch(selector.feeds(), TransitAlertMatcherFactory::feed);
    expr.atLeastOneMatch(selector.severityLevels(), TransitAlertMatcherFactory::severity);
    expr.atLeastOneMatch(selector.causes(), TransitAlertMatcherFactory::cause);
    expr.atLeastOneMatch(selector.effects(), TransitAlertMatcherFactory::effect);
    expr.atLeastOneMatch(selector.timePeriods(), TransitAlertMatcherFactory::timePeriod);

    return expr.build();
  }

  static Matcher<TransitAlert> affectsAnyOf(Matcher<EntityKey> entities) {
    return new GenericUnaryMatcher<>("affectsAnyOf", alert ->
      alert.entities().stream().map(EntitySelector::key).anyMatch(entities::match)
    );
  }

  static Matcher<TransitAlert> affectsOnly(Matcher<EntityKey> entities) {
    return new GenericUnaryMatcher<>(
      "affectsOnly",
      alert ->
        !alert.entities().isEmpty() &&
        alert.entities().stream().map(EntitySelector::key).allMatch(entities::match)
    );
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
