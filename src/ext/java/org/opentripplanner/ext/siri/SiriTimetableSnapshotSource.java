package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.siri.TimetableHelper.createUpdatedTripTimes;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_TRIP_ID;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.UNKNOWN;
import static org.opentripplanner.model.UpdateSuccess.WarningType.NOT_MONITORED;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.model.UpdateSuccess;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.EstimatedVersionFrameStructure;
import uk.org.siri.siri20.VehicleJourneyRef;

/**
 * This class should be used to create snapshots of lookup tables of realtime data. This is
 * necessary to provide planning threads a consistent constant view of a graph with realtime data at
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
   * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime
   * messages.
   */
  private final SiriTripPatternCache tripPatternCache;
  private final ZoneId timeZone;

  private final TransitService transitService;
  private final TransitLayerUpdater transitLayerUpdater;

  /**
   * If a timetable snapshot is requested less than this number of milliseconds after the previous
   * snapshot, just return the same one. Throttles the potentially resource-consuming task of
   * duplicating a TripPattern -> Timetable map and indexing the new Timetables.
   */
  private final int maxSnapshotFrequency;

  /**
   * The last committed snapshot that was handed off to a routing thread. This snapshot may be given
   * to more than one routing thread if the maximum snapshot frequency is exceeded.
   */
  private volatile TimetableSnapshot snapshot = null;

  /** Should expired realtime data be purged from the graph. */
  private final boolean purgeExpiredData;

  protected LocalDate lastPurgeDate = null;
  protected long lastSnapshotTime = -1;

  public SiriTimetableSnapshotSource(
    TimetableSnapshotSourceParameters parameters,
    TransitModel transitModel
  ) {
    this.timeZone = transitModel.getTimeZone();
    this.transitService = new DefaultTransitService(transitModel);
    this.transitLayerUpdater = transitModel.getTransitLayerUpdater();
    this.maxSnapshotFrequency = parameters.maxSnapshotFrequencyMs();
    this.purgeExpiredData = parameters.purgeExpiredData();
    this.tripPatternCache =
      new SiriTripPatternCache(tripPatternIdGenerator, transitService::getPatternForTrip);

    transitModel.initTimetableSnapshotProvider(this);

    // Force commit so that snapshot initializes
    getTimetableSnapshot(true);
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
        snapshotToReturn = getTimetableSnapshot(false);
      } finally {
        bufferLock.unlock();
      }
    } else {
      // No lock could be obtained because there is either a snapshot commit busy or updates
      // are applied at this moment, just return the current snapshot
      snapshotToReturn = snapshot;
    }

    return snapshotToReturn;
  }

  /**
   * Method to apply a trip update list to the most recent version of the timetable snapshot.
   *
   * @param transitModel transitModel to update (needed for adding/changing stop patterns)
   * @param fullDataset  true iff the list with updates represent all updates that are active right
   *                     now, i.e. all previous updates should be disregarded
   * @param updates      SIRI VehicleMonitoringDeliveries that should be applied atomically
   */
  public UpdateResult applyEstimatedTimetable(
    TransitModel transitModel,
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

      for (EstimatedTimetableDeliveryStructure etDelivery : updates) {
        var res = apply(etDelivery, transitModel, fuzzyTripMatcher, entityResolver);
        results.addAll(res);
      }

      LOG.debug("message contains {} trip updates", updates.size());
      LOG.debug("end of update message");

      // Make a snapshot after each message in anticipation of incoming requests
      // Purge data if necessary (and force new snapshot if anything was purged)
      // Make sure that the public (locking) getTimetableSnapshot function is not called.
      if (purgeExpiredData) {
        final boolean modified = purgeExpiredData();
        getTimetableSnapshot(modified);
      } else {
        getTimetableSnapshot(false);
      }
    } finally {
      // Always release lock
      bufferLock.unlock();
    }
    return UpdateResult.ofResults(results);
  }

  private List<Result<UpdateSuccess, UpdateError>> apply(
    EstimatedTimetableDeliveryStructure etDelivery,
    TransitModel transitModel,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver
  ) {
    List<Result<UpdateSuccess, UpdateError>> results = new ArrayList<>();
    List<EstimatedVersionFrameStructure> estimatedJourneyVersions = etDelivery.getEstimatedJourneyVersionFrames();
    if (estimatedJourneyVersions != null) {
      //Handle deliveries
      for (EstimatedVersionFrameStructure estimatedJourneyVersion : estimatedJourneyVersions) {
        List<EstimatedVehicleJourney> journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
        LOG.debug("Handling {} EstimatedVehicleJourneys.", journeys.size());
        for (EstimatedVehicleJourney journey : journeys) {
          if (shouldAddNewTrip(journey, entityResolver)) {
            try {
              results.add(handleAddedTrip(transitModel, journey, entityResolver));
            } catch (Throwable t) {
              // Since this is work in progress - catch everything to continue processing updates
              LOG.warn("Adding ExtraJourney {} failed.", DebugString.of(journey), t);
              results.add(Result.failure(UpdateError.noTripId(UNKNOWN)));
            }
          } else {
            // Updated trip
            results.add(
              handleModifiedTrip(
                fuzzyTripMatcher,
                entityResolver,
                journey,
                transitModel::getStopLocationById,
                transitModel.getDeduplicator()
              )
            );
          }
        }
      }
    }
    return results;
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

  private TimetableSnapshot getTimetableSnapshot(final boolean force) {
    final long now = System.currentTimeMillis();
    if (force || now - lastSnapshotTime > maxSnapshotFrequency) {
      if (force || buffer.isDirty()) {
        LOG.debug("Committing {}", buffer);
        snapshot = buffer.commit(transitLayerUpdater, force);
      } else {
        LOG.debug("Buffer was unchanged, keeping old snapshot.");
      }
      lastSnapshotTime = System.currentTimeMillis();
    } else {
      LOG.debug("Snapshot frequency exceeded. Reusing snapshot {}", snapshot);
    }
    return snapshot;
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

  private Result<UpdateSuccess, UpdateError> handleAddedTrip(
    TransitModel transitModel,
    EstimatedVehicleJourney estimatedVehicleJourney,
    EntityResolver entityResolver
  ) {
    var result = new AddedTripHelper(
      estimatedVehicleJourney,
      transitModel,
      entityResolver,
      tripPatternIdGenerator::generateUniqueTripPatternId
    )
      .build();

    if (result.isFailure()) {
      return result.toFailureResult();
    }

    /* commit */
    return addTripToGraphAndBuffer(
      result.successValue().trip(),
      result.successValue().stopPattern(),
      result.successValue().tripTimes(),
      result.successValue().serviceDate(),
      estimatedVehicleJourney,
      entityResolver
    );
  }

  private Result<UpdateSuccess, UpdateError> handleModifiedTrip(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney,
    Function<FeedScopedId, StopLocation> getStopLocationById,
    Deduplicator deduplicator
  ) {
    //Check if EstimatedVehicleJourney is reported as NOT monitored
    if (estimatedVehicleJourney.isMonitored() != null && !estimatedVehicleJourney.isMonitored()) {
      //Ignore the notMonitored-flag if the journey is NOT monitored because it has been cancelled
      if (
        estimatedVehicleJourney.isCancellation() != null &&
        !estimatedVehicleJourney.isCancellation()
      ) {
        return Result.success(UpdateSuccess.ofWarnings(NOT_MONITORED));
      }
    }

    LocalDate serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);
    Trip trip = entityResolver.resolveTrip(estimatedVehicleJourney);

    if (serviceDate == null) {
      var tripId = trip != null ? trip.getId() : null;
      return UpdateError.result(tripId, NO_START_DATE);
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
      return UpdateError.result(null, NO_TRIP_ID);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    var updateResult = createUpdatedTripTimes(
      currentTimetable,
      estimatedVehicleJourney,
      trip.getId(),
      getStopLocationById,
      serviceDate,
      timeZone,
      deduplicator
    );
    if (updateResult.isFailure()) {
      LOG.info("Failed to update TripTimes for trip found by exact match {}", trip.getId());
      return updateResult.toFailureResult();
    }

    var tripTimes = updateResult.successValue().times();
    var stopPattern = updateResult.successValue().pattern();

    if (!stopPattern.equals(pattern.getStopPattern())) {
      // Replace scheduled trip pattern, if pattern has changed
      markScheduledTripAsDeleted(trip, serviceDate);
    }

    // Also check whether trip id has been used for previously ADDED/MODIFIED trip message and
    // remove the previously created trip
    removePreviousRealtimeUpdate(trip, serviceDate);

    // Add new trip
    var result = addTripToGraphAndBuffer(
      trip,
      stopPattern,
      tripTimes,
      serviceDate,
      estimatedVehicleJourney,
      entityResolver
    );

    LOG.debug("Applied realtime data for trip {}", trip.getId().getId());

    return result;
  }

  /**
   * Add a (new) trip to the transitModel and the buffer
   */
  private Result<UpdateSuccess, UpdateError> addTripToGraphAndBuffer(
    final Trip trip,
    StopPattern stopPattern,
    TripTimes updatedTripTimes,
    final LocalDate serviceDate,
    EstimatedVehicleJourney estimatedVehicleJourney,
    EntityResolver entityResolver
  ) {
    // Preconditions
    Objects.requireNonNull(updatedTripTimes);

    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      stopPattern,
      trip,
      serviceDate
    );

    // Add new trip times to the buffer and return success
    var result = buffer.update(pattern, updatedTripTimes, serviceDate);

    // Add TripOnServiceDate to buffer if a dated service journey id is supplied in the SIRI message
    addTripOnServiceDateToBuffer(trip, serviceDate, estimatedVehicleJourney, entityResolver);

    return result;
  }

  private void addTripOnServiceDateToBuffer(
    Trip trip,
    LocalDate serviceDate,
    EstimatedVehicleJourney estimatedVehicleJourney,
    EntityResolver entityResolver
  ) {
    var datedServiceJourneyId = entityResolver.resolveDatedServiceJourneyId(
      estimatedVehicleJourney
    );

    if (datedServiceJourneyId == null) {
      if (estimatedVehicleJourney.getFramedVehicleJourneyRef() != null) {
        var tripOnDate = entityResolver.resolveTripOnServiceDate(
          estimatedVehicleJourney.getFramedVehicleJourneyRef()
        );
        if (tripOnDate == null) {
          return;
        }
        datedServiceJourneyId = tripOnDate.getId();
      }
    }

    if (datedServiceJourneyId == null) {
      return;
    }

    List<TripOnServiceDate> listOfReplacedVehicleJourneys = new ArrayList<>();

    // VehicleJourneyRef is the reference to the serviceJourney being replaced.
    VehicleJourneyRef vehicleJourneyRef = estimatedVehicleJourney.getVehicleJourneyRef();
    if (vehicleJourneyRef != null) {
      var replacedDatedServiceJourney = entityResolver.resolveTripOnServiceDate(
        vehicleJourneyRef.getValue()
      );
      if (replacedDatedServiceJourney != null) {
        listOfReplacedVehicleJourneys.add(replacedDatedServiceJourney);
      }
    }

    // Add additional replaced service journeys if present.
    estimatedVehicleJourney
      .getAdditionalVehicleJourneyReves()
      .stream()
      .map(entityResolver::resolveTripOnServiceDate)
      .filter(Objects::nonNull)
      .forEach(listOfReplacedVehicleJourneys::add);

    buffer.addLastAddedTripOnServiceDate(
      TripOnServiceDate
        .of(datedServiceJourneyId)
        .withTrip(trip)
        .withReplacementFor(listOfReplacedVehicleJourneys)
        .withServiceDate(serviceDate)
        .build()
    );
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
        final TripTimes newTripTimes = new TripTimes(tripTimes);
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
      /*
              Remove the previous realtime-added TripPattern from buffer.
              Only one version of the realtime-update should exist
             */
      buffer.removeLastAddedTripPattern(trip.getId(), serviceDate);
      buffer.removeRealtimeUpdatedTripTimes(pattern, trip.getId(), serviceDate);
      success = true;
    }

    return success;
  }

  private boolean purgeExpiredData() {
    final LocalDate today = LocalDate.now(timeZone);
    final LocalDate previously = today.minusDays(2); // Just to be safe...

    if (lastPurgeDate != null && lastPurgeDate.compareTo(previously) > 0) {
      return false;
    }

    LOG.debug("purging expired realtime data");

    lastPurgeDate = previously;

    return buffer.purgeExpiredData(previously);
  }
}
