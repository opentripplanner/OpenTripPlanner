package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.EMPTY_STOP_POINT_REF;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_MONITORED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.TripPatternCache;
import org.opentripplanner.updater.trip.TripPatternIdGenerator;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;

/**
 * Adapts from SIRI-ET EstimatedTimetables to OTP's internal real-time data model.
 */
public class SiriRealTimeTripUpdateAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(SiriRealTimeTripUpdateAdapter.class);

  /**
   * Use an id generator to generate TripPattern ids for new TripPatterns created by RealTime
   * updates.
   */
  private final TripPatternIdGenerator tripPatternIdGenerator = new TripPatternIdGenerator();
  /**
   * A synchronized cache of trip patterns that are added to the graph due to GTFS-real-time
   * messages.
   */
  private final TripPatternCache tripPatternCache;

  /**
   * Long-lived transit editor service that has access to the timetable snapshot buffer.
   * This differs from the usual use case where the transit service refers to the latest published
   * timetable snapshot.
   */
  private final TransitEditorService transitEditorService;

  private final TimetableSnapshotManager snapshotManager;

  public SiriRealTimeTripUpdateAdapter(
    TimetableRepository timetableRepository,
    TimetableSnapshotManager snapshotManager,
    TripPatternCache tripPatternCache
  ) {
    this.snapshotManager = snapshotManager;
    this.transitEditorService = new DefaultTransitService(
      timetableRepository,
      snapshotManager.getTimetableSnapshotBuffer()
    );
    this.tripPatternCache = tripPatternCache;
  }

  /**
   * Method to apply estimated timetables to the most recent version of the timetable snapshot.
   *
   * @param incrementality  the incrementality of the update, for example if updates represent all
   *                        updates that are active right now, i.e. all previous updates should be
   *                        disregarded
   * @param updates    SIRI EstimatedTimetable deliveries that should be applied atomically.
   */
  public UpdateResult applyEstimatedTimetable(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    String feedId,
    UpdateIncrementality incrementality,
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    if (updates == null) {
      LOG.warn("updates is null");
      return UpdateResult.empty();
    }

    List<Result<UpdateSuccess, UpdateError>> results = new ArrayList<>();

    if (incrementality == FULL_DATASET) {
      // Remove all updates from the buffer
      snapshotManager.clearBuffer(feedId);
    }

    for (var etDelivery : updates) {
      for (var estimatedJourneyVersion : etDelivery.getEstimatedJourneyVersionFrames()) {
        var journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
        LOG.debug("Handling {} EstimatedVehicleJourneys.", journeys.size());
        for (EstimatedVehicleJourney journey : journeys) {
          results.add(apply(journey, transitEditorService, fuzzyTripMatcher, entityResolver));
        }
      }
    }

    LOG.debug("message contains {} trip updates", updates.size());

    return UpdateResult.ofResults(results);
  }

  private Result<UpdateSuccess, UpdateError> apply(
    EstimatedVehicleJourney journey,
    TransitEditorService transitService,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver
  ) {
    List<CallWrapper> callWrappers = CallWrapper.of(journey);
    for (var call : callWrappers) {
      if (StringUtils.hasNoValueOrNullAsString(call.getStopPointRef())) {
        return UpdateError.result(null, EMPTY_STOP_POINT_REF, journey.getDataSource());
      }
    }
    SiriUpdateType siriUpdateType = null;
    try {
      siriUpdateType = updateType(journey, callWrappers, entityResolver);
      Result<TripUpdate, UpdateError> result =
        switch (siriUpdateType) {
          case REPLACEMENT_DEPARTURE -> new AddedTripBuilder(
            journey,
            transitService,
            entityResolver,
            tripPatternIdGenerator::generateUniqueTripPatternId
          ).build();
          case EXTRA_CALL -> handleExtraCall(fuzzyTripMatcher, entityResolver, journey);
          case TRIP_UPDATE -> handleModifiedTrip(fuzzyTripMatcher, entityResolver, journey);
        };

      if (result.isFailure()) {
        return result.toFailureResult();
      }

      /* commit */
      return addTripToGraphAndBuffer(result.successValue());
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e, journey.getDataSource());
    } catch (Exception e) {
      LOG.warn("{} EstimatedJourney {} failed.", siriUpdateType, DebugString.of(journey), e);
      return Result.failure(UpdateError.noTripId(UNKNOWN));
    }
  }

  private SiriUpdateType updateType(
    EstimatedVehicleJourney vehicleJourney,
    List<CallWrapper> callWrappers,
    EntityResolver entityResolver
  ) {
    // Extra call if at least one of the call is an extra call
    if (callWrappers.stream().anyMatch(CallWrapper::isExtraCall)) {
      return SiriUpdateType.EXTRA_CALL;
    }

    // Replacement departure if the trip is marked as extra journey, and it has not been added before
    if (
      TRUE.equals(vehicleJourney.isExtraJourney()) &&
      entityResolver.resolveTrip(vehicleJourney) == null
    ) {
      return SiriUpdateType.REPLACEMENT_DEPARTURE;
    }

    // otherwise this is a trip update
    return SiriUpdateType.TRIP_UPDATE;
  }

  /**
   * Get the latest timetable for TripPattern for a given service date.
   * <p>
   * Snapshot timetable is used as source if initialised, trip patterns scheduled timetable if not.
   */
  private Timetable getCurrentTimetable(TripPattern tripPattern, LocalDate serviceDate) {
    return snapshotManager.getTimetableSnapshotBuffer().resolve(tripPattern, serviceDate);
  }

  private Result<TripUpdate, UpdateError> handleModifiedTrip(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    Trip trip = entityResolver.resolveTrip(estimatedVehicleJourney);
    String dataSource = estimatedVehicleJourney.getDataSource();

    // Check if EstimatedVehicleJourney is reported as NOT monitored, ignore the notMonitored-flag
    // if the journey is NOT monitored because it has been cancelled
    if (
      !TRUE.equals(estimatedVehicleJourney.isMonitored()) &&
      !TRUE.equals(estimatedVehicleJourney.isCancellation())
    ) {
      return UpdateError.result(trip != null ? trip.getId() : null, NOT_MONITORED, dataSource);
    }

    LocalDate serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);

    if (serviceDate == null) {
      return UpdateError.result(trip != null ? trip.getId() : null, NO_START_DATE, dataSource);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitEditorService.findPattern(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      var result = fuzzyTripMatcher.match(
        estimatedVehicleJourney,
        entityResolver,
        this::getCurrentTimetable,
        snapshotManager::getNewTripPatternForModifiedTrip
      );

      if (result.isFailure()) {
        LOG.debug(
          "No trips found for EstimatedVehicleJourney. {}",
          DebugString.of(estimatedVehicleJourney)
        );
        return UpdateError.result(null, result.failureValue(), dataSource);
      }

      var tripAndPattern = result.successValue();
      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      return UpdateError.result(null, TRIP_NOT_FOUND, dataSource);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      return UpdateError.result(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN, dataSource);
    }
    var updateResult = new ModifiedTripBuilder(
      existingTripTimes,
      pattern,
      estimatedVehicleJourney,
      serviceDate,
      transitEditorService.getTimeZone(),
      entityResolver
    ).build();
    if (updateResult.isFailure()) {
      return updateResult.toFailureResult();
    }

    if (!updateResult.successValue().stopPattern().equals(pattern.getStopPattern())) {
      // Replace scheduled trip pattern, if pattern has changed
      markScheduledTripAsDeleted(trip, serviceDate);
    }

    // Also check whether trip id has been used for previously ADDED/MODIFIED trip message and
    // remove the previously created trip
    this.snapshotManager.revertTripToScheduledTripPattern(trip.getId(), serviceDate);

    return updateResult;
  }

  private Result<TripUpdate, UpdateError> handleExtraCall(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    Trip trip = entityResolver.resolveTrip(estimatedVehicleJourney);
    String dataSource = estimatedVehicleJourney.getDataSource();

    // Check if EstimatedVehicleJourney is reported as NOT monitored, ignore the notMonitored-flag
    // if the journey is NOT monitored because it has been cancelled
    if (
      !TRUE.equals(estimatedVehicleJourney.isMonitored()) &&
      !TRUE.equals(estimatedVehicleJourney.isCancellation())
    ) {
      return UpdateError.result(trip != null ? trip.getId() : null, NOT_MONITORED, dataSource);
    }

    LocalDate serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);

    if (serviceDate == null) {
      return UpdateError.result(trip != null ? trip.getId() : null, NO_START_DATE, dataSource);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitEditorService.findPattern(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      var result = fuzzyTripMatcher.match(
        estimatedVehicleJourney,
        entityResolver,
        this::getCurrentTimetable,
        snapshotManager::getNewTripPatternForModifiedTrip
      );

      if (result.isFailure()) {
        LOG.debug(
          "No trips found for EstimatedVehicleJourney. {}",
          DebugString.of(estimatedVehicleJourney)
        );
        return UpdateError.result(null, result.failureValue(), dataSource);
      }

      var tripAndPattern = result.successValue();
      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      return UpdateError.result(null, TRIP_NOT_FOUND, dataSource);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      return UpdateError.result(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN, dataSource);
    }
    var updateResult = new ExtraCallTripBuilder(
      estimatedVehicleJourney,
      transitEditorService,
      entityResolver,
      tripPatternIdGenerator::generateUniqueTripPatternId,
      trip
    ).build();
    if (updateResult.isFailure()) {
      return updateResult.toFailureResult();
    }

    if (!updateResult.successValue().stopPattern().equals(pattern.getStopPattern())) {
      // Replace scheduled trip pattern, if pattern has changed
      markScheduledTripAsDeleted(trip, serviceDate);
    }

    // Also check whether trip id has been used for previously ADDED/MODIFIED trip message and
    // remove the previously created trip
    this.snapshotManager.revertTripToScheduledTripPattern(trip.getId(), serviceDate);

    return updateResult;
  }

  /**
   * Add a (new) trip to the timetableRepository and the buffer
   */
  private Result<UpdateSuccess, UpdateError> addTripToGraphAndBuffer(TripUpdate tripUpdate) {
    Trip trip = tripUpdate.tripTimes().getTrip();
    LocalDate serviceDate = tripUpdate.serviceDate();

    final TripPattern pattern;
    if (tripUpdate.tripPatternCreation()) {
      pattern = tripUpdate.addedTripPattern();
    } else {
      // Get cached trip pattern or create one if it doesn't exist yet
      pattern = tripPatternCache.getOrCreateTripPattern(tripUpdate.stopPattern(), trip);
    }

    // Add new trip times to buffer, making protective copies as needed. Bubble success/error up.
    RealTimeTripUpdate realTimeTripUpdate = new RealTimeTripUpdate(
      pattern,
      tripUpdate.tripTimes(),
      serviceDate,
      tripUpdate.addedTripOnServiceDate(),
      tripUpdate.tripCreation(),
      tripUpdate.routeCreation(),
      tripUpdate.dataSource()
    );
    var result = snapshotManager.updateBuffer(realTimeTripUpdate);
    LOG.debug("Applied real-time data for trip {} on {}", trip, serviceDate);
    return result;
  }

  /**
   * Mark the scheduled trip in the buffer as deleted, given trip on service date
   *
   * @return true if scheduled trip was marked as deleted
   */
  private boolean markScheduledTripAsDeleted(Trip trip, final LocalDate serviceDate) {
    boolean success = false;

    final TripPattern pattern = transitEditorService.findPattern(trip);

    if (pattern != null) {
      // Mark scheduled trip times for this trip in this pattern as deleted
      final Timetable timetable = pattern.getScheduledTimetable();
      final TripTimes tripTimes = timetable.getTripTimes(trip);

      if (tripTimes == null) {
        LOG.warn("Could not mark scheduled trip as deleted {}", trip.getId());
      } else {
        final RealTimeTripTimes newTripTimes = tripTimes.copyScheduledTimes();
        newTripTimes.deleteTrip();
        snapshotManager.updateBuffer(new RealTimeTripUpdate(pattern, newTripTimes, serviceDate));
        success = true;
      }
    }

    return success;
  }

  /**
   * Types of SIRI update messages.
   */
  private enum SiriUpdateType {
    /**
     * Update of an existing trip.
     * This can be either a trip defined in planned data or a replacement departure
     * that was previously added by a real-time message.
     * The update can consist in updated passing times and/or cancellation of some stops.
     * A stop can be substituted by another if they belong to the same station.
     * The whole trip can also be marked as cancelled.
     */
    TRIP_UPDATE,

    /**
     * Addition of a new trip, not currently present in the system.
     * The new trip has a new unique id.
     * The trip can replace one or more existing trips, another SIRI message should handle the
     * cancellation of the replaced trips.
     */
    REPLACEMENT_DEPARTURE,

    /**
     * Addition of one or more stops in an existing trip.
     */
    EXTRA_CALL,
  }
}
