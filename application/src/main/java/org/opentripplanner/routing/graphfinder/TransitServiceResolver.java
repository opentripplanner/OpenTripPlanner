package org.opentripplanner.routing.graphfinder;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

/**
 * A small adapter for resolving site entities by ID from a {@link TransitService}.
 */
public class TransitServiceResolver implements SiteResolver, StopResolver {

  private final TransitService transitService;

  public TransitServiceResolver(TransitService transitService) {
    this.transitService = transitService;
  }

  @Override
  public RegularStop getStop(FeedScopedId id) {
    return Objects.requireNonNull(transitService.getRegularStop(id));
  }

  @Override
  public Entrance getEntrance(FeedScopedId id) {
    return transitService.getEntrance(id);
  }
}
