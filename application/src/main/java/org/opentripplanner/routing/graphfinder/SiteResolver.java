package org.opentripplanner.routing.graphfinder;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Interface for resolving stops and entrances by ID.
 * <p>
 * This is useful if you don't want to add a direct dependency on a big service or repository to
 * a component. Instead, you can write small adapter implementations.
 */
public interface SiteResolver extends EntranceResolver {
  RegularStop getStop(FeedScopedId id);
  Entrance getEntrance(FeedScopedId id);
}
