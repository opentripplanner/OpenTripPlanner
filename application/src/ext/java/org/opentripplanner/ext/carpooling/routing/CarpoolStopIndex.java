package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
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
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The transit stops a carpool can serve, seen from the drivable network.
 * <p>
 * A passenger is dropped off for a stop at a vertex a car can reach and leave, then walks to the
 * stop; picked up likewise after walking from the stop to such a vertex. Both snaps depend only on
 * the static street graph, so they are computed once per stop, on first use, and shared by every
 * request; the walk is timed with default street preferences, a request replays it with its own
 * (see {@link org.opentripplanner.ext.carpooling.util.GraphPathUtils#replay}). The index also
 * finds the stops inside an envelope, from a grid over their coordinates, which the corridor
 * computation uses.
 * <p>
 * Thread-safe: requests and the updater share one instance.
 */
public class CarpoolStopIndex {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolStopIndex.class);

  /**
   * Walk budget of the snap searches. The snap is the nearest car-reachable vertex whatever the
   * budget, so it does not depend on a request's maximum walk; a request with a smaller maximum
   * simply drops the stops whose walk exceeds it. Stops farther than this from any drivable
   * street are not served by carpool.
   */
  public static final Duration MAX_STOP_WALK = Duration.ofMinutes(15);

  /** Grid cell size in degrees, roughly 2 km north-south. */
  private static final double CELL_DEGREES = 0.02;

  private final CarReachableVertexSnapper snapper;
  private final Map<FeedScopedId, TransitStopVertex> stops = new HashMap<>();
  private final Map<Long, List<TransitStopVertex>> grid = new HashMap<>();
  private final Map<FeedScopedId, Optional<SnapResult>> dropoffSnaps = new ConcurrentHashMap<>();
  private final Map<FeedScopedId, Optional<SnapResult>> pickupSnaps = new ConcurrentHashMap<>();

  public CarpoolStopIndex(Graph graph, CarReachableVertexSnapper snapper) {
    this.snapper = snapper;
    for (var vertex : graph.getVerticesOfType(TransitStopVertex.class)) {
      stops.put(vertex.getId(), vertex);
      grid
        .computeIfAbsent(cellKey(vertex.getLat(), vertex.getLon()), k -> new ArrayList<>())
        .add(vertex);
    }
    LOG.info("Indexed {} transit stops for carpool", stops.size());
  }

  /** The stops whose coordinates lie inside {@code envelope}. */
  public List<TransitStopVertex> stopsWithin(Envelope envelope) {
    var result = new ArrayList<TransitStopVertex>();
    for (int lat = cell(envelope.getMinY()); lat <= cell(envelope.getMaxY()); lat++) {
      for (int lon = cell(envelope.getMinX()); lon <= cell(envelope.getMaxX()); lon++) {
        for (var stop : grid.getOrDefault(key(lat, lon), List.of())) {
          if (envelope.contains(stop.getLon(), stop.getLat())) {
            result.add(stop);
          }
        }
      }
    }
    return result;
  }

  /**
   * Where a driver drops a passenger off for the stop: a car-reachable vertex within
   * {@link #MAX_STOP_WALK} of it, with the walk from the vertex to the stop. {@code null} if the
   * stop is unknown or there is none.
   */
  @Nullable
  public SnapResult dropoffSnap(FeedScopedId stopId) {
    return snap(dropoffSnaps, stopId, true);
  }

  /**
   * Where a driver picks up a passenger coming from the stop: a car-reachable vertex within
   * {@link #MAX_STOP_WALK} of it, with the walk from the stop to the vertex. {@code null} if the
   * stop is unknown or there is none.
   */
  @Nullable
  public SnapResult pickupSnap(FeedScopedId stopId) {
    return snap(pickupSnaps, stopId, false);
  }

  /** Number of stops in the index. */
  public int size() {
    return stops.size();
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
    return cache
      .computeIfAbsent(stopId, id ->
        Optional.ofNullable(
          dropoff
            ? snapper.snapDropoff(StreetSearchRequest.DEFAULT, stop, MAX_STOP_WALK)
            : snapper.snapPickup(StreetSearchRequest.DEFAULT, stop, MAX_STOP_WALK)
        )
      )
      .orElse(null);
  }

  private static long cellKey(double lat, double lon) {
    return key(cell(lat), cell(lon));
  }

  private static int cell(double degrees) {
    return (int) Math.floor(degrees / CELL_DEGREES);
  }

  private static long key(int latCell, int lonCell) {
    return ((long) latCell << 32) ^ (lonCell & 0xffffffffL);
  }
}
