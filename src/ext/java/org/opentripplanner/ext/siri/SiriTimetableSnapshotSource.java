package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_MONITORED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.CountdownTimer;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVehicleJourney;

/**
 * This class should be used to create snapshots of lookup tables of real-time data. This is
 * necessary to provide planning threads a consistent constant view of a graph with real-time data at
 * a specific point in time.
 */
public class SiriTimetableSnapshotSource implements TimetableSnapshotProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SiriTimetableSnapshotSource.class);

  /**
   * The working copy of the timetable snapshot. Should not be visible to routing threads. Should
   * only be modified by a thread that holds a lock on {@link #bufferLock}. All public methods that
   * might modify this buffer will correctly acquire the lock.
   */
  private final TimetableSnapshot buffer = new TimetableSnapshot();
  /**
   * Lock to indicate that buffer is in use
   */
  private final ReentrantLock bufferLock = new ReentrantLock(true);
  /**
   * Use a id generator to generate TripPattern ids for new TripPatterns created by RealTime
   * updates.
   */
  private final SiriTripPatternIdGenerator tripPatternIdGenerator = new SiriTripPatternIdGenerator();
  /**
   * A synchronized cache of trip patterns that are added to the graph due to GTFS-real-time
   * messages.
   */
  private final SiriTripPatternCache tripPatternCache;
  private final TransitModel transitModel;

  private final TransitService transitService;
  private final TransitLayerUpdater transitLayerUpdater;

  /**
   * If a timetable snapshot is requested less than this number of milliseconds after the previous
   * snapshot, just return the same one. Throttles the potentially resource-consuming task of
   * duplicating a TripPattern -> Timetable map and indexing the new Timetables.
   */
  protected CountdownTimer snapshotFrequencyThrottle;

  /**
   * The last committed snapshot that was handed off to a routing thread. This snapshot may be given
   * to more than one routing thread if the maximum snapshot frequency is exceeded.
   */
  private volatile TimetableSnapshot snapshot = null;

  /** Should expired real-time data be purged from the graph. */
  private final boolean purgeExpiredData;

  protected LocalDate lastPurgeDate = null;

  public SiriTimetableSnapshotSource(
    TimetableSnapshotSourceParameters parameters,
    TransitModel transitModel
  ) {
    this.transitModel = transitModel;
    this.transitService = new DefaultTransitService(transitModel);
    this.transitLayerUpdater = transitModel.getTransitLayerUpdater();
    this.snapshotFrequencyThrottle = new CountdownTimer(parameters.maxSnapshotFrequency());
    this.purgeExpiredData = parameters.purgeExpiredData();
    this.tripPatternCache =
      new SiriTripPatternCache(tripPatternIdGenerator, transitService::getPatternForTrip);

    transitModel.initTimetableSnapshotProvider(this);

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
    TimetableSnapshot snapshotToReturn;

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
   * Method to apply a trip update list to the most recent version of the timetable snapshot.
   *
   * @param fullDataset  true iff the list with updates represent all updates that are active right
   *                     now, i.e. all previous updates should be disregarded
   * @param updates      SIRI VehicleMonitoringDeliveries that should be applied atomically
   */
  public UpdateResult applyEstimatedTimetable(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    String feedId,
    boolean fullDataset,
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    if (updates == null) {
      LOG.warn("updates is null");
      return UpdateResult.empty();
    }

    // Acquire lock on buffer
    bufferLock.lock();

    List<Result<UpdateSuccess, UpdateError>> results = new ArrayList<>();

    try {
      if (fullDataset) {
        // Remove all updates from the buffer
        buffer.clear(feedId);
      }

      for (var etDelivery : updates) {
        for (var estimatedJourneyVersion : etDelivery.getEstimatedJourneyVersionFrames()) {
          var journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
          LOG.debug("Handling {} EstimatedVehicleJourneys.", journeys.size());
          for (EstimatedVehicleJourney journey : journeys) {
            results.add(apply(journey, transitModel, fuzzyTripMatcher, entityResolver));
          }
        }
      }

      LOG.debug("message contains {} trip updates", updates.size());

      // Make a snapshot after each message in anticipation of incoming requests
      // Purge data if necessary (and force new snapshot if anything was purged)
      // Make sure that the public (locking) getTimetableSnapshot function is not called.
      if (purgeExpiredData) {
        final boolean modified = purgeExpiredData();
        commitTimetableSnapshot(modified);
      } else {
        commitTimetableSnapshot(false);
      }
    } finally {
      // Always release lock
      bufferLock.unlock();
    }
    return UpdateResult.ofResults(results);
  }

  private Result<UpdateSuccess, UpdateError> apply(
    EstimatedVehicleJourney journey,
    TransitModel transitModel,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver
  ) {
    boolean shouldAddNewTrip = false;
    try {
      shouldAddNewTrip = shouldAddNewTrip(journey, entityResolver);
      Result<TripUpdate, UpdateError> result;
      if (shouldAddNewTrip) {
        result =
          new AddedTripBuilder(
            journey,
            transitModel,
            entityResolver,
            tripPatternIdGenerator::generateUniqueTripPatternId
          )
            .build();
      } else {
        result = handleModifiedTrip(fuzzyTripMatcher, entityResolver, journey);
      }

      if (result.isFailure()) {
        return result.toFailureResult();
      }

      /* commit */
      return addTripToGraphAndBuffer(result.successValue());
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e);
    } catch (Exception e) {
      LOG.warn(
        "{} EstimatedJourney {} failed.",
        shouldAddNewTrip ? "Adding" : "Updating",
        DebugString.of(journey),
        e
      );
      return Result.failure(UpdateError.noTripId(UNKNOWN));
    }
  }

  /**
   * Check if VehicleJourney is a replacement departure according to SIRI-ET requirements.
   */
  private boolean shouldAddNewTrip(
    EstimatedVehicleJourney vehicleJourney,
    EntityResolver entityResolver
  ) {
    // Replacement departure only if ExtraJourney is true
    if (!(TRUE.equals(vehicleJourney.isExtraJourney()))) {
      return false;
    }

    // And if the trip has not been added before
    return entityResolver.resolveTrip(vehicleJourney) == null;
  }

  private void commitTimetableSnapshot(final boolean force) {
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
   * Get the latest timetable for TripPattern for a given service date.
   * <p>
   * Snapshot timetable is used as source if initialised, trip patterns scheduled timetable if not.
   */
  private Timetable getCurrentTimetable(TripPattern tripPattern, LocalDate serviceDate) {
    TimetableSnapshot timetableSnapshot = snapshot;
    if (timetableSnapshot != null) {
      return timetableSnapshot.resolve(tripPattern, serviceDate);
    }
    return tripPattern.getScheduledTimetable();
  }

  private Result<TripUpdate, UpdateError> handleModifiedTrip(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    Trip trip = entityResolver.resolveTrip(estimatedVehicleJourney);

    // Check if EstimatedVehicleJourney is reported as NOT monitored, ignore the notMonitored-flag
    // if the journey is NOT monitored because it has been cancelled
    if (
      !TRUE.equals(estimatedVehicleJourney.isMonitored()) &&
      !TRUE.equals(estimatedVehicleJourney.isCancellation())
    ) {
      return UpdateError.result(trip != null ? trip.getId() : null, NOT_MONITORED);
    }

    LocalDate serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);

    if (serviceDate == null) {
      return UpdateError.result(trip != null ? trip.getId() : null, NO_START_DATE);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitService.getPatternForTrip(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      TripAndPattern tripAndPattern = fuzzyTripMatcher.match(
        estimatedVehicleJourney,
        entityResolver,
        this::getCurrentTimetable,
        buffer::getRealtimeAddedTripPattern
      );

      if (tripAndPattern == null) {
        LOG.debug(
          "No trips found for EstimatedVehicleJourney. {}",
          DebugString.of(estimatedVehicleJourney)
        );
        return UpdateError.result(null, NO_FUZZY_TRIP_MATCH);
      }

      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      return UpdateError.result(null, TRIP_NOT_FOUND);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      return UpdateError.result(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN);
    }
    var updateResult = new ModifiedTripBuilder(
      existingTripTimes,
      pattern,
      estimatedVehicleJourney,
      serviceDate,
      transitModel.getTimeZone(),
      entityResolver
    )
      .build();
    if (updateResult.isFailure()) {
      return updateResult.toFailureResult();
    }

    if (!updateResult.successValue().stopPattern().equals(pattern.getStopPattern())) {
      // Replace scheduled trip pattern, if pattern has changed
      markScheduledTripAsDeleted(trip, serviceDate);
    }

    // Also check whether trip id has been used for previously ADDED/MODIFIED trip message and
    // remove the previously created trip
    removePreviousRealtimeUpdate(trip, serviceDate);

    return updateResult;
  }

  /**
   * Add a (new) trip to the transitModel and the buffer
   */
  private Result<UpdateSuccess, UpdateError> addTripToGraphAndBuffer(TripUpdate tripUpdate) {
    Trip trip = tripUpdate.tripTimes().getTrip();
    LocalDate serviceDate = tripUpdate.serviceDate();

    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      tripUpdate.stopPattern(),
      trip,
      serviceDate
    );

    // Add new trip times to the buffer and return success
    var result = buffer.update(pattern, tripUpdate.tripTimes(), serviceDate);

    LOG.debug("Applied real-time data for trip {} on {}", trip, serviceDate);

    return result;
  }

  /**
   * Mark the scheduled trip in the buffer as deleted, given trip on service date
   *
   * @param serviceDate service date
   * @return true if scheduled trip was marked as deleted
   */
  private boolean markScheduledTripAsDeleted(Trip trip, final LocalDate serviceDate) {
    boolean success = false;

    final TripPattern pattern = transitService.getPatternForTrip(trip);

    if (pattern != null) {
      // Mark scheduled trip times for this trip in this pattern as deleted
      final Timetable timetable = pattern.getScheduledTimetable();
      final TripTimes tripTimes = timetable.getTripTimes(trip);

      if (tripTimes == null) {
        LOG.warn("Could not mark scheduled trip as deleted {}", trip.getId());
      } else {
        final RealTimeTripTimes newTripTimes = tripTimes.copyScheduledTimes();
        newTripTimes.deleteTrip();
        buffer.update(pattern, newTripTimes, serviceDate);
        success = true;
      }
    }

    return success;
  }

  /**
   * Removes previous trip-update from buffer if there is an update with given trip on service date
   *
   * @param serviceDate service date
   * @return true if a previously added trip was removed
   */
  private boolean removePreviousRealtimeUpdate(final Trip trip, final LocalDate serviceDate) {
    boolean success = false;

    final TripPattern pattern = buffer.getRealtimeAddedTripPattern(trip.getId(), serviceDate);
    if (pattern != null) {
      // Remove the previous real-time-added TripPattern from buffer.
      // Only one version of the real-time-update should exist
      buffer.removeLastAddedTripPattern(trip.getId(), serviceDate);
      buffer.removeRealtimeUpdatedTripTimes(pattern, trip.getId(), serviceDate);
      success = true;
    }

    return success;
  }

  private boolean purgeExpiredData() {
    final LocalDate today = LocalDate.now(transitModel.getTimeZone());
    final LocalDate previously = today.minusDays(2); // Just to be safe...

    if (lastPurgeDate != null && lastPurgeDate.compareTo(previously) > 0) {
      return false;
    }

    LOG.debug("purging expired real-time data");

    lastPurgeDate = previously;

    return buffer.purgeExpiredData(previously);
  }
}
