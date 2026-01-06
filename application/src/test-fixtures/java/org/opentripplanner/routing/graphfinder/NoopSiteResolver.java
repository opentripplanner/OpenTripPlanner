package org.opentripplanner.routing.graphfinder;

import org.apache.commons.lang3.NotImplementedException;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;

public class NoopSiteResolver implements SiteResolver {

  @Override
  public RegularStop getStop(FeedScopedId id) {
    throw new NotImplementedException();
  }

  @Override
  public Entrance getEntrance(FeedScopedId id) {
    throw new NotImplementedException();
  }
}
