package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.corridor.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * Holds the driver trips ({@link CarpoolTrip}) in memory, each with its corridor
 * ({@link RoutableCarpoolTrip}).
 *
 * @see org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdater for real-time updates
 */
public interface CarpoolingRepository {
  /** Returns the current trips: those upserted and neither removed nor expired. */
  Collection<RoutableCarpoolTrip> getCarpoolTrips();

  /**
   * Returns the trip with the given id, or {@code null} when no such trip exists.
   */
  @Nullable
  RoutableCarpoolTrip getCarpoolTrip(FeedScopedId id);

  /** Returns the number of trips from the given feed. */
  int countCarpoolTrips(String feedId);

  /**
   * Inserts the trip, replacing any trip with the same id. The trip is not validated: the caller
   * makes sure that it is valid and that its corridor is that of its current route points.
   */
  void upsertCarpoolTrip(RoutableCarpoolTrip trip);

  /**
   * Removes the carpool trip with the given id. No-op if no trip with this id exists.
   */
  void removeCarpoolTrip(FeedScopedId id);

  /**
   * Removes the trips whose {@link CarpoolTrip#latestEndTime()} is more than {@code expiry} before
   * {@code now}. The SIRI source is not guaranteed to cancel a journey once it has completed, so
   * this bounds the memory of an instance that runs for a long time.
   * <p>
   * The trips are swept at most once per sweep interval, however often this is called and by
   * however many updaters sharing the repository; a call in between does nothing and returns
   * {@code 0}. The method is safe to call concurrently from multiple updater threads.
   *
   * @return the number of trips removed
   */
  int removeExpiredTrips(Instant now, Duration expiry);
}
