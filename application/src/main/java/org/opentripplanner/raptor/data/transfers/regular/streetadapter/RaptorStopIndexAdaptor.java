package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * A {@link RaptorStopIndex} over the regular stops of a {@link SiteRepository}. It is a view, not a
 * copy, so it always reflects the stops of the repository. Flex stops share the index space of the
 * regular stops, but do not take part in Raptor, so they are not in this index.
 * <p>
 * TODO: This adaptor lives in the transit module for now, but the stop index should move to the
 *       Raptor module. The index is a performance optimization: the memory locality of Raptor's
 *       stop-arrival data depends on it. Because the index space is shared with the flex stops,
 *       Raptor allocates memory for stops that never take part in routing. Generating and
 *       managing the index is a Raptor concern, not a transit model concern.
 */
public class RaptorStopIndexAdaptor implements RaptorStopIndex {

  private final SiteRepository siteRepository;

  public RaptorStopIndexAdaptor(SiteRepository siteRepository) {
    this.siteRepository = siteRepository;
  }

  @Override
  public int toStopIndex(FeedScopedId stopId) {
    var stop = siteRepository.getRegularStop(stopId);
    return stop == null ? RaptorConstants.NOT_FOUND : stop.getIndex();
  }

  @Nullable
  @Override
  public FeedScopedId toStopId(int stopIndex) {
    if (stopIndex < 0 || stopIndex >= size()) {
      return null;
    }
    return siteRepository.stopByIndex(stopIndex) instanceof RegularStop stop ? stop.getId() : null;
  }

  @Override
  public int size() {
    return siteRepository.stopIndexSize();
  }
}
