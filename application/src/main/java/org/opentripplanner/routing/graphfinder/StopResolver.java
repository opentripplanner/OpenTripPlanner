package org.opentripplanner.routing.graphfinder;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Interface for resolving stops by ID.
 * <p>
 * This is useful if you don't want to add a direct dependency on a big service or repository to
 * a component. Instead, you can write small adapter implementations.
 */
@FunctionalInterface
public interface StopResolver {
  RegularStop getStop(FeedScopedId id);
}
