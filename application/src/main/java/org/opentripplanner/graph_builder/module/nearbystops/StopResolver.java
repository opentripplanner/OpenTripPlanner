package org.opentripplanner.graph_builder.module.nearbystops;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Interface for resolving stops by ID.
 * <p>
 * This is useful if you don't want to add a direct dependency on a big service or repository to
 * a component. Instead, you can write small adapter implementations.
 */
public interface StopResolver {
  RegularStop getRegularStop(FeedScopedId id);
  AreaStop getAreaStop(FeedScopedId id);
}
