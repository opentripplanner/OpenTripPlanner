package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.street.geometry.HashGridSpatialIndex;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The transit stops a carpool can serve, seen from the drivable network.
 * <p>
 * A passenger is dropped off for a stop at a vertex a car can reach and leave, then walks to the
 * stop; picked up likewise after walking from the stop to such a vertex. The index also finds the
 * stops inside an envelope, from a grid over their coordinates, which the corridor computation
 * uses.
 * <p>
 * Thread-safe: requests and the updater share one instance.
 */
public class CarpoolStopIndex {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolStopIndex.class);

  /** Grid cell size in degrees, roughly 2 km north-south. */
  private static final double CELL_DEGREES = 0.02;

  private final CarReachableVertexSnapper snapper;
  private final Duration maxStopWalk;
  private final Map<FeedScopedId, TransitStopVertex> stops = new HashMap<>();
  private final HashGridSpatialIndex<TransitStopVertex> grid = new HashGridSpatialIndex<>(
    CELL_DEGREES,
    CELL_DEGREES
  );
  private final Map<FeedScopedId, Optional<SnapResult>> dropoffSnaps = new ConcurrentHashMap<>();
  private final Map<FeedScopedId, Optional<SnapResult>> pickupSnaps = new ConcurrentHashMap<>();

  public CarpoolStopIndex(Graph graph, CarReachableVertexSnapper snapper, Duration maxStopWalk) {
    this.snapper = snapper;
    this.maxStopWalk = maxStopWalk;
    for (var vertex : graph.findVertices(TransitStopVertex.class)) {
      stops.put(vertex.getId(), vertex);
      grid.insert(new Envelope(vertex.getCoordinate()), vertex);
    }
    LOG.info("Indexed {} transit stops for carpool", stops.size());
  }

  /**
   * The stops whose snaps can lie inside {@code envelope}: those within the distance of the longest
   * stop walk of it.
   */
  public List<TransitStopVertex> stopsWithin(Envelope envelope) {
    double walkMeters = walkMeters();
    double poleSideLat = Math.max(Math.abs(envelope.getMinY()), Math.abs(envelope.getMaxY()));
    var area = new Envelope(envelope);
    area.expandBy(
      SphericalDistanceLibrary.metersToLonDegrees(walkMeters, poleSideLat),
      SphericalDistanceLibrary.metersToDegrees(walkMeters)
    );
    return grid
      .query(area)
      .stream()
      .filter(stop -> area.contains(stop.getLon(), stop.getLat()))
      .sorted(Comparator.comparing(TransitStopVertex::getId))
      .toList();
  }

  /** How far a stop can be from its snap: the longest stop walk, at the default walk speed. */
  double walkMeters() {
    return maxStopWalk.toSeconds() * StreetSearchRequest.DEFAULT.walk().speed();
  }

  /**
   * Where a driver drops a passenger off for the stop: a car-reachable vertex within the walk
   * budget of it, with the walk from the vertex to the stop. {@code null} if the stop is unknown or
   * there is none.
   */
  @Nullable
  public SnapResult dropoffSnap(FeedScopedId stopId) {
    return snap(dropoffSnaps, stopId, true);
  }

  /**
   * Where a driver picks up a passenger coming from the stop: a car-reachable vertex within the
   * walk budget of it, with the walk from the stop to the vertex. {@code null} if the stop is
   * unknown or there is none.
   */
  @Nullable
  public SnapResult pickupSnap(FeedScopedId stopId) {
    return snap(pickupSnaps, stopId, false);
  }

  @Nullable
  private SnapResult snap(
    Map<FeedScopedId, Optional<SnapResult>> cache,
    FeedScopedId stopId,
    boolean dropoff
  ) {
    var stop = stops.get(stopId);
    if (stop == null) {
      return null;
    }
    var cached = cache.get(stopId);
    if (cached == null) {
      // Searched outside the map, which would otherwise block other threads for its duration;
      // two threads may search the same stop at once, and both find the same snap.
      var snap = Optional.ofNullable(
        dropoff
          ? snapper.snapDropoff(StreetSearchRequest.DEFAULT, stop, maxStopWalk)
          : snapper.snapPickup(StreetSearchRequest.DEFAULT, stop, maxStopWalk)
      );
      var raced = cache.putIfAbsent(stopId, snap);
      cached = raced == null ? snap : raced;
    }
    return cached.orElse(null);
  }
}
