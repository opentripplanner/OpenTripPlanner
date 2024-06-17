package org.opentripplanner.updater.trip;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.CountdownTimer;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class which abstracts away locking, updating, committing and purging of the timetable snapshot.
 * In order to keep code reviews easier this is an intermediate stage and will be refactored further.
 * In particular the following refactorings are planned:
 * <p>
 * - create only one "snapshot manager" per transit model that is shared between Siri/GTFS-RT updaters
 */
public final class TimetableSnapshotManager {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshotManager.class);
  private final TransitLayerUpdater transitLayerUpdater;
  /**
   * Lock to indicate that buffer is in use
   */
  private final ReentrantLock bufferLock = new ReentrantLock(true);

  /**
   * The working copy of the timetable snapshot. Should not be visible to routing threads. Should
   * only be modified by a thread that holds a lock on {@link #bufferLock}. All public methods that
   * might modify this buffer will correctly acquire the lock.
   */
  private final TimetableSnapshot buffer = new TimetableSnapshot();

  /**
   * The working copy of the timetable snapshot. Should not be visible to routing threads. Should
   * only be modified by a thread that holds a lock on {@link #bufferLock}. All public methods that
   * might modify this buffer will correctly acquire the lock. By design, only one thread should
   * ever be writing to this buffer.
   * TODO RT_AB: research and document why this lock is needed since only one thread should ever be
   *   writing to this buffer. One possible reason may be a need to suspend writes while indexing
   *   and swapping out the buffer. But the original idea was to make a new copy of the buffer
   *   before re-indexing it. While refactoring or rewriting parts of this system, we could throw
   *   an exception if a writing section is entered by more than one thread.
   */
  private volatile TimetableSnapshot snapshot = null;

  /**
   * If a timetable snapshot is requested less than this number of milliseconds after the previous
   * snapshot, just return the same one. Throttles the potentially resource-consuming task of
   * duplicating a TripPattern -> Timetable map and indexing the new Timetables.
   */
  private final CountdownTimer snapshotFrequencyThrottle;

  /**
   * Should expired real-time data be purged from the graph.
   * TODO RT_AB: Clarify exactly what "purge" means and in what circumstances would one turn it off.
   */
  private final boolean purgeExpiredData;
  /**
   * We inject a provider to retrieve the current service-date(now). This enables us to unit-test
   * the purgeExpiredData feature.
   */
  private final Supplier<LocalDate> localDateNow;

  private LocalDate lastPurgeDate = null;

  /**
   *
   * @param localDateNow This supplier allows you to inject a custom lambda to override what is
   *                     considered 'today'. This is useful for unit testing.
   */
  public TimetableSnapshotManager(
    TransitLayerUpdater transitLayerUpdater,
    TimetableSnapshotSourceParameters parameters,
    Supplier<LocalDate> localDateNow
  ) {
    this.transitLayerUpdater = transitLayerUpdater;
    this.snapshotFrequencyThrottle = new CountdownTimer(parameters.maxSnapshotFrequency());
    this.purgeExpiredData = parameters.purgeExpiredData();
    this.localDateNow = Objects.requireNonNull(localDateNow);
    // Force commit so that snapshot initializes
    commitTimetableSnapshot(true);
  }

  /**
   * @return an up-to-date snapshot mapping TripPatterns to Timetables. This snapshot and the
   * timetable objects it references are guaranteed to never change, so the requesting thread is
   * provided a consistent view of all TripTimes. The routing thread need only release its reference
   * to the snapshot to release resources.
   */
  public TimetableSnapshot getTimetableSnapshot() {
    // Try to get a lock on the buffer
    if (bufferLock.tryLock()) {
      // Make a new snapshot if necessary
      try {
        commitTimetableSnapshot(false);
        return snapshot;
      } finally {
        bufferLock.unlock();
      }
    }
    // No lock could be obtained because there is either a snapshot commit busy or updates
    // are applied at this moment, just return the current snapshot
    return snapshot;
  }

  /**
   * Request a commit of the timetable snapshot.
   * <p>
   * If there are no updates buffered up or not enough time has elapsed, the existing snapshot
   * is returned.
   *
   * @param force Force the committing of a new snapshot even if the above conditions are not met.
   */
  public void commitTimetableSnapshot(final boolean force) {
    if (force || snapshotFrequencyThrottle.timeIsUp()) {
      if (force || buffer.isDirty()) {
        LOG.debug("Committing {}", buffer);
        snapshot = buffer.commit(transitLayerUpdater, force);

        // We only reset the timer when the snapshot is updated. This will cause the first
        // update to be committed after a silent period. This should not have any effect in
        // a busy updater. It is however useful when manually testing the updater.
        snapshotFrequencyThrottle.restart();
      } else {
        LOG.debug("Buffer was unchanged, keeping old snapshot.");
      }
    } else {
      LOG.debug("Snapshot frequency exceeded. Reusing snapshot {}", snapshot);
    }
  }

  /**
   * Get the current trip pattern given a trip id and a service date, if it has been changed from
   * the scheduled pattern with an update, for which the stopPattern is different.
   *
   * @param tripId trip id
   * @param serviceDate service date
   * @return trip pattern created by the updater; null if pattern has not been changed for this trip.
   */
  @Nullable
  public TripPattern getRealtimeAddedTripPattern(FeedScopedId tripId, LocalDate serviceDate) {
    return buffer.getRealtimeAddedTripPattern(tripId, serviceDate);
  }

  /**
   * Make a snapshot after each message in anticipation of incoming requests.
   * Purge data if necessary (and force new snapshot if anything was purged).
   * Make sure that the public (locking) getTimetableSnapshot function is not called.
   */
  public void purgeAndCommit() {
    if (purgeExpiredData) {
      final boolean modified = purgeExpiredData();
      commitTimetableSnapshot(modified);
    } else {
      commitTimetableSnapshot(false);
    }
  }

  /**
   * If a previous realtime update has changed which trip pattern is associated with the given trip
   * on the given service date, this method will dissociate the trip from that pattern and remove
   * the trip's timetables from that pattern on that particular service date.
   * <p>
   * For this service date, the trip will revert to its original trip pattern from the scheduled
   * data, remaining on that pattern unless it's changed again by a future realtime update.
   */
  public void revertTripToScheduledTripPattern(FeedScopedId tripId, LocalDate serviceDate) {
    buffer.revertTripToScheduledTripPattern(tripId, serviceDate);
  }

  /**
   * Remove realtime data from previous service dates from the snapshot. This is useful so that
   * instances that run for multiple days don't accumulate a lot of realtime data for past
   * dates which would increase memory consumption.
   * If your OTP instances are restarted throughout the day, this is less useful and can be
   * turned off.
   *
   * @return true if any data has been modified and false if no purging has happened.
   */
  private boolean purgeExpiredData() {
    final LocalDate today = localDateNow.get();
    // TODO: Base this on numberOfDaysOfLongestTrip for tripPatterns
    final LocalDate previously = today.minusDays(2); // Just to be safe...

    // Purge data only if we have changed date
    if (lastPurgeDate != null && lastPurgeDate.compareTo(previously) >= 0) {
      return false;
    }

    LOG.debug("Purging expired realtime data");

    lastPurgeDate = previously;

    return buffer.purgeExpiredData(previously);
  }

  /**
   * Execute a {@code Runnable} with a locked snapshot buffer and release the lock afterwards. While
   * the action of locking and unlocking is not complicated to do for calling code, this method
   * exists so that the lock instance is a private field.
   */
  public void withLock(Runnable action) {
    bufferLock.lock();

    try {
      action.run();
    } finally {
      // Always release lock
      bufferLock.unlock();
    }
  }

  /**
   * Clear all data of snapshot for the provided feed id
   */
  public void clearBuffer(String feedId) {
    buffer.clear(feedId);
  }

  /**
   * Update the TripTimes of one Trip in a Timetable of a TripPattern. If the Trip of the TripTimes
   * does not exist yet in the Timetable, add it. This method will make a protective copy
   * of the Timetable if such a copy has not already been made while building up this snapshot,
   * handling both cases where patterns were pre-existing in static data or created by realtime data.
   *
   * @param serviceDate service day for which this update is valid
   * @return whether the update was actually applied
   */
  public Result<UpdateSuccess, UpdateError> updateBuffer(
    TripPattern pattern,
    TripTimes tripTimes,
    LocalDate serviceDate
  ) {
    return buffer.update(pattern, tripTimes, serviceDate);
  }

  /**
   * Returns an updated timetable for the specified pattern if one is available in this snapshot, or
   * the originally scheduled timetable if there are no updates in this snapshot.
   */
  public Timetable resolve(TripPattern pattern, LocalDate serviceDate) {
    return buffer.resolve(pattern, serviceDate);
  }
}
