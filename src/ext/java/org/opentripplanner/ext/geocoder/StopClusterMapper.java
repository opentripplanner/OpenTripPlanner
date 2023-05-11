package org.opentripplanner.ext.geocoder;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.service.TransitService;

/**
 * Mappers for generating {@link StopCluster} from the transit model.
 */
class StopClusterMapper {

  private final TransitService transitService;

  StopClusterMapper(TransitService transitService) {
    this.transitService = transitService;
  }

  /**
   * De-duplicates collections of {@link StopLocation} and {@link StopLocationsGroup} into a stream
   * of {@link StopCluster}.
   * Deduplication means
   * - stop/station relationships are resolved and only the station returned
   * - of "identical" stops which are very close to each other and have an identical name, only one
   *   is chosen (at random)
   */
  Stream<StopCluster> generateStopClusters(
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
      .flatMap(sl -> this.map(sl).stream());
    var stations = stopLocationsGroups.stream().map(this::map);

    return Stream.concat(stops, stations);
  }

  StopCluster map(StopLocationsGroup g) {
    var modes = transitService.getModesOfStopsLocationGroup(g).map(Enum::name).distinct().toList();
    return new StopCluster(
      g.getId(),
      null,
      g.getName().toString(),
      toCoordinate(g.getCoordinate()),
      modes
    );
  }

  Optional<StopCluster> map(StopLocation sl) {
    return Optional
      .ofNullable(sl.getName())
      .map(name -> {
        var modes = transitService.getModesOfStopLocation(sl).map(Enum::name).distinct().toList();
        return new StopCluster(
          sl.getId(),
          sl.getCode(),
          name.toString(),
          toCoordinate(sl.getCoordinate()),
          modes
        );
      });
  }

  private static StopCluster.Coordinate toCoordinate(WgsCoordinate c) {
    return new StopCluster.Coordinate(c.latitude(), c.longitude());
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private record DeduplicationKey(I18NString name, WgsCoordinate coordinate) {}
}
