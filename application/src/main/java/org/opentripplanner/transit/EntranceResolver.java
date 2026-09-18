package org.opentripplanner.transit;

import org.opentripplanner.core.domain.model.id.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;

@FunctionalInterface
public interface EntranceResolver {
  Entrance getEntrance(FeedScopedId id);
}
