package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
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

  private final Map<FeedScopedId, CarpoolTripWithVertices> trips = new ConcurrentHashMap<>();

  /** The earliest instant at which the next expiry sweep is allowed to run. */
  private final AtomicReference<Instant> nextSweep = new AtomicReference<>(Instant.MIN);

  @Override
  public Collection<CarpoolTripWithVertices> getCarpoolTrips() {
    return trips.values();
  }

  @Override
  @Nullable
  public CarpoolTripWithVertices getCarpoolTrip(FeedScopedId id) {
    return trips.get(id);
  }

  @Override
  public void upsertCarpoolTrip(CarpoolTripWithVertices tripWithVertices) {
    CarpoolTrip trip = tripWithVertices.trip();
    CarpoolTripWithVertices existing = trips.put(trip.getId(), tripWithVertices);
    if (existing != null) {
      LOG.debug("Updated carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    } else {
      LOG.debug("Added new carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    }
  }

  @Override
  public void removeCarpoolTrip(FeedScopedId id) {
    CarpoolTripWithVertices removed = trips.remove(id);
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
    for (CarpoolTripWithVertices tripWithVertices : trips.values()) {
      CarpoolTrip trip = tripWithVertices.trip();
      if (
        trip.latestEndTime().toInstant().isBefore(expiryThreshold) &&
        trips.remove(trip.getId(), tripWithVertices)
      ) {
        removed++;
      }
    }
    if (removed > 0) {
      LOG.debug("Removed {} carpool trips that ended before {}", removed, expiryThreshold);
    }
    return removed;
  }
}
