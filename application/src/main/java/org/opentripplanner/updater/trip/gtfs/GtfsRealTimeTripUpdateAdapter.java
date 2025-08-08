package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_DUPLICATED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_UNSCHEDULED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_TRIP_FOR_CANCELLATION_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;
import static org.opentripplanner.updater.trip.gtfs.TripTimesUpdater.getWheelchairAccessibility;

import com.google.common.collect.Multimaps;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
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
import org.opentripplanner.updater.trip.gtfs.model.AddedRoute;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Adapts from GTFS-RT TripUpdates to OTP's internal real-time data model.
 */
public class GtfsRealTimeTripUpdateAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsRealTimeTripUpdateAdapter.class);

  /**
   * Maximum time in seconds since midnight for arrivals and departures
   */
  private static final long MAX_ARRIVAL_DEPARTURE_TIME = 48 * 60 * 60;

  /** A synchronized cache of trip patterns added to the graph due to GTFS-realtime messages. */
  private final TripPatternCache tripPatternCache = new TripPatternCache();

  private final ZoneId timeZone;

  /**
   * Long-lived transit editor service that has access to the timetable snapshot buffer.
   * This differs from the usual use case where the transit service refers to the latest published
   * timetable snapshot.
   */
  private final TransitEditorService transitEditorService;

  private final Deduplicator deduplicator;

  private final Map<FeedScopedId, Integer> serviceCodes;

  private final TimetableSnapshotManager snapshotManager;
  private final Supplier<LocalDate> localDateNow;

  /**
   * Constructor to allow tests to provide their own clock, not using system time.
   */
  public GtfsRealTimeTripUpdateAdapter(
    TimetableRepository timetableRepository,
    TimetableSnapshotManager snapshotManager,
    Supplier<LocalDate> localDateNow
  ) {
    this.snapshotManager = snapshotManager;
    this.timeZone = timetableRepository.getTimeZone();
    this.localDateNow = localDateNow;
    this.transitEditorService = new DefaultTransitService(
      timetableRepository,
      snapshotManager.getTimetableSnapshotBuffer()
    );
    this.deduplicator = timetableRepository.getDeduplicator();
    this.serviceCodes = timetableRepository.getServiceCodes();
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
    Map<ScheduleRelationship, Integer> failuresByRelationship = new HashMap<>();
    List<Result<UpdateSuccess, UpdateError>> results = new ArrayList<>();

    if (updateIncrementality == FULL_DATASET) {
      // Remove all updates from the buffer
      snapshotManager.clearBuffer(feedId);
    }

    debug(feedId, "message contains {} trip updates", updates.size());
    for (var i = 0; i < updates.size(); ++i) {
      var uIndex = i;
      var rawTripUpdate = updates.get(uIndex);

      if (fuzzyTripMatcher != null) {
        final GtfsRealtime.TripDescriptor trip = fuzzyTripMatcher.match(
          feedId,
          rawTripUpdate.getTrip()
        );
        rawTripUpdate = rawTripUpdate.toBuilder().setTrip(trip).build();
      }

      var tripUpdate = new TripUpdate(rawTripUpdate);
      var tripDescriptor = tripUpdate.tripDescriptor();

      tripDescriptor
        .tripId()
        .filter(StringUtils::hasValue)
        .map(id -> new FeedScopedId(feedId, id))
        .ifPresentOrElse(
          tripId -> {
            LocalDate serviceDate;
            try {
              // TODO: figure out the correct service date. For the special case that a trip
              // starts for example at 40:00, yesterday would probably be a better guess.
              serviceDate = tripDescriptor.startDate().orElse(localDateNow.get());
            } catch (ParseException e) {
              debug(
                tripId,
                null,
                "Failed to parse start date in gtfs-rt trip update: {}",
                e.getMessage()
              );
              return;
            }
            // Determine what kind of trip update this is
            var scheduleRelationship = tripDescriptor.scheduleRelationship();
            if (updateIncrementality == DIFFERENTIAL) {
              purgePatternModifications(scheduleRelationship, tripId, serviceDate);
            }

            if (LOG.isTraceEnabled()) {
              trace(
                tripId,
                serviceDate,
                "trip update #{} ({} updates): {}",
                uIndex,
                updates.size(),
                tripUpdate
              );
            } else {
              debug(tripId, serviceDate, "trip update #{} ({} updates)", uIndex, updates.size());
            }

            Result<UpdateSuccess, UpdateError> result;
            try {
              result = switch (scheduleRelationship) {
                case SCHEDULED -> handleScheduledTrip(
                  tripUpdate,
                  tripId,
                  serviceDate,
                  forwardsDelayPropagationType,
                  backwardsDelayPropagationType
                );
                case NEW, ADDED -> validateAndHandleNewTrip(tripUpdate, tripId, serviceDate);
                case CANCELED -> handleCanceledTrip(
                  tripId,
                  serviceDate,
                  CancelationType.CANCEL,
                  updateIncrementality
                );
                case DELETED -> handleCanceledTrip(
                  tripId,
                  serviceDate,
                  CancelationType.DELETE,
                  updateIncrementality
                );
                case REPLACEMENT -> validateAndHandleReplacementTrip(
                  tripUpdate,
                  tripId,
                  serviceDate
                );
                case UNSCHEDULED -> UpdateError.result(tripId, NOT_IMPLEMENTED_UNSCHEDULED);
                case DUPLICATED -> UpdateError.result(tripId, NOT_IMPLEMENTED_DUPLICATED);
              };
            } catch (DataValidationException e) {
              result = DataValidationExceptionMapper.toResult(e);
            }

            results.add(result);
            if (result.isFailure()) {
              debug(tripId, serviceDate, "Failed to apply TripUpdate.");
              if (failuresByRelationship.containsKey(scheduleRelationship)) {
                var c = failuresByRelationship.get(scheduleRelationship);
                failuresByRelationship.put(scheduleRelationship, ++c);
              } else {
                failuresByRelationship.put(scheduleRelationship, 1);
              }
            }
          },
          () -> {
            debug(feedId, "No trip id found for gtfs-rt trip update: \n{}", tripUpdate);
            results.add(Result.failure(UpdateError.noTripId(INVALID_INPUT_STRUCTURE)));
          }
        );
    }

    var updateResult = UpdateResult.ofResults(results);

    if (updateIncrementality == FULL_DATASET) {
      logUpdateResult(feedId, failuresByRelationship, updateResult);
    }
    return updateResult;
  }

  /**
   * Remove previous realtime updates for this trip. This is necessary to avoid previous stop
   * pattern modifications from persisting. If a trip was previously added with the
   * ScheduleRelationship NEW and is now cancelled or deleted, we still want to keep the realtime
   * added trip pattern.
   */
  private void purgePatternModifications(
    ScheduleRelationship tripScheduleRelationship,
    FeedScopedId tripId,
    LocalDate serviceDate
  ) {
    final TripPattern pattern = snapshotManager.getNewTripPatternForModifiedTrip(
      tripId,
      serviceDate
    );
    if (
      !isPreviouslyAddedTrip(tripId, pattern, serviceDate) ||
      (tripScheduleRelationship != ScheduleRelationship.CANCELED &&
        tripScheduleRelationship != ScheduleRelationship.DELETED)
    ) {
      // Remove previous realtime updates for this trip. This is necessary to avoid previous
      // stop pattern modifications from persisting. If a trip was previously added with the ScheduleRelationship
      // NEW and is now cancelled or deleted, we still want to keep the realtime added trip pattern.
      this.snapshotManager.revertTripToScheduledTripPattern(tripId, serviceDate);
    }
  }

  private boolean isPreviouslyAddedTrip(
    FeedScopedId tripId,
    TripPattern pattern,
    LocalDate serviceDate
  ) {
    if (pattern == null) {
      return false;
    }
    var timetable = snapshotManager.resolve(pattern, serviceDate);
    if (timetable == null) {
      return false;
    }
    var tripTimes = timetable.getTripTimes(tripId);
    if (tripTimes == null) {
      return false;
    }
    return tripTimes.getRealTimeState() == RealTimeState.ADDED;
  }

  private static void logUpdateResult(
    String feedId,
    Map<ScheduleRelationship, Integer> failuresByRelationship,
    UpdateResult updateResult
  ) {
    ResultLogger.logUpdateResult(feedId, "gtfs-rt-trip-updates", updateResult);

    if (!failuresByRelationship.isEmpty()) {
      info(feedId, "Failures by scheduleRelationship {}", failuresByRelationship);
    }

    var warnings = Multimaps.index(updateResult.warnings(), w -> w);
    warnings
      .keySet()
      .forEach(key -> {
        var count = warnings.get(key).size();
        info(feedId, "{} warnings of type {}", count, key);
      });
  }

  private Result<UpdateSuccess, UpdateError> handleScheduledTrip(
    TripUpdate tripUpdate,
    FeedScopedId tripId,
    LocalDate serviceDate,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    final TripPattern pattern = getPatternForTripId(tripId);

    if (pattern == null) {
      debug(tripId, serviceDate, "No pattern found for tripId, skipping TripUpdate.");
      return UpdateError.result(tripId, TRIP_NOT_FOUND);
    }

    if (tripUpdate.stopTimeUpdates().isEmpty()) {
      debug(tripId, serviceDate, "TripUpdate contains no updates, skipping.");
      return UpdateError.result(tripId, NO_UPDATES);
    }

    final FeedScopedId serviceId = transitEditorService.getTrip(tripId).getServiceId();
    final Set<LocalDate> serviceDates = transitEditorService
      .getCalendarService()
      .getServiceDatesForServiceId(serviceId);
    if (!serviceDates.contains(serviceDate)) {
      debug(
        tripId,
        serviceDate,
        "SCHEDULED trip has service date {} for which trip's service is not valid, skipping.",
        serviceDate.toString()
      );
      return UpdateError.result(tripId, NO_SERVICE_ON_DATE);
    }

    // Get new TripTimes based on scheduled timetable
    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      pattern.getScheduledTimetable(),
      tripUpdate,
      timeZone,
      serviceDate,
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
        new FeedScopedId(tripId.getFeedId(), entry.getValue())
      );
      if (stop != null) {
        newStops.put(entry.getKey(), stop);
      } else {
        debug(
          tripId,
          serviceDate,
          "Graph doesn't contain assigned stop id '{}' at position '{}' for trip '{}' , skipping stop assignment.",
          entry.getValue(),
          entry.getKey(),
          tripId
        );
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

      final Trip trip = transitEditorService.getTrip(tripId);
      // Get cached trip pattern or create one if it doesn't exist yet
      final TripPattern newPattern = tripPatternCache.getOrCreateTripPattern(
        newStopPattern,
        trip,
        pattern
      );

      cancelScheduledTrip(tripId, serviceDate, CancelationType.DELETE);
      return snapshotManager.updateBuffer(
        new RealTimeTripUpdate(newPattern, updatedTripTimes, serviceDate)
      );
    } else {
      // Set the updated trip times in the buffer
      return snapshotManager.updateBuffer(
        new RealTimeTripUpdate(pattern, updatedTripTimes, serviceDate)
      );
    }
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing an NEW trip.
   *
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> validateAndHandleNewTrip(
    final TripUpdate tripUpdate,
    final FeedScopedId tripId,
    final LocalDate serviceDate
  ) {
    // Check whether trip id already exists in graph
    if (transitEditorService.getScheduledTrip(tripId) != null) {
      debug(tripId, serviceDate, "Graph already contains trip id of NEW trip, skipping.");
      return UpdateError.result(tripId, TRIP_ALREADY_EXISTS);
    }

    // Create new Trip
    var tripDescriptor = tripUpdate.tripDescriptor();

    var optionalRoute = getRoute(tripId.getFeedId(), tripDescriptor);
    var route = optionalRoute.orElseGet(() -> createRoute(tripDescriptor, tripId));

    // TODO: which Agency ID to use? Currently use feed id.
    var tripBuilder = Trip.of(tripId);
    tripBuilder.withRoute(route);

    // Find service ID running on this service date
    final Set<FeedScopedId> serviceIds = transitEditorService
      .getCalendarService()
      .getServiceIdsOnDate(serviceDate);
    if (serviceIds.isEmpty()) {
      // No service id exists: return error for now
      debug(
        tripId,
        serviceDate,
        "NEW trip has service date {} for which no service id is available, skipping.",
        serviceDate.toString()
      );
      return UpdateError.result(tripId, NO_SERVICE_ON_DATE);
    } else {
      // Just use first service id of set
      tripBuilder.withServiceId(serviceIds.iterator().next());
    }

    var tripHeadsign = tripUpdate.tripHeadsign();
    tripHeadsign.ifPresent(tripBuilder::withHeadsign);
    tripUpdate.tripShortName().ifPresent(tripBuilder::withShortName);

    Trip trip = tripBuilder.build();

    return handleNewOrReplacementTrip(
      trip,
      tripUpdate,
      serviceDate,
      RealTimeState.ADDED,
      optionalRoute.isEmpty()
    );
  }

  /**
   * Remove any stop that is not know in the static transit data.
   */
  private List<StopAndStopTimeUpdate> matchStopsToStopTimeUpdates(
    TripUpdate tripUpdate,
    FeedScopedId tripId,
    LocalDate serviceDate
  ) {
    return tripUpdate
      .stopTimeUpdates()
      .stream()
      .flatMap(st ->
        st
          .stopId()
          .flatMap(id -> {
            var stopId = new FeedScopedId(tripId.getFeedId(), id);
            var stop = transitEditorService.getRegularStop(stopId);
            if (stop == null) {
              debug(
                tripId,
                serviceDate,
                "Stop '{}' not found in graph. Removing from NEW trip.",
                stopId
              );
            }
            return stop == null
              ? Optional.empty()
              : Optional.of(new StopAndStopTimeUpdate(stop, st));
          })
          .stream()
      )
      .toList();
  }

  /**
   * Handle GTFS-RT TripUpdate message containing an NEW or REPLACEMENT trip.
   *
   * @param serviceDate     service date for added trip
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> handleNewOrReplacementTrip(
    Trip trip,
    TripUpdate tripUpdate,
    LocalDate serviceDate,
    RealTimeState realTimeState,
    boolean hasANewRouteBeenCreated
  ) {
    FeedScopedId tripId = trip.getId();
    var stopAndStopTimeUpdates = matchStopsToStopTimeUpdates(tripUpdate, tripId, serviceDate);

    var warnings = new ArrayList<UpdateSuccess.WarningType>(0);

    if (stopAndStopTimeUpdates.size() < tripUpdate.stopTimeUpdates().size()) {
      warnings.add(UpdateSuccess.WarningType.UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP);
    }

    // check if after filtering the stops we still have at least 2
    if (stopAndStopTimeUpdates.size() < 2) {
      debug(
        tripId,
        serviceDate,
        "NEW or REPLACEMENT trip has fewer than two known stops, skipping."
      );
      return UpdateError.result(tripId, TOO_FEW_STOPS);
    }

    var result = TripTimesUpdater.createNewTripTimesFromGtfsRt(
      trip,
      getWheelchairAccessibility(tripUpdate).orElse(null),
      stopAndStopTimeUpdates,
      timeZone,
      serviceDate,
      realTimeState,
      tripUpdate.tripHeadsign().orElse(null),
      deduplicator,
      serviceCodes.get(trip.getServiceId())
    );

    return result
      .flatMap(value ->
        addNewOrReplacementTripToSnapshot(
          value,
          serviceDate,
          realTimeState,
          hasANewRouteBeenCreated
        )
      )
      .mapSuccess(s -> s.addWarnings(warnings));
  }

  private Route createRoute(
    org.opentripplanner.updater.trip.gtfs.model.TripDescriptor tripDescriptor,
    FeedScopedId tripId
  ) {
    // the route in this update doesn't already exist, but the update contains the information so it will be created
    var routeId = tripDescriptor.routeId().map(id -> new FeedScopedId(tripId.getFeedId(), id));
    return routeId
      .map(id -> {
        var builder = Route.of(id);

        var addedRouteExtension = AddedRoute.ofTripDescriptor(tripDescriptor);

        var agency = transitEditorService
          .findAgency(new FeedScopedId(tripId.getFeedId(), addedRouteExtension.agencyId()))
          .orElseGet(() -> fallbackAgency(tripId.getFeedId()));

        builder.withAgency(agency);

        builder.withGtfsType(addedRouteExtension.routeType());
        var mode = TransitModeMapper.mapMode(addedRouteExtension.routeType());
        builder.withMode(mode);

        // Create route name
        var name = Objects.requireNonNullElse(
          addedRouteExtension.routeLongName(),
          tripId.toString()
        );
        builder.withLongName(new NonLocalizedString(name));
        builder.withUrl(addedRouteExtension.routeUrl());
        return builder.build();
      })
      .orElseGet(() -> {
        var builder = Route.of(tripId);

        builder.withAgency(fallbackAgency(tripId.getFeedId()));
        // Guess the route type as it doesn't exist yet in the specifications
        // Bus. Used for short- and long-distance bus routes.
        builder.withGtfsType(3);
        builder.withMode(TransitMode.BUS);
        // Create route name
        I18NString longName = NonLocalizedString.ofNullable(tripId.getId());
        builder.withLongName(longName);
        return builder.build();
      });
  }

  /**
   * Create dummy agency for added trips.
   */
  private Agency fallbackAgency(String feedId) {
    return Agency.of(new FeedScopedId(feedId, "autogenerated-gtfs-rt-added-route"))
      .withName("Agency automatically added by GTFS-RT update")
      .withTimezone(transitEditorService.getTimeZone().toString())
      .build();
  }

  private Optional<Route> getRoute(
    String feedId,
    org.opentripplanner.updater.trip.gtfs.model.TripDescriptor tripDescriptor
  ) {
    return tripDescriptor
      .routeId()
      .flatMap(id ->
        Optional.ofNullable(transitEditorService.getRoute(new FeedScopedId(feedId, id)))
      );
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

    final TripPattern originalTripPattern = transitEditorService.findPattern(trip);
    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      stopPattern,
      trip,
      originalTripPattern
    );

    trace(
      trip.getId(),
      serviceDate,
      "Trip pattern added with mode {} on {} from {} to {}",
      trip.getRoute().getMode(),
      serviceDate,
      pattern.firstStop().getName(),
      pattern.lastStop().getName()
    );

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
      if (tripTimes == null) {
        debug(
          tripId,
          serviceDate,
          "Could not cancel scheduled trip because it's not in the timetable"
        );
      } else {
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
  private boolean cancelPreviouslyAddedTrip(
    final FeedScopedId tripId,
    final LocalDate serviceDate,
    CancelationType cancelationType
  ) {
    boolean cancelledAddedTrip = false;

    final TripPattern pattern = snapshotManager.getNewTripPatternForModifiedTrip(
      tripId,
      serviceDate
    );
    if (isPreviouslyAddedTrip(tripId, pattern, serviceDate)) {
      // Cancel trip times for this trip in this pattern
      final Timetable timetable = snapshotManager.resolve(pattern, serviceDate);
      var tripTimes = timetable.getTripTimes(tripId);
      if (tripTimes == null) {
        debug(tripId, serviceDate, "Could not cancel previously added trip on {}", serviceDate);
      } else {
        cancelTrip(serviceDate, cancelationType, pattern, tripTimes);
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
    final TripUpdate tripUpdate,
    final FeedScopedId tripId,
    final LocalDate serviceDate
  ) {
    // Preconditions
    Objects.requireNonNull(tripUpdate);
    Objects.requireNonNull(serviceDate);

    //
    // Validate modified trip
    //

    // Check whether trip id already exists in graph
    Trip trip = transitEditorService.getTrip(tripId);

    if (trip == null) {
      debug(tripId, serviceDate, "Feed does not contain trip id of REPLACEMENT trip, skipping.");
      return UpdateError.result(tripId, TRIP_NOT_FOUND);
    }

    // Check whether service date is served by trip
    final Set<FeedScopedId> serviceIds = transitEditorService
      .getCalendarService()
      .getServiceIdsOnDate(serviceDate);
    if (!serviceIds.contains(trip.getServiceId())) {
      // TODO: should we support this and change service id of trip?
      debug(
        tripId,
        serviceDate,
        "REPLACEMENT trip has a service date that is not served by trip, skipping."
      );
      return UpdateError.result(tripId, NO_SERVICE_ON_DATE);
    }

    return handleNewOrReplacementTrip(trip, tripUpdate, serviceDate, RealTimeState.MODIFIED, false);
  }

  private Result<UpdateSuccess, UpdateError> handleCanceledTrip(
    FeedScopedId tripId,
    final LocalDate serviceDate,
    CancelationType cancelationType,
    UpdateIncrementality incrementality
  ) {
    var canceledPreviouslyAddedTrip =
      incrementality != FULL_DATASET &&
      cancelPreviouslyAddedTrip(tripId, serviceDate, cancelationType);

    // if previously an added trip was removed, there can't be a scheduled trip to remove
    if (canceledPreviouslyAddedTrip) {
      return Result.success(UpdateSuccess.noWarnings());
    }
    // Try to cancel scheduled trip
    final boolean cancelScheduledSuccess = cancelScheduledTrip(
      tripId,
      serviceDate,
      cancelationType
    );

    if (!cancelScheduledSuccess) {
      debug(tripId, serviceDate, "No pattern found for tripId. Skipping cancellation.");
      return UpdateError.result(tripId, NO_TRIP_FOR_CANCELLATION_FOUND);
    }

    debug(tripId, serviceDate, "Canceled trip");

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

  private static void debug(
    FeedScopedId id,
    @Nullable LocalDate serviceDate,
    String message,
    Object... params
  ) {
    log(Level.DEBUG, id.getFeedId(), id.getId(), serviceDate, message, params);
  }

  private static void debug(String feedId, String message, Object... params) {
    log(Level.DEBUG, feedId, null, null, message, params);
  }

  private static void trace(
    FeedScopedId id,
    @Nullable LocalDate serviceDate,
    String message,
    Object... params
  ) {
    log(Level.TRACE, id.getFeedId(), id.getId(), serviceDate, message, params);
  }

  private static void info(String feedId, String message, Object... params) {
    log(Level.INFO, feedId, null, null, message, params);
  }

  /**
   * This adds detailed per-update logging to allow tracking what feeds and updates were applied to
   * a given trip.
   * <p>
   * The INFO level is used for aggregated statistics, while DEBUG/TRACE is used to link specific
   * messages to a trip.
   */
  private static void log(
    Level logLevel,
    String feedId,
    @Nullable String tripId,
    @Nullable LocalDate serviceDate,
    String message,
    Object... params
  ) {
    if (LOG.isEnabledForLevel(logLevel)) {
      String m = tripId != null || serviceDate != null
        ? "[feedId: %s, tripId: %s, serviceDate: %s] %s".formatted(
            feedId,
            tripId,
            serviceDate,
            message
          )
        : "[feedId: %s] %s".formatted(feedId, message);
      LOG.makeLoggingEventBuilder(logLevel).log(m, params);
    }
  }

  private enum CancelationType {
    CANCEL,
    DELETE,
  }
}
