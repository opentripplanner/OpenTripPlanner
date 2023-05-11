package org.opentripplanner.ext.geocoder;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;

/**
 * Helper for generating {@link StopCluster}.
 */
class StopClusters {

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  /**
   * Deduplicates collections of {@link StopLocation} and {@link StopLocationsGroup} into a stream
   * of {@link StopCluster}.
   * Deduplication means
   * - stop/station relationships are resolved and only the station returned
   * - of "identical" stops which are very close to each other and have an identical name, only one
   *   is chosen (at random)
   */
  static Stream<StopCluster> generateStopClusters(
    Collection<StopLocation> stopLocations,
    Collection<StopLocationsGroup> stopLocationsGroups
  ) {
    var stops = stopLocations
      .stream()
      // remove stop locations without a parent station
      .filter(sl -> sl.getParentStation() == null)
      // stops without a name (for example flex areas) are useless for searching, so we remove them, too
      .filter(sl -> sl.getName() != null)
      // if they are very close to each other and have the same name, only one is chosen (at random)
      .filter(
        distinctByKey(sl ->
          new DeduplicationKey(sl.getName(), sl.getCoordinate().roundToApproximate10m())
        )
      )
      .flatMap(sl -> StopCluster.of(sl).stream());
    var stations = stopLocationsGroups.stream().map(StopCluster::of);

    return Stream.concat(stops, stations);
  }

  private record DeduplicationKey(I18NString name, WgsCoordinate coordinate) {}
}
