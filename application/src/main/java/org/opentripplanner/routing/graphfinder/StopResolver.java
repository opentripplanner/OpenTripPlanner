package org.opentripplanner.routing.graphfinder;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

@FunctionalInterface
public interface StopResolver {
  RegularStop getStop(FeedScopedId id);
}
