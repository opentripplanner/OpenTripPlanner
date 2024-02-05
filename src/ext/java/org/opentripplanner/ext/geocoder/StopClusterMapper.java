package org.opentripplanner.ext.geocoder;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.service.TransitService;

/**
 * Mappers for generating {@link LuceneStopCluster} from the transit model.
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
  Iterable<LuceneStopCluster> generateStopClusters(
    Collection<StopLocation> stopLocations,
    Collection<StopLocationsGroup> stopLocationsGroups
  ) {
    var stops = stopLocations
      .stream()
      // remove stop locations without a parent station
      .filter(sl -> sl.getParentStation() == null)
      // stops without a name (for example flex areas) are useless for searching, so we remove them, too
      .filter(sl -> sl.getName() != null)
      .toList();

    // if they are very close to each other and have the same name, only one is chosen (at random)
    var deduplicatedStops = ListUtils
      .distinctByKey(
        stops,
        sl -> new DeduplicationKey(sl.getName(), sl.getCoordinate().roundToApproximate100m())
      )
      .stream()
      .flatMap(s -> this.map(s).stream())
      .toList();
    var stations = stopLocationsGroups.stream().map(this::map).toList();

    return Iterables.concat(deduplicatedStops, stations);
  }

  LuceneStopCluster map(StopLocationsGroup g) {
    var modes = transitService.getModesOfStopLocationsGroup(g).stream().map(Enum::name).toList();
    var agencies = agenciesForStopLocationsGroup(g)
      .stream()
      .map(s -> s.getId().toString())
      .toList();
    return new LuceneStopCluster(
      g.getId(),
      null,
      g.getName().toString(),
      toCoordinate(g.getCoordinate()),
      modes,
      agencies
    );
  }

  Optional<LuceneStopCluster> map(StopLocation sl) {
    var agencies = agenciesForStopLocation(sl).stream().map(a -> a.getId().toString()).toList();
    return Optional
      .ofNullable(sl.getName())
      .map(name -> {
        var modes = transitService.getModesOfStopLocation(sl).stream().map(Enum::name).toList();
        return new LuceneStopCluster(
          sl.getId(),
          sl.getCode(),
          name.toString(),
          toCoordinate(sl.getCoordinate()),
          modes,
          agencies
        );
      });
  }

  private List<Agency> agenciesForStopLocation(StopLocation stop) {
    return transitService.getRoutesForStop(stop).stream().map(Route::getAgency).distinct().toList();
  }

  private List<Agency> agenciesForStopLocationsGroup(StopLocationsGroup group) {
    return group
      .getChildStops()
      .stream()
      .flatMap(sl -> agenciesForStopLocation(sl).stream())
      .distinct()
      .toList();
  }

  private static StopCluster.Coordinate toCoordinate(WgsCoordinate c) {
    return new StopCluster.Coordinate(c.latitude(), c.longitude());
  }

  static StopCluster.Agency toAgency(Agency a) {
    return new StopCluster.Agency(a.getId(), a.getName());
  }

  static StopCluster.FeedPublisher toFeedPublisher(FeedInfo fi) {
    if (fi == null) {
      return null;
    } else {
      return new StopCluster.FeedPublisher(fi.getPublisherName());
    }
  }

  private record DeduplicationKey(I18NString name, WgsCoordinate coordinate) {}
}
