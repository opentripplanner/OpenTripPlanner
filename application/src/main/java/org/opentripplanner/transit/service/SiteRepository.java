package org.opentripplanner.transit.service;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.utils.collection.CollectionsView;
import org.opentripplanner.utils.collection.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for Stop entities.
 */
public class SiteRepository implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(SiteRepository.class);

  private final AtomicInteger stopIndexCounter;
  private final Map<FeedScopedId, RegularStop> regularStopById;
  private final Map<FeedScopedId, Station> stationById;
  private final Map<FeedScopedId, MultiModalStation> multiModalStationById;
  private final Map<FeedScopedId, GroupOfStations> groupOfStationsById;
  private final Map<FeedScopedId, AreaStop> areaStopById;
  private final Map<FeedScopedId, GroupStop> groupStopById;
  private transient SiteRepositoryIndex index;

  @Inject
  public SiteRepository() {
    this.stopIndexCounter = new AtomicInteger(0);
    this.regularStopById = Map.of();
    this.stationById = Map.of();
    this.multiModalStationById = Map.of();
    this.groupOfStationsById = Map.of();
    this.areaStopById = Map.of();
    this.groupStopById = Map.of();
    this.index = createIndex();
  }

  SiteRepository(SiteRepositoryBuilder builder) {
    this.stopIndexCounter = builder.stopIndexCounter();
    this.regularStopById = builder.regularStopsById().asImmutableMap();
    this.stationById = builder.stationById().asImmutableMap();
    this.multiModalStationById = builder.multiModalStationById().asImmutableMap();
    this.groupOfStationsById = builder.groupOfStationById().asImmutableMap();
    this.areaStopById = builder.areaStopById().asImmutableMap();
    this.groupStopById = builder.groupStopById().asImmutableMap();
    reindex();
  }

  /**
   * Merge child into main. The child model must be created using the {@code main.withContext()}
   * method, if not this method will fail! If a duplicate key exist, then child value is kept -
   * this feature is normally not allowed, but not enforced here.
   */
  private SiteRepository(SiteRepository main, SiteRepository child) {
    this.stopIndexCounter = assertSameStopIndexCounterIsUsedToCreateBothModels(main, child);
    this.areaStopById = MapUtils.combine(main.areaStopById, child.areaStopById);
    this.regularStopById = MapUtils.combine(main.regularStopById, child.regularStopById);
    this.groupOfStationsById = MapUtils.combine(
      main.groupOfStationsById,
      child.groupOfStationsById
    );
    this.groupStopById = MapUtils.combine(main.groupStopById, child.groupStopById);
    this.multiModalStationById = MapUtils.combine(
      main.multiModalStationById,
      child.multiModalStationById
    );
    this.stationById = MapUtils.combine(main.stationById, child.stationById);
    reindex();
  }

  /**
   * Create a new builder based on an empty model. This is useful in unit-tests, but should
   * NOT be used in the main code. It is not possible to merge the result with another
   * {@link SiteRepository}, because they do not share the same context(stopIndexCounter).
   * <p>
   * In the application code the correct way is to retrieve a model instance and then use the
   * {@link #withContext()} method to create a builder.
   */
  public static SiteRepositoryBuilder of() {
    return new SiteRepositoryBuilder(new AtomicInteger(0));
  }

  /**
   * Create a new builder attached to the existing model. The entities of the existing model are
   * NOT copied into the builder, but the builder has access to the model - allowing it to check
   * for duplicates and injecting information from the model(indexing). The changes in the
   * SiteRepositoryBuilder can then be merged into the original model - this is for now left to the
   * caller.
   * <p>
   * USE THIS TO CREATE A SAFE BUILDER IN PRODUCTION CODE. You MAY use this method in unit-tests,
   * the alternative is the {@link #of()} method. This method should be used if the test have a
   * SiteRepository and the {@link #of()} method should be used if a stop-model in not needed.
   */
  public SiteRepositoryBuilder withContext() {
    return new SiteRepositoryBuilder(this.stopIndexCounter);
  }

  /**
   * Return a regular transit stop if found (not flex stops).
   */
  public RegularStop getRegularStop(FeedScopedId id) {
    return regularStopById.get(id);
  }

  /**
   * Return all regular transit stops, not flex stops and flex group of stops.
   */
  public Collection<RegularStop> listRegularStops() {
    return regularStopById.values();
  }

  /**
   * Find regular stops within a geographical area.
   */
  public Collection<RegularStop> findRegularStops(Envelope envelope) {
    return index.findRegularStops(envelope);
  }

  public boolean hasAreaStops() {
    return !areaStopById.isEmpty();
  }

  /**
   * Flex locations are generated by GTFS graph builder, but consumed only after the street graph is
   * built.
   */
  @Nullable
  public AreaStop getAreaStop(FeedScopedId id) {
    return areaStopById.get(id);
  }

  /**
   * Return all flex stops, not regular transit stops and flex group of stops.
   */
  public Collection<AreaStop> listAreaStops() {
    return areaStopById.values();
  }

  /**
   * Find flex stops within a geographical area.
   */
  public Collection<AreaStop> findAreaStops(Envelope envelope) {
    return index.findAreaStops(envelope);
  }

  /**
   * Return all flex groups of stops.
   */
  public Collection<GroupStop> listGroupStops() {
    return groupStopById.values();
  }

  @Nullable
  public StopLocation stopByIndex(int stopIndex) {
    return index.stopByIndex(stopIndex);
  }

  public int stopIndexSize() {
    return index.stopIndexSize();
  }

  /**
   * Return regular transit stop, flex stop or flex group of stops.
   */
  @Nullable
  public StopLocation getStopLocation(FeedScopedId id) {
    return getById(id, regularStopById, areaStopById, groupStopById);
  }

  /**
   * Return all stops including regular transit stops, flex stops and flex group of stops.
   */
  public Collection<StopLocation> listStopLocations() {
    return new CollectionsView<>(
      regularStopById.values(),
      areaStopById.values(),
      groupStopById.values()
    );
  }

  @Nullable
  public Station getStationById(FeedScopedId id) {
    return stationById.get(id);
  }

  public Collection<Station> listStations() {
    return stationById.values();
  }

  @Nullable
  public MultiModalStation getMultiModalStation(FeedScopedId id) {
    return multiModalStationById.get(id);
  }

  public Collection<MultiModalStation> listMultiModalStations() {
    return multiModalStationById.values();
  }

  @Nullable
  public MultiModalStation getMultiModalStationForStation(Station station) {
    return index.getMultiModalStationForStation(station);
  }

  public Collection<GroupOfStations> listGroupOfStations() {
    return groupOfStationsById.values();
  }

  /**
   * Finds a {@link StopLocationsGroup} by id. Return a station, multimodal station, or group of
   * station.
   */
  @Nullable
  public StopLocationsGroup getStopLocationsGroup(FeedScopedId id) {
    return getById(id, stationById, multiModalStationById, groupOfStationsById);
  }

  /**
   * Returns all {@link StopLocationsGroup}s present, including stations, group of stations and
   * multimodal stations.
   */
  public Collection<StopLocationsGroup> listStopLocationGroups() {
    return new CollectionsView<>(
      stationById.values(),
      multiModalStationById.values(),
      groupOfStationsById.values()
    );
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
    StopLocation stop = getStopLocation(id);
    return stop == null ? null : stop.getCoordinate();
  }

  /**
   * Return all stops associated with the given id. If a Station, a MultiModalStation, or a
   * GroupOfStations matches the id, then all child stops are returned. If the id matches a regular
   * stops, area stop or stop group, then a list with one item is returned.
   * An empty list is if nothing is found.
   */
  public Collection<StopLocation> findStopOrChildStops(FeedScopedId id) {
    StopLocationsGroup stops = getStopLocationsGroup(id);
    if (stops != null) {
      return stops.getChildStops();
    }

    // Single stop (regular transit and flex)
    StopLocation stop = getStopLocation(id);
    return stop == null ? List.of() : List.of(stop);
  }

  /**
   * Call this method after deserializing this class. This will reindex the SiteRepository.
   */
  public void reindexAfterDeserialization() {
    reindex();
  }

  public SiteRepository merge(SiteRepository child) {
    return new SiteRepository(this, child);
  }

  private void reindex() {
    LOG.info("Index site repository...");
    index = createIndex();
    LOG.info("Index site repository complete.");
  }

  private SiteRepositoryIndex createIndex() {
    return new SiteRepositoryIndex(
      regularStopById.values(),
      areaStopById.values(),
      groupStopById.values(),
      multiModalStationById.values(),
      stopIndexCounter.get()
    );
  }

  @Nullable
  @SafeVarargs
  private static <V> V getById(FeedScopedId id, Map<FeedScopedId, ? extends V>... maps) {
    if (id == null) {
      return null;
    }
    for (Map<FeedScopedId, ? extends V> map : maps) {
      V v = map.get(id);
      if (v != null) {
        return v;
      }
    }
    return null;
  }

  /**
   * The 'stopIndexCounter' must be the same instance, hence the '!=' operator.
   */
  @SuppressWarnings("NumberEquality")
  private static AtomicInteger assertSameStopIndexCounterIsUsedToCreateBothModels(
    SiteRepository main,
    SiteRepository child
  ) {
    if (main.stopIndexCounter != child.stopIndexCounter) {
      throw new IllegalArgumentException(
        "Two Stop repositories can only be merged if they are created with the same stopIndexCounter. " +
        "This is archived by using the 'SiteRepository.withContext()' method. We do this to avoid " +
        "duplicates/gaps in the stopIndex."
      );
    }
    return main.stopIndexCounter;
  }
}
