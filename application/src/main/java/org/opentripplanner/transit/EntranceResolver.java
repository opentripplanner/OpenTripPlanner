package org.opentripplanner.transit;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;

@FunctionalInterface
public interface EntranceResolver {
  Entrance getEntrance(FeedScopedId id);
}
