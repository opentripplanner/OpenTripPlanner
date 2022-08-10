package org.opentripplanner.transit.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for Stop entities.
 */
public class StopModel implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(StopModel.class);

  /** Parent stops **/
  private final Map<FeedScopedId, Station> stationById = new HashMap<>();
  /**
   * Optional level above parent stops (only supported in NeTEx)
   */
  private final Map<FeedScopedId, MultiModalStation> multiModalStationById = new HashMap<>();
  /**
   * Optional grouping that can contain both stations and multimodal stations (only supported in
   * NeTEx)
   */
  private final Map<FeedScopedId, GroupOfStations> groupOfStationsById = new HashMap<>();

  private final Map<FeedScopedId, TransitStopVertex> transitStopVertices = new HashMap<>();

  /** The density center of the graph for determining the initial geographic extent in the client. */
  private Coordinate center = null;

  private final Map<FeedScopedId, FlexStopLocation> locationsById = new HashMap<>();

  private final Map<FeedScopedId, FlexLocationGroup> locationGroupsById = new HashMap<>();

  private transient StopModelIndex index;

  public TransitStopVertex getStopVertexForStop(Stop stop) {
    return index.getStopVertexForStop(stop);
  }

  public void index() {
    LOG.info("Index stop model...");
    index = new StopModelIndex(this);
    LOG.info("Index stop model complete.");
  }

  public StopModelIndex getStopModelIndex() {
    //TODO refactoring transit model - thread safety
    if (index == null) {
      index();
    }
    return index;
  }

  /**
   * @param id Id of Stop, Station, MultiModalStation or GroupOfStations
   * @return The associated TransitStopVertex or all underlying TransitStopVertices
   */
  public Set<Vertex> getStopVerticesById(FeedScopedId id) {
    var stops = getStopsForId(id);

    if (stops == null) {
      return null;
    }

    return stops
      .stream()
      .filter(Stop.class::isInstance)
      .map(Stop.class::cast)
      .map(index::getStopVertexForStop)
      .collect(Collectors.toSet());
  }

  /**
   * @param id Id of Stop, Station, MultiModalStation or GroupOfStations
   * @return The coordinate for the transit entity
   */
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

    // Single stop
    var stop = index.getStopForId(id);
    if (stop != null) {
      return stop.getCoordinate();
    }

    return null;
  }

  public Collection<MultiModalStation> getAllMultiModalStations() {
    return multiModalStationById.values();
  }

  public Collection<TransitStopVertex> getAllStopVertices() {
    return transitStopVertices.values();
  }

  public boolean hasFlexLocations() {
    return !locationsById.isEmpty();
  }

  public void addFlexLocation(FeedScopedId id, FlexStopLocation flexStopLocation) {
    locationsById.put(id, flexStopLocation);
  }

  public Collection<FlexStopLocation> getAllFlexLocations() {
    return locationsById.values();
  }

  public Collection<FlexLocationGroup> getAllFlexLocationGroups() {
    return locationGroupsById.values();
  }

  public void addFlexLocationGroup(FeedScopedId id, FlexLocationGroup flexLocationGroup) {
    locationGroupsById.put(id, flexLocationGroup);
  }

  private Collection<StopLocation> getStopsForId(FeedScopedId id) {
    // GroupOfStations
    GroupOfStations groupOfStations = groupOfStationsById.get(id);
    if (groupOfStations != null) {
      return groupOfStations.getChildStops();
    }

    // Multimodal station
    MultiModalStation multiModalStation = multiModalStationById.get(id);
    if (multiModalStation != null) {
      return multiModalStation.getChildStops();
    }

    // Station
    Station station = stationById.get(id);
    if (station != null) {
      return station.getChildStops();
    }
    // Single stop
    var stop = index.getStopForId(id);
    if (stop != null) {
      return Collections.singleton(stop);
    }

    return null;
  }

  /**
   * Returns all {@link StopCollection}s present in this graph, including stations, group of
   * stations and multimodal stations.
   */
  public Stream<StopCollection> getAllStopCollections() {
    return Stream.concat(
      stationById.values().stream(),
      Stream.concat(groupOfStationsById.values().stream(), multiModalStationById.values().stream())
    );
  }

  public Station getStationById(FeedScopedId id) {
    return stationById.get(id);
  }

  public MultiModalStation getMultiModalStation(FeedScopedId id) {
    return multiModalStationById.get(id);
  }

  public Collection<Station> getStations() {
    return stationById.values();
  }

  /**
   * Finds a {@link StopCollection} by id.
   */
  public StopCollection getStopCollectionById(FeedScopedId id) {
    var station = stationById.get(id);
    if (station != null) {
      return station;
    }

    var groupOfStations = groupOfStationsById.get(id);
    if (groupOfStations != null) {
      return groupOfStations;
    }

    return multiModalStationById.get(id);
  }

  public void addTransitStopVertex(FeedScopedId id, TransitStopVertex stopVertex) {
    transitStopVertices.put(id, stopVertex);
  }

  /**
   * Calculates Transit center from median of coordinates of all transitStops if graph has transit.
   * If it doesn't it isn't calculated. (mean walue of min, max latitude and longitudes are used)
   * <p>
   * Transit center is saved in center variable
   * <p>
   * This speeds up calculation, but problem is that median needs to have all of
   * latitudes/longitudes in memory, this can become problematic in large installations. It works
   * without a problem on New York State.
   */
  public void calculateTransitCenter() {
    var vertices = getAllStopVertices();

    // we need this check because there could be only FlexStopLocations (which don't have vertices)
    // in the graph
    if (!vertices.isEmpty()) {
      var medianCalculator = new MedianCalcForDoubles(vertices.size());

      vertices.forEach(v -> medianCalculator.add(v.getLon()));
      double lon = medianCalculator.median();

      medianCalculator.reset();
      vertices.forEach(v -> medianCalculator.add(v.getLat()));
      double lat = medianCalculator.median();

      this.center = new Coordinate(lon, lat);
    }
  }

  public Optional<Coordinate> getCenter() {
    return Optional.ofNullable(center);
  }

  public void addStation(Station station) {
    stationById.put(station.getId(), station);
  }

  public void addMultiModalStation(MultiModalStation multiModalStation) {
    multiModalStationById.put(multiModalStation.getId(), multiModalStation);
  }

  public void addGroupsOfStations(GroupOfStations groupOfStations) {
    groupOfStationsById.put(groupOfStations.getId(), groupOfStations);
  }

  /**
   * Flex locations are generated by GTFS graph builder, but consumed only after the street graph is
   * built
   */
  public FlexStopLocation getLocationById(FeedScopedId id) {
    return locationsById.get(id);
  }
}
