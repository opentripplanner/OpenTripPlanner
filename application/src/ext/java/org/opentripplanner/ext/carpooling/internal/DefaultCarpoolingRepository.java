package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCarpoolingRepository implements CarpoolingRepository {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingRepository.class);

  /**
   * Minimum time between expiry sweeps. Trips expire on a multi-day timescale, so scanning the
   * repository on every poll is wasteful; sweeping at most this often keeps the work negligible
   * regardless of how short — or how many — the polling feeds are.
   */
  private static final Duration SWEEP_INTERVAL = Duration.ofHours(1);

  private final Map<FeedScopedId, RoutableCarpoolTrip> trips = new ConcurrentHashMap<>();

  /** The earliest instant at which the next expiry sweep is allowed to run. */
  private final AtomicReference<Instant> nextSweep = new AtomicReference<>(Instant.MIN);

  /**
   * Trips by the grid cells their corridor envelopes cover, kept in lockstep with {@link #trips}:
   * registered when stored with a corridor, forgotten when replaced, removed or expired.
   */
  private final TripSpatialIndex spatialIndex = new TripSpatialIndex();

  @Override
  public Collection<RoutableCarpoolTrip> getCarpoolTrips() {
    return trips.values();
  }

  @Override
  @Nullable
  public RoutableCarpoolTrip getCarpoolTrip(FeedScopedId id) {
    return trips.get(id);
  }

  @Override
  public Collection<RoutableCarpoolTrip> getCarpoolTripsNear(WgsCoordinate point) {
    var result = new ArrayList<RoutableCarpoolTrip>();
    for (var id : spatialIndex.near(point.latitude(), point.longitude())) {
      var trip = trips.get(id);
      if (trip == null) {
        continue;
      }
      for (var envelope : trip.corridor().legEnvelopes()) {
        if (envelope.contains(point.longitude(), point.latitude())) {
          result.add(trip);
          break;
        }
      }
    }
    return result;
  }

  @Override
  public void upsertCarpoolTrip(RoutableCarpoolTrip routableTrip) {
    CarpoolTrip trip = routableTrip.trip();
    RoutableCarpoolTrip existing = trips.put(trip.getId(), routableTrip);
    spatialIndex.put(trip.getId(), routableTrip.corridor().legEnvelopes());
    if (existing != null) {
      LOG.debug("Updated carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    } else {
      LOG.debug("Added new carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    }
  }

  @Override
  public void removeCarpoolTrip(FeedScopedId id) {
    RoutableCarpoolTrip removed = trips.remove(id);
    spatialIndex.remove(id);
    if (removed != null) {
      LOG.debug("Removed carpool trip {}", id);
    } else {
      LOG.debug("Tried to remove unknown carpool trip {}", id);
    }
  }

  @Override
  public int removeExpiredTrips(Instant now, Duration expiry) {
    Instant allowedAt = nextSweep.get();
    if (now.isBefore(allowedAt) || !nextSweep.compareAndSet(allowedAt, now.plus(SWEEP_INTERVAL))) {
      return 0;
    }

    Instant expiryThreshold = now.minus(expiry);
    int removed = 0;
    for (RoutableCarpoolTrip routableTrip : trips.values()) {
      CarpoolTrip trip = routableTrip.trip();
      if (
        trip.latestEndTime().toInstant().isBefore(expiryThreshold) &&
        trips.remove(trip.getId(), routableTrip)
      ) {
        spatialIndex.remove(trip.getId());
        removed++;
      }
    }
    if (removed > 0) {
      LOG.debug("Removed {} carpool trips that ended before {}", removed, expiryThreshold);
    }
    return removed;
  }
}
