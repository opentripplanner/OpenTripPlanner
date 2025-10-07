package org.opentripplanner.graph_builder.module.nearbystops;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * Adapter class that extracts two methods from the {@link SiteRepository}.
 */
public class SiteRepositoryResolver implements StopResolver {

  private final SiteRepository siteRepository;

  public SiteRepositoryResolver(SiteRepository siteRepository) {
    this.siteRepository = siteRepository;
  }

  @Override
  public RegularStop getRegularStop(FeedScopedId id) {
    return siteRepository.getRegularStop(id);
  }

  @Override
  public AreaStop getAreaStop(FeedScopedId id) {
    return siteRepository.getAreaStop(id);
  }
}
