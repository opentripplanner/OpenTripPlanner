package org.opentripplanner.ext.carpooling.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Which trips could serve a passenger at a given point, answered from a grid over the trips'
 * corridor envelopes instead of a scan of every trip.
 * <p>
 * Each trip is registered under the grid cells its leg envelopes cover. A lookup for a point
 * returns the trips registered in the point's cell; the caller checks the exact envelopes and the
 * ellipse itself. Cells are coarse (about 5 km north-south), so a trip covers few cells and a
 * lookup is a handful of hash operations however many trips exist.
 * <p>
 * Thread-safe: the updater registers and removes trips while requests look them up.
 */
final class TripSpatialIndex {

  /** Grid cell size in degrees. */
  static final double CELL_DEGREES = 0.05;

  private final Map<Long, Set<FeedScopedId>> cells = new ConcurrentHashMap<>();
  private final Map<FeedScopedId, List<Long>> cellsByTrip = new ConcurrentHashMap<>();

  /** Registers the trip under every cell its envelopes cover, replacing an earlier registration. */
  void put(FeedScopedId tripId, List<Envelope> envelopes) {
    remove(tripId);
    var keys = new ArrayList<Long>();
    for (var envelope : envelopes) {
      int minLat = cell(envelope.getMinY());
      int maxLat = cell(envelope.getMaxY());
      int minLon = cell(envelope.getMinX());
      int maxLon = cell(envelope.getMaxX());
      for (int lat = minLat; lat <= maxLat; lat++) {
        for (int lon = minLon; lon <= maxLon; lon++) {
          keys.add(key(lat, lon));
        }
      }
    }
    for (var key : keys) {
      cells.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(tripId);
    }
    cellsByTrip.put(tripId, keys);
  }

  /** Forgets the trip. No-op if it is not registered. */
  void remove(FeedScopedId tripId) {
    var keys = cellsByTrip.remove(tripId);
    if (keys == null) {
      return;
    }
    for (var key : keys) {
      var trips = cells.get(key);
      if (trips != null) {
        trips.remove(tripId);
        if (trips.isEmpty()) {
          cells.remove(key, trips);
        }
      }
    }
  }

  /** The trips registered in the cell containing the point: a superset of those whose envelopes contain it. */
  Set<FeedScopedId> near(double lat, double lon) {
    var trips = cells.get(key(cell(lat), cell(lon)));
    return trips == null ? Set.of() : new HashSet<>(trips);
  }

  int size() {
    return cellsByTrip.size();
  }

  private static int cell(double degrees) {
    return (int) Math.floor(degrees / CELL_DEGREES);
  }

  private static long key(int latCell, int lonCell) {
    return ((long) latCell << 32) ^ (lonCell & 0xffffffffL);
  }
}
