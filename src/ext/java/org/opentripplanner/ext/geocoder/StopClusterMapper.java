package org.opentripplanner.ext.geocoder;

import static org.opentripplanner.ext.geocoder.StopCluster.LocationType.STATION;
import static org.opentripplanner.ext.geocoder.StopCluster.LocationType.STOP;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.Station;
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
    List<StopLocation> stops = stopLocations
      .stream()
      // remove stop locations without a parent station
      .filter(sl -> sl.getParentStation() == null)
      // stops without a name (for example flex areas) are useless for searching, so we remove them, too
      .filter(sl -> sl.getName() != null)
      .toList();

    // if they are very close to each other and have the same name, only one is chosen (at random)
    var deduplicatedStops = stops
      .stream()
      .collect(
        Collectors.groupingBy(sl ->
          new DeduplicationKey(sl.getName(), sl.getCoordinate().roundToApproximate100m())
        )
      )
      .values()
      .stream()
      .map(group -> map(group).orElse(null))
      .filter(Objects::nonNull)
      .toList();
    var stations = stopLocationsGroups.stream().map(StopClusterMapper::map).toList();

    return Iterables.concat(deduplicatedStops, stations);
  }

  private static LuceneStopCluster map(StopLocationsGroup g) {
    var childStops = g.getChildStops();
    var ids = childStops.stream().map(s -> s.getId().toString()).toList();
    var childNames = getNames(childStops);
    var codes = getCodes(childStops);

    return new LuceneStopCluster(
      g.getId().toString(),
      ids,
      ListUtils.combine(List.of(g.getName()), childNames),
      codes,
      toCoordinate(g.getCoordinate())
    );
  }

  private static List<String> getCodes(Collection<StopLocation> childStops) {
    return childStops.stream().map(StopLocation::getCode).filter(Objects::nonNull).toList();
  }

  private static List<I18NString> getNames(Collection<StopLocation> childStops) {
    return childStops.stream().map(StopLocation::getName).filter(Objects::nonNull).toList();
  }

  private static Optional<LuceneStopCluster> map(List<StopLocation> stopLocations) {
    var primary = stopLocations.getFirst();
    var secondaryIds = stopLocations.stream().skip(1).map(sl -> sl.getId().toString()).toList();
    var names = getNames(stopLocations);
    var codes = getCodes(stopLocations);

    return Optional
      .ofNullable(primary.getName())
      .map(name ->
        new LuceneStopCluster(
          primary.getId().toString(),
          secondaryIds,
          names,
          codes,
          toCoordinate(primary.getCoordinate())
        )
      );
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

  StopCluster.Location toLocation(FeedScopedId id) {
    var loc = transitService.getStopLocation(id);
    if (loc != null) {
      var feedPublisher = toFeedPublisher(transitService.getFeedInfo(id.getFeedId()));
      var modes = transitService.getModesOfStopLocation(loc).stream().map(Enum::name).toList();
      var agencies = agenciesForStopLocation(loc)
        .stream()
        .map(StopClusterMapper::toAgency)
        .toList();
      return new StopCluster.Location(
        loc.getId(),
        loc.getCode(),
        STOP,
        loc.getName().toString(),
        new StopCluster.Coordinate(loc.getLat(), loc.getLon()),
        modes,
        agencies,
        feedPublisher
      );
    } else {
      var group = transitService.getStopLocationsGroup(id);
      var feedPublisher = toFeedPublisher(transitService.getFeedInfo(id.getFeedId()));
      var modes = transitService
        .getModesOfStopLocationsGroup(group)
        .stream()
        .map(Enum::name)
        .toList();
      var agencies = agenciesForStopLocationsGroup(group)
        .stream()
        .map(StopClusterMapper::toAgency)
        .toList();
      return new StopCluster.Location(
        group.getId(),
        extractCode(group),
        STATION,
        group.getName().toString(),
        new StopCluster.Coordinate(group.getLat(), group.getLon()),
        modes,
        agencies,
        feedPublisher
      );
    }
  }

  @Nullable
  private static String extractCode(StopLocationsGroup group) {
    if (group instanceof Station station) {
      return station.getCode();
    } else {
      return null;
    }
  }

  private static StopCluster.Coordinate toCoordinate(WgsCoordinate c) {
    return new StopCluster.Coordinate(c.latitude(), c.longitude());
  }

  static StopCluster.Agency toAgency(Agency a) {
    return new StopCluster.Agency(a.getId(), a.getName());
  }

  private static StopCluster.FeedPublisher toFeedPublisher(FeedInfo fi) {
    if (fi == null) {
      return null;
    } else {
      return new StopCluster.FeedPublisher(fi.getPublisherName());
    }
  }

  private record DeduplicationKey(I18NString name, WgsCoordinate coordinate) {}
}
