package org.opentripplanner.ext.carpooling.updater;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves incoming carpool trips (street vertices and corridor, ~100 ms each) on a background
 * executor, so that a SIRI poll only parses and delegates, and the updater is primed as soon as
 * the first poll has been read rather than after every trip in the feed has been resolved.
 * <p>
 * A trip is invisible to routing until its resolution is stored. Each submission carries a
 * version, checked before resolving and again before storing, so a newer delivery or a
 * cancellation supersedes whatever is queued or in flight for the trip; the final check and the
 * store are one atomic step with respect to {@link #cancel}, so a cancellation can never be
 * overtaken by a resolution that had already passed the check. The queue also remembers
 * the latest version submitted per trip ({@link #pendingVersion}), so the updater can drop a
 * re-delivery that changes nothing, and how many queued trips are not held yet
 * ({@link #pendingNew}), which is what the trip limit counts.
 * <p>
 * The executor decides the concurrency; production uses a single daemon thread, tests run tasks
 * inline.
 */
final class CarpoolTripResolutionQueue {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolTripResolutionQueue.class);

  private final Executor executor;
  private final Function<CarpoolTrip, CarpoolTripWithVertices> resolver;
  private final CarpoolingRepository repository;
  private final Map<FeedScopedId, Long> currentVersion = new ConcurrentHashMap<>();
  private final Map<FeedScopedId, CarpoolTrip> pendingTrips = new ConcurrentHashMap<>();
  private final Set<FeedScopedId> pendingNew = ConcurrentHashMap.newKeySet();
  private final AtomicLong versions = new AtomicLong();

  /**
   * @param executor runs the resolution tasks
   * @param resolver resolves a trip's vertices and corridor; {@code null} means the trip cannot be
   *        routed and is removed from the repository
   * @param repository receives the resolved trips
   */
  CarpoolTripResolutionQueue(
    Executor executor,
    Function<CarpoolTrip, CarpoolTripWithVertices> resolver,
    CarpoolingRepository repository
  ) {
    this.executor = executor;
    this.resolver = resolver;
    this.repository = repository;
  }

  /** Queues the trip for resolution, superseding any version of it queued or in flight. */
  void submit(CarpoolTrip trip) {
    var id = trip.getId();
    long version = versions.incrementAndGet();
    currentVersion.put(id, version);
    pendingTrips.put(id, trip);
    if (repository.getCarpoolTrip(id) == null) {
      pendingNew.add(id);
    }
    try {
      executor.execute(() -> run(trip, version));
    } catch (RejectedExecutionException e) {
      currentVersion.remove(id, version);
      forget(trip);
      LOG.warn("Carpool trip {} not resolved: the resolver is shut down", id);
    }
  }

  /**
   * Forgets whatever is queued or in flight for the trip; the caller removes it from the
   * repository afterwards. Once this returns, no resolution of the trip that was in flight will be
   * stored.
   */
  synchronized void cancel(FeedScopedId tripId) {
    currentVersion.remove(tripId);
    pendingTrips.remove(tripId);
    pendingNew.remove(tripId);
  }

  /** The latest version of the trip submitted and not yet stored or dropped, or {@code null}. */
  @Nullable
  CarpoolTrip pendingVersion(FeedScopedId tripId) {
    return pendingTrips.get(tripId);
  }

  /** Trips queued or in flight. */
  int pending() {
    return pendingTrips.size();
  }

  /** Trips queued or in flight that the repository does not hold yet. */
  int pendingNew() {
    return pendingNew.size();
  }

  private void run(CarpoolTrip trip, long version) {
    try {
      if (!isCurrent(trip, version)) {
        return;
      }
      var resolved = resolver.apply(trip);
      synchronized (this) {
        if (!isCurrent(trip, version)) {
          return;
        }
        if (resolved == null) {
          repository.removeCarpoolTrip(trip.getId());
        } else {
          repository.upsertCarpoolTrip(resolved);
        }
      }
    } catch (RuntimeException e) {
      LOG.warn("Failed to resolve carpool trip {}", trip.getId(), e);
    } finally {
      if (currentVersion.remove(trip.getId(), version)) {
        forget(trip);
      }
    }
  }

  /** Drops the pending bookkeeping of this version unless a newer one has replaced it. */
  private void forget(CarpoolTrip trip) {
    // Compared by identity: a newer version of the trip has the same id and may compare equal.
    var id = trip.getId();
    if (
      pendingTrips.computeIfPresent(id, (k, current) -> current == trip ? null : current) == null
    ) {
      pendingNew.remove(id);
    }
  }

  private boolean isCurrent(CarpoolTrip trip, long version) {
    Long current = currentVersion.get(trip.getId());
    return current != null && current == version;
  }
}
