package org.opentripplanner.transit.model.filter.transit;

import java.util.function.Predicate;
import org.opentripplanner.routing.alertpatch.EntityKey;

/**
 * Resolves which of the entities that have alerts are selected by a
 * {@link TransitAlertEntitySelectRequest}.
 */
@FunctionalInterface
public interface TransitAlertEntityResolver {
  /**
   * Returns a predicate matching the entity keys selected by the given request. The returned
   * predicate is only meant to be used for a single search and may cache lookups.
   */
  Predicate<EntityKey> matcher(TransitAlertEntitySelectRequest request);
}
