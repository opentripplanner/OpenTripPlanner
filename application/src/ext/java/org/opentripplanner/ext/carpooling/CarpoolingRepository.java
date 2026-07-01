package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * Repository for managing carpooling trip ({@link CarpoolTrip}) data.
 * <p>
 * This repository maintains an in-memory index of driver trips.
 *
 * @see CarpoolTrip for trip data model
 * @see org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdater for real-time updates
 */
public interface CarpoolingRepository {
  /**
   * Returns all currently carpooling trips.
   * <p>
   * The returned collection includes all driver trips that have been added via {@link #upsertCarpoolTrip}
   * and not yet removed or expired. The collection is typically used by the routing service to find
   * compatible trips for passengers.
   */
  Collection<CarpoolTrip> getCarpoolTrips();

  /**
   * Inserts a new carpooling trip or updates an existing trip with the same ID.
   * <p>
   * This method is the primary mechanism for adding driver trip data to the repository. It is
   * typically called by real-time updaters when receiving trip information from external systems,
   * or when passenger bookings modify trip capacity.
   *
   * <h3>Validation</h3>
   * <p>
   * The method does not validate trip data beyond basic null checks. It is the caller's
   * responsibility to ensure the trip is valid (has stops, positive capacity, etc.). Invalid
   * trips may cause routing failures later.
   *
   * @param trip the carpool trip to insert or update, must not be null. If a trip with the same
   *        ID exists, it will be completely replaced.
   * @throws IllegalArgumentException if trip is null
   */
  void upsertCarpoolTrip(CarpoolTrip trip);

  /**
   * Removes the carpool trip with the given id. No-op if no trip with this id exists.
   */
  void removeCarpoolTrip(FeedScopedId id);

  /**
   * Removes all carpool trips that have already ended and are therefore no longer routable.
   * <p>
   * A trip is considered expired when its {@link CarpoolTrip#latestEndTime()} is before
   * {@code now.minus(expiry)}. Removing such trips bounds the memory use of instances that run for a
   * long time without restarting, since the SIRI source is not guaranteed to send an explicit
   * cancellation for a journey once it has completed. This mirrors how real-time timetable data is
   * purged once its service date has passed (see {@code TimetableSnapshot#purgeExpiredData}), with
   * the difference that carpool trips carry a concrete instant rather than a service date.
   * <p>
   * The scan is throttled so that it runs at most once per sweep interval no matter how often it is
   * called. The throttle state lives on the repository rather than on the caller, so when several
   * updaters (one per SIRI feed) share a repository, the trips are swept at most once per interval
   * in total instead of once per feed. Calls made before the interval has elapsed are a no-op and
   * return {@code 0}. The method is safe to call concurrently from multiple updater threads.
   *
   * @param now the current instant, used both as the throttle reference and to derive the expiry
   *            cut-off
   * @param expiry how long a trip is retained after its latest end time before it becomes eligible
   *               for removal
   * @return the number of trips removed, or {@code 0} when the call was throttled
   */
  int removeExpiredTrips(Instant now, Duration expiry);

  /**
   * Returns the cached outcome of routing the trip's baseline (its driver waypoints, in order), or
   * {@code null} when there is no usable entry — either nothing is cached, or the cached entry was
   * computed for a different route-point geometry than the trip now has.
   * <p>
   * This is a routing memoization, not part of the trip's domain data: the baseline route depends
   * only on the trip's waypoint geometry and the static street graph, never on the passenger
   * request, so it is computed once and reused across requests. The cache is keyed by trip id and
   * validated against the trip's current route points, so a trip whose geometry changed — including
   * after a concurrent re-route — never reads a stale entry; entries are also dropped when the trip
   * is removed or expires. A successful outcome carries one routed duration per leg
   * ({@code stops().size() - 1} entries); an unroutable outcome carries {@code null} durations (see
   * {@link CachedBaselineRouting}). Returned durations are a copy the caller may not mutate into the
   * cache.
   */
  @Nullable
  CachedBaselineRouting cachedBaselineRouting(CarpoolTrip trip);

  /**
   * Stores the outcome of routing {@code trip}'s baseline, replacing any previous entry. Pass the
   * routed per-leg durations, or {@code null} to record that the baseline is unroutable so later
   * requests skip the trip without re-routing it. Durations are copied, so later mutation by the
   * caller does not affect the cache. The entry is tagged with the trip's current route points and
   * is ignored once those change (see {@link #cachedBaselineRouting}).
   */
  void cacheBaselineRouting(CarpoolTrip trip, @Nullable Duration[] legDurations);

  /**
   * A trip's cached baseline-routing outcome, valid only for the geometry it was routed against. A
   * {@code null} {@link #legDurations()} means the baseline was found unroutable and the trip
   * should be skipped without re-routing; otherwise the array holds one routed travel duration per
   * leg.
   */
  record CachedBaselineRouting(@Nullable Duration[] legDurations) {}
}
