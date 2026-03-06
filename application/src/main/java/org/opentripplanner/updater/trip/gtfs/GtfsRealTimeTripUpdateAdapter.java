package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.NOT_IMPLEMENTED_DUPLICATED;
import static org.opentripplanner.updater.spi.UpdateErrorType.NOT_IMPLEMENTED_UNSCHEDULED;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_TRIP_FOR_CANCELLATION_FOUND;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.updater.spi.UpdateErrorType.OUTSIDE_SERVICE_PERIOD;
import static org.opentripplanner.updater.spi.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;
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
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
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
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;
import org.opentripplanner.updater.trip.patterncache.TripPatternIdGenerator;

/**
 * Adapts from GTFS-RT TripUpdates to OTP's internal real-time data model.
 */
public class GtfsRealTimeTripUpdateAdapter {

  /**
   * A synchronized cache of trip patterns added to the timetable repository
   * due to GTFS-realtime messages.
   **/
  private final TripPatternCache tripPatternCache;

  /**
   * Long-lived transit editor service that has access to the timetable snapshot buffer.
   * This differs from the usual use case where the transit service refers to the latest published
   * timetable snapshot.
   */
  private final TransitEditorService transitEditorService;

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
    this.tripTimesUpdater = new TripTimesUpdater(timetableRepository.getTimeZone(), deduplicator);
    this.tripPatternCache = new TripPatternCache(
      new TripPatternIdGenerator(),
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

        result = applyUpdate(
          tripUpdate,
          updateIncrementality,
          backwardsDelayPropagationType,
          forwardsDelayPropagationType
        );
      } catch (DataValidationException e) {
        // TODO: switch to exception
        result = DataValidationExceptionMapper.map(e).toResult();
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

      return snapshotManager.updateBuffer(
        RealTimeTripUpdate.of(newPattern, updatedTripTimes, tripUpdate.serviceDate())
          .withRevertPreviousRealTimeUpdates(true)
          .withHideTripInScheduledPattern(pattern)
          .build()
      );
    } else {
      // Set the updated trip times in the buffer
      return snapshotManager.updateBuffer(
        RealTimeTripUpdate.of(pattern, updatedTripTimes, tripUpdate.serviceDate())
          .withRevertPreviousRealTimeUpdates(true)
          .build()
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

    // Create StopPattern
    final StopPattern stopPattern = tripTimesWithStopPattern.stopPattern();

    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(stopPattern, trip);

    // Look up the scheduled pattern for MODIFIED trips so the manager can mark it as deleted
    TripPattern hideTripInScheduledPattern = null;
    if (realTimeState == RealTimeState.MODIFIED) {
      hideTripInScheduledPattern = getPatternForTripId(trip.getId());
    }

    // Add new trip times to the buffer
    var builder = RealTimeTripUpdate.of(pattern, tripTimes, serviceDate)
      .withRouteCreation(hasANewRouteBeenCreated)
      .withRevertPreviousRealTimeUpdates(true)
      .withHideTripInScheduledPattern(hideTripInScheduledPattern);
    if (realTimeState == RealTimeState.ADDED) {
      builder
        .withAddedTripOnServiceDate(
          TripOnServiceDate.of(trip.getId()).withTrip(trip).withServiceDate(serviceDate).build()
        )
        .withTripCreation(true);
    }
    return snapshotManager.updateBuffer(builder.build());
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
    // For DIFFERENTIAL updates, try to cancel a previously added trip
    if (incrementality != FULL_DATASET) {
      var addedPattern = snapshotManager.getNewTripPatternForModifiedTrip(
        tripUpdate.tripId(),
        tripUpdate.serviceDate()
      );
      if (addedPattern != null) {
        var timetable = snapshotManager.resolve(addedPattern, tripUpdate.serviceDate());
        if (timetable != null) {
          var tripTimes = timetable.getTripTimes(tripUpdate.tripId());
          if (tripTimes != null && tripTimes.getRealTimeState() == RealTimeState.ADDED) {
            var builder = tripTimes.createRealTimeFromScheduledTimes();
            switch (cancelationType) {
              case CANCEL -> builder.cancelTrip();
              case DELETE -> builder.deleteTrip();
            }
            return snapshotManager.updateBuffer(
              RealTimeTripUpdate.of(addedPattern, builder.build(), tripUpdate.serviceDate()).build()
            );
          }
        }
      }
    }

    // Cancel the scheduled trip
    var pattern = getPatternForTripId(tripUpdate.tripId());
    if (pattern == null) {
      return UpdateError.result(tripUpdate.tripId(), NO_TRIP_FOR_CANCELLATION_FOUND);
    }

    var tripTimes = pattern.getScheduledTimetable().getTripTimes(tripUpdate.tripId());
    if (tripTimes == null) {
      return UpdateError.result(tripUpdate.tripId(), NO_TRIP_FOR_CANCELLATION_FOUND);
    }

    var builder = tripTimes.createRealTimeFromScheduledTimes();
    switch (cancelationType) {
      case CANCEL -> builder.cancelTrip();
      case DELETE -> builder.deleteTrip();
    }
    return snapshotManager.updateBuffer(
      RealTimeTripUpdate.of(pattern, builder.build(), tripUpdate.serviceDate())
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );
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
