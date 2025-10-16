package org.opentripplanner.graph_builder.module.nearbystops;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

/**
 * A small adapter for resolving stops by ID from a {@link TransitService}.
 */
public class TransitServiceResolver implements StopResolver {

  private final TransitService service;

  public TransitServiceResolver(TransitService service) {
    this.service = service;
  }

  @Override
  public RegularStop getRegularStop(FeedScopedId id) {
    return service.getRegularStop(id);
  }

  @Override
  public AreaStop getAreaStop(FeedScopedId id) {
    return service.getAreaStop(id);
  }
}
