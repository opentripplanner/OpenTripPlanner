package org.opentripplanner.place.nearbystopfinder;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Look up the scheduled trip patterns visiting a stop.
 */
@FunctionalInterface
public interface PatternsByStopLookup {
  /**
   * Return the scheduled trip patterns visiting the stop with the given id, or an empty
   * collection if no pattern visits it.
   */
  Collection<TripPattern> findPatterns(FeedScopedId stopId);
}
