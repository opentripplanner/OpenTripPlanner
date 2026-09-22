package org.opentripplanner.ext.carpooling.updater;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a feed's carpool trip changes on a background executor, so that a SIRI poll only parses
 * and delegates, and the updater is primed as soon as the first poll has been read rather than
 * after every trip in the feed has been resolved.
 * <p>
 * A change is a trip to resolve (street vertices and corridor, ~100 ms) and store, or a trip to
 * remove; a trip is invisible to routing until its resolution is stored. Only a trip's latest
 * change waits: one queued while an earlier change of the same trip is still waiting replaces it,
 * so the queue holds at most one change per trip however often the feed changes it. The executor
 * applies the changes one at a time, so the repository always ends up with the latest change of
 * every trip. Production uses a single daemon thread, tests run the changes inline.
 */
final class CarpoolTripResolutionQueue {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolTripResolutionQueue.class);

  private final Executor executor;
  private final Function<CarpoolTrip, RoutableCarpoolTrip> resolver;
  private final CarpoolingRepository repository;
  /** The latest change of each trip waiting to be applied; an empty one is a removal. */
  private final Map<FeedScopedId, Optional<CarpoolTrip>> pending = new ConcurrentHashMap<>();

  /**
   * @param executor runs the changes, one at a time and in the order they were queued
   * @param resolver resolves a trip's vertices and corridor; {@code null} means the trip cannot be
   *        routed and is removed from the repository
   * @param repository receives the changes
   */
  CarpoolTripResolutionQueue(
    Executor executor,
    Function<CarpoolTrip, RoutableCarpoolTrip> resolver,
    CarpoolingRepository repository
  ) {
    this.executor = executor;
    this.resolver = resolver;
    this.repository = repository;
  }

  /** Queues the trip to be resolved and stored. */
  void submit(CarpoolTrip trip) {
    enqueue(trip.getId(), Optional.of(trip));
  }

  /** Queues the removal of the trip. */
  void remove(FeedScopedId tripId) {
    enqueue(tripId, Optional.empty());
  }

  private void enqueue(FeedScopedId tripId, Optional<CarpoolTrip> change) {
    if (pending.put(tripId, change) != null) {
      // The task queued for the earlier change applies this one instead.
      return;
    }
    try {
      executor.execute(() -> apply(tripId));
    } catch (RejectedExecutionException e) {
      pending.remove(tripId);
      LOG.warn("Carpool trip {} not updated: the resolver is shut down", tripId);
    }
  }

  /**
   * Stores the resolution of the trip's latest change, or removes the trip if that change is a
   * removal, or the trip cannot be routed or fails to resolve: an older version must not outlive a
   * change it could not take. A change queued from here on gets a task of its own.
   */
  private void apply(FeedScopedId tripId) {
    var trip = pending.remove(tripId).orElse(null);
    try {
      var resolved = trip == null ? null : resolver.apply(trip);
      if (resolved == null) {
        repository.removeCarpoolTrip(tripId);
      } else {
        repository.upsertCarpoolTrip(resolved);
      }
    } catch (RuntimeException e) {
      LOG.warn("Failed to update carpool trip {}, removing it", tripId, e);
      repository.removeCarpoolTrip(tripId);
    }
  }
}
