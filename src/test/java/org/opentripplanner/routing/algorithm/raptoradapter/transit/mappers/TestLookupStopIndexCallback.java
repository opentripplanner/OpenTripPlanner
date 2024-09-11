package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Map;
import java.util.stream.IntStream;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public class TestLookupStopIndexCallback implements LookupStopIndexCallback {

  private final Map<FeedScopedId, int[]> index;

  public TestLookupStopIndexCallback(Map<FeedScopedId, int[]> index) {
    this.index = index;
  }

  @Override
  public IntStream lookupStopLocationIndexes(FeedScopedId stopLocationId) {
    int[] values = index.get(stopLocationId);
    if (values == null) {
      throw new EntityNotFoundException(StopLocation.class, stopLocationId);
    }
    return IntStream.of(values);
  }
}
