package org.opentripplanner.transit.model.filter.transit;

import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;

/**
 * A factory for creating matchers for the entities that alerts affect, identified by
 * {@link EntityKey}s.
 * <p>
 * The matcher is built from the entity criteria of a single {@link TransitAlertSelectRequest}. An
 * entity matches if it is selected by any of the entity selectors. A selector without entity
 * criteria matches all entities. The alert-level criteria of the selector are ignored, see
 * {@link TransitAlertMatcherFactory}.
 */
public class TransitAlertEntityMatcherFactory {

  /**
   * Creates a matcher for the entities selected by the given selector.
   *
   * @param selector the selector whose entity criteria are used.
   * @param resolver resolves which entities are selected by an entity selector.
   */
  public static Matcher<EntityKey> of(
    TransitAlertSelectRequest selector,
    TransitAlertEntityResolver resolver
  ) {
    return ExpressionBuilder.<EntityKey>of()
      .atLeastOneMatch(selector.entities(), entities -> entities(entities, resolver))
      .build();
  }

  static Matcher<EntityKey> entities(
    TransitAlertEntitySelectRequest entities,
    TransitAlertEntityResolver resolver
  ) {
    return new GenericUnaryMatcher<>("entities", resolver.matcher(entities)::test);
  }
}
