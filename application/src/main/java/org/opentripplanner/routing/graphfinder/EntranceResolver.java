package org.opentripplanner.routing.graphfinder;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;

@FunctionalInterface
public interface EntranceResolver {
  Entrance getEntrance(FeedScopedId id);
}
