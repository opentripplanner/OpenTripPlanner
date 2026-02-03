package org.opentripplanner.updater.trip.gtfs;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.DELETED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_DUPLICATED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_UNSCHEDULED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_TRIP_FOR_CANCELLATION_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.OUTSIDE_SERVICE_PERIOD;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.updater.trip.siri.SiriTripPatternCache;
import org.opentripplanner.updater.trip.siri.SiriTripPatternIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts from GTFS-RT TripUpdates to OTP's internal real-time data model.
 */
public class GtfsRealTimeTripUpdateAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsRealTimeTripUpdateAdapter.class);

  /**
   * A synchronized cache of trip patterns added to the timetable repository
   * due to GTFS-realtime messages.
   * <p>
   * This has "Siri" in the name because we are combining the two versions very carefully, step by
   * step. Once this process is complete, we will clean up the name and move it to an appropriate
   * package.
   **/
  private final SiriTripPatternCache tripPatternCache;

  /**
   * Long-lived transit editor service that has access to the timetable snapshot buffer.
   * This differs from the usual use case where the transit service refers to the latest published
   * timetable snapshot.
   */
  private final TransitEditorService transitEditorService;

  private final DeduplicatorService deduplicator;

  private final TimetableSnapshotManager snapshotManager;
  private final Supplier<LocalDate> localDateNow;
  private final TripTimesUpdater tripTimesUpdater;

  /**
   * Constructor to allow tests to provide their own clock, not using system time.
   */
  public GtfsRealTimeTripUpdateAdapter(
    TimetableRepository timetableRepository,
    DeduplicatorService deduplicator,
    TimetableSnapshotManager snapshotManager,
    Supplier<LocalDate> localDateNow
  ) {
    this.snapshotManager = snapshotManager;
    this.localDateNow = localDateNow;
    this.transitEditorService = new DefaultTransitService(
      timetableRepository,
      snapshotManager.getTimetableSnapshotBuffer()
    );
    this.deduplicator = deduplicator;
    this.tripTimesUpdater = new TripTimesUpdater(timetableRepository.getTimeZone(), deduplicator);
    this.tripPatternCache = new SiriTripPatternCache(
      new SiriTripPatternIdGenerator(),
      transitEditorService::findPattern
    );
  }

  /**
   * Method to apply a trip update list to the most recent version of the timetable snapshot. A
   * GTFS-RT feed is always applied against a single static feed (indicated by feedId).
   * <p>
   * However, multi-feed support is not completed and we currently assume there is only one static
   * feed when matching IDs.
   *
   * @param backwardsDelayPropagationType Defines when delays are propagated to previous stops and
   *                                      if these stops are given the NO_DATA flag.
   * @param updateIncrementality          Determines the incrementality of the updates. FULL updates clear the buffer
   *                                      of all previous updates for the given feed id.
   * @param updates                       GTFS-RT TripUpdate's that should be applied atomically
   */
  public UpdateResult applyTripUpdates(
    @Nullable GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    UpdateIncrementality updateIncrementality,
    List<GtfsRealtime.TripUpdate> updates,
    String feedId
  ) {
    List<Result<UpdateSuccess, UpdateError>> results = new ArrayList<>();

    if (updateIncrementality == FULL_DATASET) {
      // Remove all updates from the buffer
      snapshotManager.clearBuffer(feedId);
    }

    for (var rawTripUpdate : updates) {
      Result<UpdateSuccess, UpdateError> result;
      try {
        if (fuzzyTripMatcher != null) {
          var trip = fuzzyTripMatcher.match(feedId, rawTripUpdate.getTrip());
          rawTripUpdate = rawTripUpdate.toBuilder().setTrip(trip).build();
        }

        var tripUpdate = new TripUpdate(feedId, rawTripUpdate, localDateNow);
        var validationResult = tripUpdate.validate();
        if (validationResult.isFailure()) {
          results.add(validationResult.toFailureResult());
          continue;
        }

        // Determine what kind of trip update this is
        if (updateIncrementality == DIFFERENTIAL) {
          purgePatternModifications(tripUpdate);
        }

        result = applyUpdate(
          tripUpdate,
          updateIncrementality,
          backwardsDelayPropagationType,
          forwardsDelayPropagationType
        );
      } catch (DataValidationException e) {
        result = DataValidationExceptionMapper.toResult(e);
      }
      results.add(result);
    }

    var updateResult = UpdateResult.ofResults(results);

    if (updateIncrementality == FULL_DATASET) {
      ResultLogger.logUpdateResult(feedId, "gtfs-rt-trip-updates", updateResult);
    }
    return updateResult;
  }

  private Result<UpdateSuccess, UpdateError> applyUpdate(
    TripUpdate tripUpdate,
    UpdateIncrementality updateIncrementality,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    ForwardsDelayPropagationType forwardsDelayPropagationType
  ) {
    return switch (tripUpdate.scheduleRelationship()) {
      case SCHEDULED -> handleScheduledTrip(
        tripUpdate,
        forwardsDelayPropagationType,
        backwardsDelayPropagationType
      );
      case NEW, ADDED -> validateAndHandleNewTrip(tripUpdate);
      case CANCELED -> handleCanceledTrip(tripUpdate, CancelationType.CANCEL, updateIncrementality);
      case DELETED -> handleCanceledTrip(tripUpdate, CancelationType.DELETE, updateIncrementality);
      case REPLACEMENT -> validateAndHandleReplacementTrip(tripUpdate);
      case UNSCHEDULED -> UpdateError.result(tripUpdate.tripId(), NOT_IMPLEMENTED_UNSCHEDULED);
      case DUPLICATED -> UpdateError.result(tripUpdate.tripId(), NOT_IMPLEMENTED_DUPLICATED);
    };
  }

  /**
   * Remove previous realtime updates for this trip. This is necessary to avoid previous stop
   * pattern modifications from persisting. If a trip was previously added with the
   * ScheduleRelationship NEW and is now cancelled or deleted, we still want to keep the realtime
   * added trip pattern.
   */
  private void purgePatternModifications(TripUpdate tripUpdate) {
    final TripPattern pattern = snapshotManager.getNewTripPatternForModifiedTrip(
      tripUpdate.tripId(),
      tripUpdate.serviceDate()
    );
    if (
      !isPreviouslyAddedTrip(tripUpdate, pattern) ||
      (tripUpdate.scheduleRelationship() != CANCELED &&
        tripUpdate.scheduleRelationship() != DELETED)
    ) {
      // Remove previous realtime updates for this trip. This is necessary to avoid previous
      // stop pattern modifications from persisting. If a trip was previously added with the ScheduleRelationship
      // NEW and is now cancelled or deleted, we still want to keep the realtime added trip pattern.
      this.snapshotManager.revertTripToScheduledTripPattern(
        tripUpdate.tripId(),
        tripUpdate.serviceDate()
      );
    }
  }

  private boolean isPreviouslyAddedTrip(TripUpdate update, TripPattern pattern) {
    if (pattern == null) {
      return false;
    }
    var timetable = snapshotManager.resolve(pattern, update.serviceDate());
    if (timetable == null) {
      return false;
    }
    var tripTimes = timetable.getTripTimes(update.tripId());
    if (tripTimes == null) {
      return false;
    }
    return tripTimes.getRealTimeState() == RealTimeState.ADDED;
  }

  private Result<UpdateSuccess, UpdateError> handleScheduledTrip(
    TripUpdate tripUpdate,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    final TripPattern pattern = getPatternForTripId(tripUpdate.tripId());

    if (pattern == null) {
      return UpdateError.result(tripUpdate.tripId(), TRIP_NOT_FOUND);
    }

    if (tripUpdate.stopTimeUpdates().isEmpty()) {
      return UpdateError.result(tripUpdate.tripId(), NO_UPDATES);
    }

    var serviceId = transitEditorService.getTrip(tripUpdate.tripId()).getServiceId();
    var serviceDates = transitEditorService
      .getCalendarService()
      .getServiceDatesForServiceId(serviceId);
    if (!serviceDates.contains(tripUpdate.serviceDate())) {
      return UpdateError.result(tripUpdate.tripId(), NO_SERVICE_ON_DATE);
    }

    // Get new TripTimes based on scheduled timetable
    var result = tripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      pattern.getScheduledTimetable(),
      tripUpdate,
      forwardsDelayPropagationType,
      backwardsDelayPropagationType
    );

    if (result.isFailure()) {
      // necessary so the success type is correct
      return result.toFailureResult();
    }

    var tripTimesPatch = result.successValue();

    var updatedPickup = tripTimesPatch.updatedPickup();
    var updatedDropoff = tripTimesPatch.updatedDropoff();
    var replacedStopIndices = tripTimesPatch.replacedStopIndices();

    var updatedTripTimes = tripTimesPatch.tripTimes();

    Map<Integer, StopLocation> newStops = new HashMap<>();
    for (var entry : replacedStopIndices.entrySet()) {
      var stop = transitEditorService.getRegularStop(
        new FeedScopedId(tripUpdate.tripId().getFeedId(), entry.getValue())
      );
      if (stop != null) {
        newStops.put(entry.getKey(), stop);
      }
    }

    // If there are stops with different pickup / drop off, or replaced stops, we need to change the pattern from the scheduled one
    if (!updatedPickup.isEmpty() || !updatedDropoff.isEmpty() || !newStops.isEmpty()) {
      StopPattern newStopPattern = pattern
        .copyPlannedStopPattern()
        .updatePickups(updatedPickup)
        .updateDropoffs(updatedDropoff)
        .replaceStops(newStops)
        .build();

      final Trip trip = transitEditorService.getTrip(tripUpdate.tripId());
      // Get cached trip pattern or create one if it doesn't exist yet
      final TripPattern newPattern = tripPatternCache.getOrCreateTripPattern(newStopPattern, trip);

      cancelScheduledTrip(tripUpdate.tripId(), tripUpdate.serviceDate(), CancelationType.DELETE);
      return snapshotManager.updateBuffer(
        new RealTimeTripUpdate(newPattern, updatedTripTimes, tripUpdate.serviceDate())
      );
    } else {
      // Set the updated trip times in the buffer
      return snapshotManager.updateBuffer(
        new RealTimeTripUpdate(pattern, updatedTripTimes, tripUpdate.serviceDate())
      );
    }
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing an NEW trip.
   *
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> validateAndHandleNewTrip(final TripUpdate tripUpdate) {
    // Check whether trip id already exists in graph
    if (transitEditorService.getScheduledTrip(tripUpdate.tripId()) != null) {
      return UpdateError.result(tripUpdate.tripId(), TRIP_ALREADY_EXISTS);
    }
    // get service ID running only on this service date
    var serviceId = transitEditorService.getOrCreateServiceIdForDate(tripUpdate.serviceDate());
    if (serviceId == null) {
      return UpdateError.result(tripUpdate.tripId(), OUTSIDE_SERVICE_PERIOD);
    }

    var result = new RouteFactory(transitEditorService).getOrCreate(tripUpdate);

    // TODO: which Agency ID to use? Currently use feed id.
    var tripBuilder = Trip.of(tripUpdate.tripId())
      .withRoute(result.route())
      .withServiceId(serviceId);

    tripUpdate.tripHeadsign().ifPresent(tripBuilder::withHeadsign);
    tripUpdate.tripShortName().ifPresent(tripBuilder::withShortName);

    Trip trip = tripBuilder.build();

    return handleNewOrReplacementTrip(
      trip,
      tripUpdate,
      RealTimeState.ADDED,
      result.newRouteCreated()
    );
  }

  /**
   * Remove any stop that is not know in the static transit data.
   */
  private List<StopAndStopTimeUpdate> matchStopsToStopTimeUpdates(TripUpdate tripUpdate) {
    return tripUpdate
      .stopTimeUpdates()
      .stream()
      .flatMap(st ->
        st
          .stopId()
          .flatMap(id -> {
            var stopId = new FeedScopedId(tripUpdate.tripId().getFeedId(), id);
            var stop = transitEditorService.getRegularStop(stopId);
            return Optional.ofNullable(stop).map(s -> new StopAndStopTimeUpdate(s, st));
          })
          .stream()
      )
      .toList();
  }

  /**
   * Handle GTFS-RT TripUpdate message containing an NEW or REPLACEMENT trip.
   *
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> handleNewOrReplacementTrip(
    Trip trip,
    TripUpdate tripUpdate,
    RealTimeState realTimeState,
    boolean hasANewRouteBeenCreated
  ) {
    FeedScopedId tripId = trip.getId();
    var stopAndStopTimeUpdates = matchStopsToStopTimeUpdates(tripUpdate);

    var warnings = new ArrayList<UpdateSuccess.WarningType>(0);

    if (stopAndStopTimeUpdates.size() < tripUpdate.stopTimeUpdates().size()) {
      warnings.add(UpdateSuccess.WarningType.UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP);
    }

    // check if after filtering the stops we still have at least 2
    if (stopAndStopTimeUpdates.size() < 2) {
      return UpdateError.result(tripId, TOO_FEW_STOPS);
    }

    var result = tripTimesUpdater.createNewTripTimesFromGtfsRt(
      trip,
      tripUpdate,
      stopAndStopTimeUpdates,
      realTimeState,
      transitEditorService.getServiceCode(trip.getServiceId())
    );

    return result
      .flatMap(value ->
        addNewOrReplacementTripToSnapshot(
          value,
          tripUpdate.serviceDate(),
          realTimeState,
          hasANewRouteBeenCreated
        )
      )
      .mapSuccess(s -> s.addWarnings(warnings));
  }

  /**
   * Add a new or replacement trip to the snapshot
   *
   * @param serviceDate       service date of trip
   * @param realTimeState     real-time state of new trip
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> addNewOrReplacementTripToSnapshot(
    final TripTimesWithStopPattern tripTimesWithStopPattern,
    final LocalDate serviceDate,
    final RealTimeState realTimeState,
    final boolean hasANewRouteBeenCreated
  ) {
    RealTimeTripTimes tripTimes = tripTimesWithStopPattern.tripTimes();
    Trip trip = tripTimes.getTrip();

    if (realTimeState == RealTimeState.MODIFIED) {
      // Mark scheduled trip as DELETED
      cancelScheduledTrip(trip.getId(), serviceDate, CancelationType.DELETE);
    }

    // Create StopPattern
    final StopPattern stopPattern = tripTimesWithStopPattern.stopPattern();

    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(stopPattern, trip);

    // Add new trip times to the buffer
    return snapshotManager.updateBuffer(
      new RealTimeTripUpdate(
        pattern,
        tripTimes,
        serviceDate,
        realTimeState == RealTimeState.ADDED
          ? TripOnServiceDate.of(trip.getId()).withTrip(trip).withServiceDate(serviceDate).build()
          : null,
        realTimeState == RealTimeState.ADDED,
        hasANewRouteBeenCreated
      )
    );
  }

  /**
   * Cancel scheduled trip in buffer given trip id  on service date
   *
   * @return true if scheduled trip was cancelled
   */
  private boolean cancelScheduledTrip(
    final FeedScopedId tripId,
    final LocalDate serviceDate,
    CancelationType cancelationType
  ) {
    boolean success = false;

    final TripPattern pattern = getPatternForTripId(tripId);

    if (pattern != null) {
      // Cancel scheduled trip times for this trip in this pattern
      final Timetable timetable = pattern.getScheduledTimetable();
      var tripTimes = timetable.getTripTimes(tripId);
      if (tripTimes != null) {
        cancelTrip(serviceDate, cancelationType, pattern, tripTimes);
        success = true;
      }
    }

    return success;
  }

  /**
   * Cancel previously added trip from buffer if there is a previously added trip with given trip id
   * on service date. This does not remove the added trip from the buffer, it just marks it as
   * canceled or deleted. Any TripPattern that was created for the added trip continues to exist,
   * and will be reused if a similar added trip message is received with the same route and stop
   * sequence.
   *
   * @return true if a previously added trip was cancelled
   */
  private boolean cancelPreviouslyAddedTrip(TripUpdate update, CancelationType cancelationType) {
    boolean cancelledAddedTrip = false;

    final TripPattern pattern = snapshotManager.getNewTripPatternForModifiedTrip(
      update.tripId(),
      update.serviceDate()
    );
    if (isPreviouslyAddedTrip(update, pattern)) {
      // Cancel trip times for this trip in this pattern
      final Timetable timetable = snapshotManager.resolve(pattern, update.serviceDate());
      var tripTimes = timetable.getTripTimes(update.tripId());
      if (tripTimes != null) {
        cancelTrip(update.serviceDate(), cancelationType, pattern, tripTimes);
        cancelledAddedTrip = true;
      }
    }
    return cancelledAddedTrip;
  }

  private void cancelTrip(
    LocalDate serviceDate,
    CancelationType cancelationType,
    TripPattern pattern,
    TripTimes tripTimes
  ) {
    final RealTimeTripTimesBuilder builder = tripTimes.createRealTimeFromScheduledTimes();
    switch (cancelationType) {
      case CANCEL -> builder.cancelTrip();
      case DELETE -> builder.deleteTrip();
    }
    snapshotManager.updateBuffer(new RealTimeTripUpdate(pattern, builder.build(), serviceDate));
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing a REPLACEMENT trip.
   *
   * @param tripUpdate     GTFS-RT TripUpdate message
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> validateAndHandleReplacementTrip(
    TripUpdate tripUpdate
  ) {
    // Check whether trip id already exists in graph
    Trip trip = transitEditorService.getTrip(tripUpdate.tripId());

    if (trip == null) {
      return UpdateError.result(tripUpdate.tripId(), TRIP_NOT_FOUND);
    }

    // Check whether service date is served by trip
    final Set<FeedScopedId> serviceIds = transitEditorService
      .getCalendarService()
      .getServiceIdsOnDate(tripUpdate.serviceDate());
    if (!serviceIds.contains(trip.getServiceId())) {
      // TODO: should we support this and change service id of trip?
      return UpdateError.result(tripUpdate.tripId(), NO_SERVICE_ON_DATE);
    }

    return handleNewOrReplacementTrip(trip, tripUpdate, RealTimeState.MODIFIED, false);
  }

  private Result<UpdateSuccess, UpdateError> handleCanceledTrip(
    TripUpdate tripUpdate,
    CancelationType cancelationType,
    UpdateIncrementality incrementality
  ) {
    var canceledPreviouslyAddedTrip =
      incrementality != FULL_DATASET && cancelPreviouslyAddedTrip(tripUpdate, cancelationType);

    // if previously an added trip was removed, there can't be a scheduled trip to remove
    if (canceledPreviouslyAddedTrip) {
      return Result.success(UpdateSuccess.noWarnings());
    }
    // Try to cancel scheduled trip
    final boolean cancelScheduledSuccess = cancelScheduledTrip(
      tripUpdate.tripId(),
      tripUpdate.serviceDate(),
      cancelationType
    );

    if (!cancelScheduledSuccess) {
      return UpdateError.result(tripUpdate.tripId(), NO_TRIP_FOR_CANCELLATION_FOUND);
    }

    return Result.success(UpdateSuccess.noWarnings());
  }

  /**
   * Retrieve a trip pattern given a trip id.
   *
   * @param tripId trip id
   * @return trip pattern or null if no trip pattern was found
   */
  private TripPattern getPatternForTripId(FeedScopedId tripId) {
    Trip trip = transitEditorService.getTrip(tripId);
    return transitEditorService.findPattern(trip);
  }

  private enum CancelationType {
    CANCEL,
    DELETE,
  }
}
