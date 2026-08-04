package org.opentripplanner.transit.repository;

import java.time.LocalDate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;

/**
 * The mutable repository for the realtime-updated timetables. It is managed by the transaction
 * framework: trip updaters obtain it through a
 * {@link org.opentripplanner.framework.transaction.api.WriteContext} on the single writer thread,
 * and {@link #createSnapshot()} is called at commit time to publish a new immutable
 * {@link TimetableRepositorySnapshot} for the request threads.
 */
public interface TimetableRepository extends TimetableRepositorySnapshot {
  /**
   * Update the TripTimes of one Trip in a Timetable of a TripPattern. If the Trip of the TripTimes
   * does not exist yet in the Timetable, add it. If the update assigns the trip to a new pattern
   * (the stop pattern was modified in real time), the trip is associated with that pattern for the
   * service date of the update.
   */
  void update(RealTimeTripUpdate realTimeTripUpdate);

  /**
   * Produce an immutable snapshot of the current state of this repository.
   */
  TimetableRepositorySnapshot createSnapshot();

  /**
   * Clear all realtime data for the provided feed id, reverting every trip of that feed to its
   * scheduled timetable and pattern.
   */
  void clear(String feedId);

  /**
   * If a previous realtime update has changed which trip pattern is associated with the given trip
   * on the given service date, dissociate the trip from that pattern and remove the trip's
   * timetables from that pattern on that particular service date.
   *
   * <p>For this service date, the trip will revert to its original trip pattern from the scheduled
   * data, remaining on that pattern unless it's changed again by a future realtime update.
   *
   * @return true if the trip was found to be shifted to a different trip pattern by a realtime
   * message and an attempt was made to re-associate it with its originally scheduled trip pattern.
   */
  boolean revertTripToScheduledTripPattern(FeedScopedId tripId, LocalDate serviceDate);

  /**
   * Remove all realtime data which is valid for a service date on-or-before the one supplied.
   *
   * @return true if any data has been modified and false if no purging has happened.
   */
  boolean purgeExpiredData(LocalDate serviceDate);
}
