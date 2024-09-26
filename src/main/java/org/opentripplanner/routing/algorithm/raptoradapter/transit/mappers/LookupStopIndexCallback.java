package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Collection;
import java.util.stream.IntStream;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * The raptor mapper does not have access to the transit layer, so it needs help to
 * lookup stop-location indexes (Stop index used by Raptor). There is a one-to-one
 * mapping between stops and stop-index, but a station, multimodal-station or group-of-stations
 * will most likely contain more than one stop.
 */
@FunctionalInterface
public interface LookupStopIndexCallback {
  /**
   * The implementation of this method should list all stop indexes part of the entity referenced
   * by the given id.
   * @return a stream of stop indexes. We return a stream here because we need to merge this with
   *         the indexes of other stops.
   */
  IntStream lookupStopLocationIndexes(FeedScopedId stopLocationId);

  /**
   * Take a set of stop location ids and convert them into a sorted distinct list of
   * stop indexes.
   */
  default int[] lookupStopLocationIndexes(Collection<FeedScopedId> stopLocationIds) {
    return stopLocationIds
      .stream()
      .flatMapToInt(this::lookupStopLocationIndexes)
      .sorted()
      .distinct()
      .toArray();
  }
}
