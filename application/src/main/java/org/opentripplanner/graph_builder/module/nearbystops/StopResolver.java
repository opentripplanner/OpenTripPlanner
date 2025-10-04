package org.opentripplanner.graph_builder.module.nearbystops;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;

public interface StopResolver {
  RegularStop getRegularStop(FeedScopedId id);
  AreaStop getAreaStop(FeedScopedId id);
}
