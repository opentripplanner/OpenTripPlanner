package org.opentripplanner.updater.trip;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RealTimeRaptorTransitDataUpdater;
import org.opentripplanner.routing.util.ConcurrentPublished;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class which abstracts away locking, updating, committing and purging of the timetable snapshot.
 */
public final class TimetableSnapshotManager {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshotManager.class);
  private final RealTimeRaptorTransitDataUpdater realtimeRaptorTransitDataUpdater;

  /**
   * The working copy of the timetable snapshot. Should not be visible to routing threads.
   * By design, only one thread should ever be writing to this buffer.
   */
  private final TimetableSnapshot buffer = new TimetableSnapshot();

  /**
   * The last committed snapshot that was handed off to a routing thread. This snapshot may be given
   * to more than one routing thread.
   */
  private final ConcurrentPublished<TimetableSnapshot> snapshot = new ConcurrentPublished<>();

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
    @Nullable RealTimeRaptorTransitDataUpdater realtimeRaptorTransitDataUpdater,
    TimetableSnapshotParameters parameters,
    Supplier<LocalDate> localDateNow
  ) {
    this.realtimeRaptorTransitDataUpdater = realtimeRaptorTransitDataUpdater;
    this.purgeExpiredData = parameters.purgeExpiredData();
    this.localDateNow = Objects.requireNonNull(localDateNow);
    // Force commit so that snapshot initializes
    commitTimetableSnapshot(true);
  }

  /**
   * @return an up-to-date snapshot of real-time data. This snapshot and the timetable objects it
   * references are guaranteed to never change, so the requesting thread is
   * provided a consistent view of all TripTimes. The routing thread need only release its reference
   * to the snapshot to release resources.
   */
  public TimetableSnapshot getTimetableSnapshot() {
    return snapshot.get();
  }

  /**
   * @return the current timetable snapshot buffer that contains pending changes (not yet published
   * in a snapshot).
   * This should be used in the context of an updater to build a TransitEditorService that sees all
   * the changes applied so far by real-time updates.
   */
  public TimetableSnapshot getTimetableSnapshotBuffer() {
    return buffer;
  }

  /**
   * Request a commit of the timetable snapshot.
   * <p>
   * If there are no updates buffered up or not enough time has elapsed, the existing snapshot
   * is returned.
   *
   * @param force Force the committing of a new snapshot even if the above conditions are not met.
   */
  void commitTimetableSnapshot(final boolean force) {
    if (force || buffer.isDirty()) {
      LOG.debug("Committing {}", buffer);
      snapshot.publish(buffer.commit(realtimeRaptorTransitDataUpdater, force));
    } else {
      LOG.debug("Buffer was unchanged, keeping old snapshot.");
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
  public TripPattern getNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate) {
    return buffer.getNewTripPatternForModifiedTrip(tripId, serviceDate);
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
   * Clear all data of snapshot for the provided feed id
   */
  public void clearBuffer(String feedId) {
    buffer.clear(feedId);
  }

  /**
   * Update the TripTimes of one Trip in a Timetable of a TripPattern. If the Trip of the TripTimes
   * does not exist yet in the Timetable, add it. This method will make a protective copy of the
   * Timetable if such a copy has not already been made while building up this snapshot, handling
   * both cases where patterns were pre-existing in static data or created by realtime data.
   *
   * @return whether the update was actually applied
   */
  public Result<UpdateSuccess, UpdateError> updateBuffer(RealTimeTripUpdate realTimeTripUpdate) {
    return buffer.update(realTimeTripUpdate);
  }

  /**
   * Returns an updated timetable for the specified pattern if one is available in this snapshot, or
   * the originally scheduled timetable if there are no updates in this snapshot.
   */
  public Timetable resolve(TripPattern pattern, LocalDate serviceDate) {
    return buffer.resolve(pattern, serviceDate);
  }
}
