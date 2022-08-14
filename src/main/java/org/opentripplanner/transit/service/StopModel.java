package org.opentripplanner.transit.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.FlexLocationGroup;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopCollection;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.util.MedianCalcForDoubles;
import org.opentripplanner.util.lang.CollectionsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for Stop entities.
 */
public class StopModel implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(StopModel.class);

  private final Map<FeedScopedId, Stop> regularTransitStopById = new HashMap<>();
  private final Map<FeedScopedId, Station> stationById = new HashMap<>();
  private final Map<FeedScopedId, MultiModalStation> multiModalStationById = new HashMap<>();
  /**
   * Optional grouping that can contain both stations and multimodal stations (only supported in
   * NeTEx)
   */
  private final Map<FeedScopedId, GroupOfStations> groupOfStationsById = new HashMap<>();

  /** The density center of the graph for determining the initial geographic extent in the client. */
  private Coordinate center = null;

  private final Map<FeedScopedId, FlexStopLocation> flexStopsById = new HashMap<>();

  private final Map<FeedScopedId, FlexLocationGroup> flexStopGroupsById = new HashMap<>();

  private transient StopModelIndex index;

  @Inject
  public StopModel() {}

  public void index() {
    LOG.info("Index stop model...");
    index =
      new StopModelIndex(getAllStops(), multiModalStationById.values(), flexStopsById.values());
    LOG.info("Index stop model complete.");
  }

  private StopModelIndex getStopModelIndex() {
    //TODO refactoring transit model - thread safety
    if (index == null) {
      index();
    }
    return index;
  }

  /**
   * @param id Id of Stop, Station, MultiModalStation or GroupOfStations
   * @return The coordinate for the transit entity
   */
  @Nullable
  public WgsCoordinate getCoordinateById(FeedScopedId id) {
    // GroupOfStations
    GroupOfStations groupOfStations = groupOfStationsById.get(id);
    if (groupOfStations != null) {
      return groupOfStations.getCoordinate();
    }
    // Multimodal station
    MultiModalStation multiModalStation = multiModalStationById.get(id);
    if (multiModalStation != null) {
      return multiModalStation.getCoordinate();
    }
    // Station
    Station station = stationById.get(id);
    if (station != null) {
      return station.getCoordinate();
    }
    // Single stop (regular transit and flex)
    StopLocation stop = getStopLocationById(id);
    return stop == null ? null : stop.getCoordinate();
  }

  public boolean hasFlexLocations() {
    return !flexStopsById.isEmpty();
  }

  public void addFlexLocation(FlexStopLocation stop) {
    invalidateIndex();
    flexStopsById.put(stop.getId(), stop);
  }

  public Collection<FlexStopLocation> getAllFlexLocations() {
    return flexStopsById.values();
  }

  public void addFlexLocationGroup(FlexLocationGroup group) {
    invalidateIndex();
    flexStopGroupsById.put(group.getId(), group);
  }

  /**
   * Return all stops associated with the given id. All child stops are returned in case a Station,
   * a MultiModalStation, or a GroupOfStations id found. If not regular transit stops, flex stop
   * locations and flex location groups are search and a list with one item is returned if found.
   * An empty list is if nothing is found.
   * <p>
   * TODO OTP2 - This method is use-case specific and only used in one place - refactor this,
   *           - and remove the coupling between StopModel and Graph.
   */
  public Collection<StopLocation> getStopsForId(FeedScopedId id) {
    StopCollection stopCollection = getStopCollectionById(id);
    if (stopCollection != null) {
      return stopCollection.getChildStops();
    }

    // Single stop (regular transit and flex)
    StopLocation stop = getStopLocationById(id);
    return stop == null ? List.of() : List.of(stop);
  }

  /**
   * Returns all {@link StopCollection}s present, including stations, group of stations and
   * multimodal stations.
   */
  public Collection<StopCollection> getAllStopCollections() {
    return new CollectionsView<>(
      stationById.values(),
      multiModalStationById.values(),
      groupOfStationsById.values()
    );
  }

  @Nullable
  public Station getStationById(FeedScopedId id) {
    return stationById.get(id);
  }

  @Nullable
  public MultiModalStation getMultiModalStation(FeedScopedId id) {
    return multiModalStationById.get(id);
  }

  public Collection<Station> getStations() {
    return stationById.values();
  }

  /**
   * Finds a {@link StopCollection} by id. Return a station, multimodal station, or group of
   * station.
   */
  @Nullable
  public StopCollection getStopCollectionById(FeedScopedId id) {
    return findById(id, stationById, multiModalStationById, groupOfStationsById);
  }

  /**
   * Calculates Transit center from median of coordinates of all transitStops if graph has transit.
   * If it doesn't it isn't calculated. (mean value of min, max latitude and longitudes are used)
   * <p>
   * Transit center is saved in center variable
   * <p>
   * This speeds up calculation, but problem is that median needs to have all of
   * latitudes/longitudes in memory, this can become problematic in large installations. It works
   * without a problem on New York State.
   */
  public void calculateTransitCenter() {
    var stops = getAllStops();

    if (stops.isEmpty()) {
      return;
    }

    // we need this check because there could be only FlexStopLocations (which don't have vertices)
    // in the graph
    var medianCalculator = new MedianCalcForDoubles(stops.size());

    stops.forEach(v -> medianCalculator.add(v.getLon()));
    double lon = medianCalculator.median();

    medianCalculator.reset();
    stops.forEach(v -> medianCalculator.add(v.getLat()));
    double lat = medianCalculator.median();

    this.center = new Coordinate(lon, lat);
  }

  public Optional<Coordinate> getCenter() {
    return Optional.ofNullable(center);
  }

  public void addStation(Station station) {
    invalidateIndex();
    stationById.put(station.getId(), station);
  }

  public void addMultiModalStation(MultiModalStation multiModalStation) {
    invalidateIndex();
    multiModalStationById.put(multiModalStation.getId(), multiModalStation);
  }

  public void addGroupsOfStations(GroupOfStations groupOfStations) {
    invalidateIndex();
    groupOfStationsById.put(groupOfStations.getId(), groupOfStations);
  }

  /**
   * Flex locations are generated by GTFS graph builder, but consumed only after the street graph is
   * built
   */
  @Nullable
  public FlexStopLocation getFlexStopsById(FeedScopedId id) {
    return flexStopsById.get(id);
  }

  public Collection<Stop> queryStopSpatialIndex(Envelope envelope) {
    return getStopModelIndex().queryStopSpatialIndex(envelope);
  }

  /**
   * Return a regular transit stop if found(not flex stops).
   */
  public Stop getRegularTransitStopById(FeedScopedId id) {
    return regularTransitStopById.get(id);
  }

  @Nullable
  public MultiModalStation getMultiModalStationForStation(Station station) {
    return getStopModelIndex().getMultiModalStationForStation(station);
  }

  /**
   * Return all stops including regular transit stops, flex stops and flex group of stops.
   */
  public Collection<StopLocation> getAllStops() {
    return new CollectionsView<>(
      regularTransitStopById.values(),
      flexStopsById.values(),
      flexStopGroupsById.values()
    );
  }

  /**
   * Return regular transit stop, flex stop or flex group of stops.
   */
  @Nullable
  public StopLocation getStopLocationById(FeedScopedId id) {
    return findById(id, regularTransitStopById, flexStopsById, flexStopGroupsById);
  }

  public void addStop(Stop stop) {
    invalidateIndex();
    regularTransitStopById.put(stop.getId(), stop);
  }

  public StopLocation stopByIndex(int index) {
    return getStopModelIndex().stopByIndex(index);
  }

  // TODO rename to stopIndexSize
  public int size() {
    return getStopModelIndex().stopIndexSize();
  }

  public Collection<FlexStopLocation> queryLocationIndex(Envelope envelope) {
    return getStopModelIndex().queryLocationIndex(envelope);
  }

  private void invalidateIndex() {
    this.index = null;
  }

  @Nullable
  @SafeVarargs
  private static <V> V findById(FeedScopedId id, Map<FeedScopedId, ? extends V>... maps) {
    for (Map<FeedScopedId, ? extends V> map : maps) {
      V v = map.get(id);
      if (v != null) {
        return v;
      }
    }
    return null;
  }
}
