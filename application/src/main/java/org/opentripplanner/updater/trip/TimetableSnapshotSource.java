package org.opentripplanner.updater.trip;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_DUPLICATED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_UNSCHEDULED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_TRIP_FOR_CANCELLATION_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_VALID_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimaps;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import de.mfdz.MfdzRealtimeExtensions;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
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
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.GtfsRealtimeMapper;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class should be used to create snapshots of lookup tables of realtime data. This is
 * necessary to provide planning threads a consistent constant view of a graph with realtime data at
 * a specific point in time.
 */
public class TimetableSnapshotSource implements TimetableSnapshotProvider {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshotSource.class);

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

  public TimetableSnapshotSource(
    TimetableSnapshotSourceParameters parameters,
    TimetableRepository timetableRepository
  ) {
    this(parameters, timetableRepository, () -> LocalDate.now(timetableRepository.getTimeZone()));
  }

  /**
   * Constructor is package local to allow unit-tests to provide their own clock, not using system
   * time.
   */
  TimetableSnapshotSource(
    TimetableSnapshotSourceParameters parameters,
    TimetableRepository timetableRepository,
    Supplier<LocalDate> localDateNow
  ) {
    this.snapshotManager =
      new TimetableSnapshotManager(
        timetableRepository.getTransitLayerUpdater(),
        parameters,
        localDateNow
      );
    this.timeZone = timetableRepository.getTimeZone();
    this.transitEditorService =
      new DefaultTransitService(timetableRepository, snapshotManager.getTimetableSnapshotBuffer());
    this.deduplicator = timetableRepository.getDeduplicator();
    this.serviceCodes = timetableRepository.getServiceCodes();
    this.localDateNow = localDateNow;

    // Inject this into the transit model
    timetableRepository.initTimetableSnapshotProvider(this);
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
    GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    UpdateIncrementality updateIncrementality,
    List<TripUpdate> updates,
    String feedId
  ) {
    if (updates == null) {
      LOG.warn("updates is null");
      return UpdateResult.empty();
    }

    Map<ScheduleRelationship, Integer> failuresByRelationship = new HashMap<>();
    List<Result<UpdateSuccess, UpdateError>> results = new ArrayList<>();

    if (updateIncrementality == FULL_DATASET) {
      // Remove all updates from the buffer
      snapshotManager.clearBuffer(feedId);
    }

    LOG.debug("message contains {} trip updates", updates.size());
    int uIndex = 0;
    for (TripUpdate tripUpdate : updates) {
      if (!tripUpdate.hasTrip()) {
        debug(feedId, "", "Missing TripDescriptor in gtfs-rt trip update: \n{}", tripUpdate);
        continue;
      }

      if (fuzzyTripMatcher != null) {
        final TripDescriptor trip = fuzzyTripMatcher.match(feedId, tripUpdate.getTrip());
        tripUpdate = tripUpdate.toBuilder().setTrip(trip).build();
      }

      final TripDescriptor tripDescriptor = tripUpdate.getTrip();

      if (!tripDescriptor.hasTripId() || tripDescriptor.getTripId().isBlank()) {
        debug(feedId, "", "No trip id found for gtfs-rt trip update: \n{}", tripUpdate);
        results.add(Result.failure(UpdateError.noTripId(INVALID_INPUT_STRUCTURE)));
        continue;
      }

      FeedScopedId tripId = new FeedScopedId(feedId, tripUpdate.getTrip().getTripId());

      LocalDate serviceDate;
      if (tripDescriptor.hasStartDate()) {
        try {
          serviceDate = ServiceDateUtils.parseString(tripDescriptor.getStartDate());
        } catch (final ParseException e) {
          debug(
            tripId,
            "Failed to parse start date in gtfs-rt trip update: {}",
            tripDescriptor.getStartDate()
          );
          continue;
        }
      } else {
        // TODO: figure out the correct service date. For the special case that a trip
        // starts for example at 40:00, yesterday would probably be a better guess.
        serviceDate = localDateNow.get();
      }
      // Determine what kind of trip update this is
      var scheduleRelationship = Objects.requireNonNullElse(
        tripDescriptor.getScheduleRelationship(),
        SCHEDULED
      );
      if (updateIncrementality == DIFFERENTIAL) {
        purgePatternModifications(scheduleRelationship, tripId, serviceDate);
      }

      uIndex += 1;
      LOG.debug("trip update #{} ({} updates) :", uIndex, tripUpdate.getStopTimeUpdateCount());
      LOG.trace("{}", tripUpdate);

      Result<UpdateSuccess, UpdateError> result;
      try {
        result =
          switch (scheduleRelationship) {
            case SCHEDULED -> handleScheduledTrip(
              tripUpdate,
              tripId,
              serviceDate,
              backwardsDelayPropagationType
            );
            case ADDED -> validateAndHandleAddedTrip(
              tripUpdate,
              tripDescriptor,
              tripId,
              serviceDate
            );
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
            case REPLACEMENT -> validateAndHandleModifiedTrip(
              tripUpdate,
              tripDescriptor,
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
        debug(tripId, "Failed to apply TripUpdate.");
        LOG.trace(" Contents: {}", tripUpdate);
        if (failuresByRelationship.containsKey(scheduleRelationship)) {
          var c = failuresByRelationship.get(scheduleRelationship);
          failuresByRelationship.put(scheduleRelationship, ++c);
        } else {
          failuresByRelationship.put(scheduleRelationship, 1);
        }
      }
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
   * ScheduleRelationship ADDED and is now cancelled or deleted, we still want to keep the realtime
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
      (
        tripScheduleRelationship != ScheduleRelationship.CANCELED &&
        tripScheduleRelationship != ScheduleRelationship.DELETED
      )
    ) {
      // Remove previous realtime updates for this trip. This is necessary to avoid previous
      // stop pattern modifications from persisting. If a trip was previously added with the ScheduleRelationship
      // ADDED and is now cancelled or deleted, we still want to keep the realtime added trip pattern.
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

  @Override
  public TimetableSnapshot getTimetableSnapshot() {
    return snapshotManager.getTimetableSnapshot();
  }

  /**
   * @return the current timetable snapshot buffer that contains pending changes (not yet published
   * in a snapshot). This should be used in the context of an updater to build  a TransitEditorService
   * that sees all the changes applied so far by real-time updates.
   */
  public TimetableSnapshot getTimetableSnapshotBuffer() {
    return snapshotManager.getTimetableSnapshotBuffer();
  }

  private static void logUpdateResult(
    String feedId,
    Map<ScheduleRelationship, Integer> failuresByRelationship,
    UpdateResult updateResult
  ) {
    ResultLogger.logUpdateResult(feedId, "gtfs-rt-trip-updates", updateResult);

    if (!failuresByRelationship.isEmpty()) {
      LOG.info("[feedId: {}] Failures by scheduleRelationship {}", feedId, failuresByRelationship);
    }

    var warnings = Multimaps.index(updateResult.warnings(), w -> w);
    warnings
      .keySet()
      .forEach(key -> {
        var count = warnings.get(key).size();
        LOG.info("[feedId: {}] {} warnings of type {}", feedId, count, key);
      });
  }

  private Result<UpdateSuccess, UpdateError> handleScheduledTrip(
    TripUpdate tripUpdate,
    FeedScopedId tripId,
    LocalDate serviceDate,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    final TripPattern pattern = getPatternForTripId(tripId);

    if (pattern == null) {
      debug(tripId, "No pattern found for tripId, skipping TripUpdate.");
      return UpdateError.result(tripId, TRIP_NOT_FOUND);
    }

    if (tripUpdate.getStopTimeUpdateCount() < 1) {
      debug(tripId, "TripUpdate contains no updates, skipping.");
      return UpdateError.result(tripId, NO_UPDATES);
    }

    final FeedScopedId serviceId = transitEditorService.getTripForId(tripId).getServiceId();
    final Set<LocalDate> serviceDates = transitEditorService
      .getCalendarService()
      .getServiceDatesForServiceId(serviceId);
    if (!serviceDates.contains(serviceDate)) {
      debug(
        tripId,
        "SCHEDULED trip has service date {} for which trip's service is not valid, skipping.",
        serviceDate.toString()
      );
      return UpdateError.result(tripId, NO_SERVICE_ON_DATE);
    }

    // Get new TripTimes based on scheduled timetable
    var result = pattern
      .getScheduledTimetable()
      .createUpdatedTripTimesFromGTFSRT(
        tripUpdate,
        timeZone,
        serviceDate,
        backwardsDelayPropagationType
      );

    if (result.isFailure()) {
      // necessary so the success type is correct
      return result.toFailureResult();
    }

    var tripTimesPatch = result.successValue();

    List<Integer> skippedStopIndices = tripTimesPatch.getSkippedStopIndices();

    var updatedTripTimes = tripTimesPatch.getTripTimes();

    // Make sure that updated trip times have the correct real time state
    updatedTripTimes.setRealTimeState(RealTimeState.UPDATED);

    // If there are skipped stops, we need to change the pattern from the scheduled one
    if (skippedStopIndices.size() > 0) {
      StopPattern newStopPattern = pattern
        .copyPlannedStopPattern()
        .cancelStops(skippedStopIndices)
        .build();

      final Trip trip = transitEditorService.getTripForId(tripId);
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
   * Validate and handle GTFS-RT TripUpdate message containing an ADDED trip.
   *
   * @param tripUpdate     GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> validateAndHandleAddedTrip(
    final TripUpdate tripUpdate,
    final TripDescriptor tripDescriptor,
    final FeedScopedId tripId,
    final LocalDate serviceDate
  ) {
    // Preconditions
    Objects.requireNonNull(tripUpdate);
    Objects.requireNonNull(serviceDate);

    //
    // Validate added trip
    //

    // Check whether trip id already exists in graph
    final Trip trip = transitEditorService.getScheduledTripForId(tripId);

    if (trip != null) {
      // TODO: should we support this and add a new instantiation of this trip (making it
      // frequency based)?
      debug(tripId, "Graph already contains trip id of ADDED trip, skipping.");
      return UpdateError.result(tripId, TRIP_ALREADY_EXISTS);
    }

    // Check whether a start date exists
    if (!tripDescriptor.hasStartDate()) {
      // TODO: should we support this and apply update to all days?
      debug(tripId, "ADDED trip doesn't have a start date in TripDescriptor, skipping.");
      return UpdateError.result(tripId, NO_START_DATE);
    }

    final List<StopTimeUpdate> stopTimeUpdates = removeUnknownStops(tripUpdate, tripId);

    var warnings = new ArrayList<UpdateSuccess.WarningType>(0);

    if (stopTimeUpdates.size() < tripUpdate.getStopTimeUpdateCount()) {
      warnings.add(UpdateSuccess.WarningType.UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP);
    }

    // check if after filtering the stops we still have at least 2
    if (stopTimeUpdates.size() < 2) {
      debug(tripId, "ADDED trip has fewer than two known stops, skipping.");
      return UpdateError.result(tripId, TOO_FEW_STOPS);
    }

    // Check whether all stop times are available and all stops exist
    final var stops = checkNewStopTimeUpdatesAndFindStops(tripId, stopTimeUpdates);
    if (stops == null) {
      return UpdateError.result(tripId, NO_VALID_STOPS);
    }

    //
    // Handle added trip
    //
    return handleAddedTrip(tripUpdate, stopTimeUpdates, tripDescriptor, stops, tripId, serviceDate)
      .mapSuccess(s -> s.addWarnings(warnings));
  }

  /**
   * Remove any stop that is not know in the static transit data.
   */
  private List<StopTimeUpdate> removeUnknownStops(TripUpdate tripUpdate, FeedScopedId tripId) {
    return tripUpdate
      .getStopTimeUpdateList()
      .stream()
      .filter(StopTimeUpdate::hasStopId)
      .filter(st -> {
        var stopId = new FeedScopedId(tripId.getFeedId(), st.getStopId());
        var stopFound = transitEditorService.getRegularStop(stopId) != null;
        if (!stopFound) {
          debug(tripId, "Stop '{}' not found in graph. Removing from ADDED trip.", st.getStopId());
        }
        return stopFound;
      })
      .toList();
  }

  /**
   * Check stop time updates of trip update that results in a new trip (ADDED or MODIFIED) and find
   * all stops of that trip.
   *
   * @return stops when stop time updates are correct; null if there are errors
   */
  private List<StopLocation> checkNewStopTimeUpdatesAndFindStops(
    final FeedScopedId tripId,
    final List<StopTimeUpdate> stopTimeUpdates
  ) {
    Integer previousStopSequence = null;
    Long previousTime = null;
    final List<StopLocation> stops = new ArrayList<>(stopTimeUpdates.size());

    for (int index = 0; index < stopTimeUpdates.size(); ++index) {
      final StopTimeUpdate stopTimeUpdate = stopTimeUpdates.get(index);

      // Check stop sequence
      if (stopTimeUpdate.hasStopSequence()) {
        final Integer stopSequence = stopTimeUpdate.getStopSequence();

        // Check non-negative
        if (stopSequence < 0) {
          debug(tripId, "Trip update contains negative stop sequence, skipping.");
          return null;
        }

        // Check whether sequence is increasing
        if (previousStopSequence != null && previousStopSequence > stopSequence) {
          debug(tripId, "Trip update contains decreasing stop sequence, skipping.");
          return null;
        }
        previousStopSequence = stopSequence;
      } else {
        // Allow missing stop sequences for ADDED and MODIFIED trips
      }

      // Find stops
      if (stopTimeUpdate.hasStopId()) {
        // Find stop
        final var stop = transitEditorService.getRegularStop(
          new FeedScopedId(tripId.getFeedId(), stopTimeUpdate.getStopId())
        );
        if (stop != null) {
          // Remember stop
          stops.add(stop);
        } else {
          debug(
            tripId,
            "Graph doesn't contain stop id '{}' of trip update, skipping.",
            stopTimeUpdate.getStopId()
          );
          return null;
        }
      } else {
        debug(tripId, "Trip update misses a stop id at stop time list index {}, skipping.", index);
        return null;
      }

      // Check arrival time
      if (stopTimeUpdate.hasArrival() && stopTimeUpdate.getArrival().hasTime()) {
        // Check for increasing time
        final Long time = stopTimeUpdate.getArrival().getTime();
        if (previousTime != null && previousTime > time) {
          debug(tripId, "Trip update contains decreasing times, skipping.");
          return null;
        }
        previousTime = time;
      } else {
        debug(tripId, "Trip update misses arrival time, skipping.");
        return null;
      }

      // Check departure time
      if (stopTimeUpdate.hasDeparture() && stopTimeUpdate.getDeparture().hasTime()) {
        // Check for increasing time
        final Long time = stopTimeUpdate.getDeparture().getTime();
        if (previousTime != null && previousTime > time) {
          debug(tripId, "Trip update contains decreasing times, skipping.");
          return null;
        }
        previousTime = time;
      } else {
        debug(tripId, "Trip update misses departure time, skipping.");
        return null;
      }
    }
    return stops;
  }

  /**
   * Handle GTFS-RT TripUpdate message containing an ADDED trip.
   *
   * @param stopTimeUpdates GTFS-RT stop time updates
   * @param tripDescriptor  GTFS-RT TripDescriptor
   * @param stops           the stops of each StopTimeUpdate in the TripUpdate message
   * @param serviceDate     service date for added trip
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> handleAddedTrip(
    final TripUpdate tripUpdate,
    final List<StopTimeUpdate> stopTimeUpdates,
    final TripDescriptor tripDescriptor,
    final List<StopLocation> stops,
    final FeedScopedId tripId,
    final LocalDate serviceDate
  ) {
    // Preconditions
    Objects.requireNonNull(stops);
    Preconditions.checkArgument(
      stopTimeUpdates.size() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    Route route;
    boolean routeExists = routeExists(tripId.getFeedId(), tripDescriptor);
    if (routeExists) {
      route =
        transitEditorService.getRouteForId(
          new FeedScopedId(tripId.getFeedId(), tripDescriptor.getRouteId())
        );
    } else {
      route = createRoute(tripDescriptor, tripId);
    }

    // Create new Trip

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
        "ADDED trip has service date {} for which no service id is available, skipping.",
        serviceDate.toString()
      );
      return UpdateError.result(tripId, NO_SERVICE_ON_DATE);
    } else {
      // Just use first service id of set
      tripBuilder.withServiceId(serviceIds.iterator().next());
    }
    return addTripToGraphAndBuffer(
      tripBuilder.build(),
      tripUpdate.getVehicle(),
      stopTimeUpdates,
      stops,
      serviceDate,
      RealTimeState.ADDED,
      !routeExists
    );
  }

  private Route createRoute(TripDescriptor tripDescriptor, FeedScopedId tripId) {
    // the route in this update doesn't already exist, but the update contains the information so it will be created
    if (
      tripDescriptor.hasExtension(MfdzRealtimeExtensions.tripDescriptor) &&
      !routeExists(tripId.getFeedId(), tripDescriptor)
    ) {
      FeedScopedId routeId = new FeedScopedId(tripId.getFeedId(), tripDescriptor.getRouteId());

      var builder = Route.of(routeId);

      var addedRouteExtension = AddedRoute.ofTripDescriptor(tripDescriptor);

      var agency = transitEditorService
        .findAgencyById(new FeedScopedId(tripId.getFeedId(), addedRouteExtension.agencyId()))
        .orElseGet(() -> fallbackAgency(tripId.getFeedId()));

      builder.withAgency(agency);

      builder.withGtfsType(addedRouteExtension.routeType());
      var mode = TransitModeMapper.mapMode(addedRouteExtension.routeType());
      builder.withMode(mode);

      // Create route name
      var name = Objects.requireNonNullElse(addedRouteExtension.routeLongName(), tripId.toString());
      builder.withLongName(new NonLocalizedString(name));
      builder.withUrl(addedRouteExtension.routeUrl());
      return builder.build();
    }
    // no information about the rout is given, so we create a dummy one
    else {
      var builder = Route.of(tripId);

      builder.withAgency(fallbackAgency(tripId.getFeedId()));
      // Guess the route type as it doesn't exist yet in the specifications
      // Bus. Used for short- and long-distance bus routes.
      builder.withGtfsType(3);
      builder.withMode(TransitMode.BUS);
      // Create route name
      I18NString longName = NonLocalizedString.ofNullable(tripDescriptor.getTripId());
      builder.withLongName(longName);
      return builder.build();
    }
  }

  /**
   * Create dummy agency for added trips.
   */
  private Agency fallbackAgency(String feedId) {
    return Agency
      .of(new FeedScopedId(feedId, "autogenerated-gtfs-rt-added-route"))
      .withName("Agency automatically added by GTFS-RT update")
      .withTimezone(transitEditorService.getTimeZone().toString())
      .build();
  }

  private boolean routeExists(String feedId, TripDescriptor tripDescriptor) {
    if (tripDescriptor.hasRouteId() && StringUtils.hasValue(tripDescriptor.getRouteId())) {
      var routeId = new FeedScopedId(feedId, tripDescriptor.getRouteId());
      return Objects.nonNull(transitEditorService.getRouteForId(routeId));
    } else {
      return false;
    }
  }

  /**
   * Add a (new) trip to the graph and the buffer
   *
   * @param trip              trip
   * @param vehicleDescriptor accessibility information of the vehicle
   * @param stops             list of stops corresponding to stop time updates
   * @param serviceDate       service date of trip
   * @param realTimeState     real-time state of new trip
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> addTripToGraphAndBuffer(
    final Trip trip,
    final GtfsRealtime.VehicleDescriptor vehicleDescriptor,
    final List<StopTimeUpdate> stopTimeUpdates,
    final List<StopLocation> stops,
    final LocalDate serviceDate,
    final RealTimeState realTimeState,
    final boolean isAddedRoute
  ) {
    // Preconditions
    Objects.requireNonNull(stops);
    Preconditions.checkArgument(
      stopTimeUpdates.size() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Calculate seconds since epoch on GTFS midnight (noon minus 12h) of service date
    final long midnightSecondsSinceEpoch = ServiceDateUtils
      .asStartOfService(serviceDate, timeZone)
      .toEpochSecond();

    // Create StopTimes
    final List<StopTime> stopTimes = new ArrayList<>(stopTimeUpdates.size());
    for (int index = 0; index < stopTimeUpdates.size(); ++index) {
      final StopTimeUpdate stopTimeUpdate = stopTimeUpdates.get(index);
      final var stop = stops.get(index);

      // Create stop time
      final StopTime stopTime = new StopTime();
      stopTime.setTrip(trip);
      stopTime.setStop(stop);
      // Set arrival time
      if (stopTimeUpdate.hasArrival() && stopTimeUpdate.getArrival().hasTime()) {
        final long arrivalTime = stopTimeUpdate.getArrival().getTime() - midnightSecondsSinceEpoch;
        if (arrivalTime < 0 || arrivalTime > MAX_ARRIVAL_DEPARTURE_TIME) {
          debug(
            trip.getId(),
            "ADDED trip has invalid arrival time (compared to start date in " +
            "TripDescriptor), skipping."
          );
          return UpdateError.result(trip.getId(), INVALID_ARRIVAL_TIME);
        }
        stopTime.setArrivalTime((int) arrivalTime);
      }
      // Set departure time
      if (stopTimeUpdate.hasDeparture() && stopTimeUpdate.getDeparture().hasTime()) {
        final long departureTime =
          stopTimeUpdate.getDeparture().getTime() - midnightSecondsSinceEpoch;
        if (departureTime < 0 || departureTime > MAX_ARRIVAL_DEPARTURE_TIME) {
          debug(
            trip.getId(),
            "ADDED trip has invalid departure time (compared to start date in " +
            "TripDescriptor), skipping."
          );
          return UpdateError.result(trip.getId(), INVALID_DEPARTURE_TIME);
        }
        stopTime.setDepartureTime((int) departureTime);
      }
      stopTime.setTimepoint(1); // Exact time
      if (stopTimeUpdate.hasStopSequence()) {
        stopTime.setStopSequence(stopTimeUpdate.getStopSequence());
      }
      var added = AddedStopTime.ofStopTime(stopTimeUpdate);
      stopTime.setPickupType(added.pickup());
      stopTime.setDropOffType(added.dropOff());
      // Add stop time to list
      stopTimes.add(stopTime);
    }

    // TODO: filter/interpolate stop times like in PatternHopFactory?

    // Create StopPattern
    final StopPattern stopPattern = new StopPattern(stopTimes);

    final TripPattern originalTripPattern = transitEditorService.getPatternForTrip(trip);
    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      stopPattern,
      trip,
      originalTripPattern
    );

    // Create new trip times
    final RealTimeTripTimes newTripTimes = TripTimesFactory.tripTimes(
      trip,
      stopTimes,
      deduplicator
    );

    // Update all times to mark trip times as realtime
    // TODO: should we incorporate the delay field if present?
    for (int stopIndex = 0; stopIndex < newTripTimes.getNumStops(); stopIndex++) {
      newTripTimes.updateArrivalTime(stopIndex, newTripTimes.getScheduledArrivalTime(stopIndex));
      newTripTimes.updateDepartureTime(
        stopIndex,
        newTripTimes.getScheduledDepartureTime(stopIndex)
      );
    }

    // Set service code of new trip times
    final int serviceCode = serviceCodes.get(trip.getServiceId());
    newTripTimes.setServiceCode(serviceCode);

    // Make sure that updated trip times have the correct real time state
    newTripTimes.setRealTimeState(realTimeState);

    if (vehicleDescriptor != null) {
      if (vehicleDescriptor.hasWheelchairAccessible()) {
        GtfsRealtimeMapper
          .mapWheelchairAccessible(vehicleDescriptor.getWheelchairAccessible())
          .ifPresent(newTripTimes::updateWheelchairAccessibility);
      }
    }
    LOG.trace(
      "Trip pattern added with mode {} on {} from {} to {}",
      trip.getRoute().getMode(),
      serviceDate,
      pattern.firstStop().getName(),
      pattern.lastStop().getName()
    );
    // Add new trip times to the buffer
    // TODO add support for TripOnServiceDate for GTFS-RT
    return snapshotManager.updateBuffer(
      new RealTimeTripUpdate(
        pattern,
        newTripTimes,
        serviceDate,
        null,
        realTimeState == RealTimeState.ADDED,
        isAddedRoute
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
      final int tripIndex = timetable.getTripIndex(tripId);
      if (tripIndex == -1) {
        debug(tripId, "Could not cancel scheduled trip because it's not in the timetable");
      } else {
        final RealTimeTripTimes newTripTimes = timetable
          .getTripTimes(tripIndex)
          .copyScheduledTimes();
        switch (cancelationType) {
          case CANCEL -> newTripTimes.cancelTrip();
          case DELETE -> newTripTimes.deleteTrip();
        }
        snapshotManager.updateBuffer(new RealTimeTripUpdate(pattern, newTripTimes, serviceDate));
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
      final int tripIndex = timetable.getTripIndex(tripId);
      if (tripIndex == -1) {
        debug(tripId, "Could not cancel previously added trip on {}", serviceDate);
      } else {
        final RealTimeTripTimes newTripTimes = timetable
          .getTripTimes(tripIndex)
          .copyScheduledTimes();
        switch (cancelationType) {
          case CANCEL -> newTripTimes.cancelTrip();
          case DELETE -> newTripTimes.deleteTrip();
        }
        snapshotManager.updateBuffer(new RealTimeTripUpdate(pattern, newTripTimes, serviceDate));
        cancelledAddedTrip = true;
      }
    }
    return cancelledAddedTrip;
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing a MODIFIED trip.
   *
   * @param tripUpdate     GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> validateAndHandleModifiedTrip(
    final TripUpdate tripUpdate,
    final TripDescriptor tripDescriptor,
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
    Trip trip = transitEditorService.getTripForId(tripId);

    if (trip == null) {
      // TODO: should we support this and consider it an ADDED trip?
      debug(tripId, "Feed does not contain trip id of MODIFIED trip, skipping.");
      return UpdateError.result(tripId, TRIP_NOT_FOUND);
    }

    // Check whether a start date exists
    if (!tripDescriptor.hasStartDate()) {
      // TODO: should we support this and apply update to all days?
      debug(tripId, "REPLACEMENT trip doesn't have a start date in TripDescriptor, skipping.");
      return UpdateError.result(tripId, NO_START_DATE);
    } else {
      // Check whether service date is served by trip
      final Set<FeedScopedId> serviceIds = transitEditorService
        .getCalendarService()
        .getServiceIdsOnDate(serviceDate);
      if (!serviceIds.contains(trip.getServiceId())) {
        // TODO: should we support this and change service id of trip?
        debug(tripId, "REPLACEMENT trip has a service date that is not served by trip, skipping.");
        return UpdateError.result(tripId, NO_SERVICE_ON_DATE);
      }
    }

    // Check whether at least two stop updates exist
    if (tripUpdate.getStopTimeUpdateCount() < 2) {
      debug(tripId, "REPLACEMENT trip has less then two stops, skipping.");
      return UpdateError.result(tripId, TOO_FEW_STOPS);
    }

    // Check whether all stop times are available and all stops exist
    var stops = checkNewStopTimeUpdatesAndFindStops(tripId, tripUpdate.getStopTimeUpdateList());
    if (stops == null) {
      return UpdateError.result(tripId, NO_VALID_STOPS);
    }

    //
    // Handle modified trip
    //

    return handleModifiedTrip(trip, tripUpdate, stops, serviceDate);
  }

  /**
   * Handle GTFS-RT TripUpdate message containing a REPLACEMENT trip.
   *
   * @param trip        trip that is modified
   * @param tripUpdate  GTFS-RT TripUpdate message
   * @param stops       the stops of each StopTimeUpdate in the TripUpdate message
   * @param serviceDate service date for modified trip
   * @return empty Result if successful or one containing an error
   */
  private Result<UpdateSuccess, UpdateError> handleModifiedTrip(
    final Trip trip,
    final TripUpdate tripUpdate,
    final List<StopLocation> stops,
    final LocalDate serviceDate
  ) {
    // Preconditions
    Objects.requireNonNull(stops);
    Preconditions.checkArgument(
      tripUpdate.getStopTimeUpdateCount() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Mark scheduled trip as DELETED
    var tripId = trip.getId();
    cancelScheduledTrip(tripId, serviceDate, CancelationType.DELETE);

    // Add new trip
    return addTripToGraphAndBuffer(
      trip,
      tripUpdate.getVehicle(),
      tripUpdate.getStopTimeUpdateList(),
      stops,
      serviceDate,
      RealTimeState.MODIFIED,
      false
    );
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
      debug(tripId, "No pattern found for tripId. Skipping cancellation.");
      return UpdateError.result(tripId, NO_TRIP_FOR_CANCELLATION_FOUND);
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
    Trip trip = transitEditorService.getTripForId(tripId);
    return transitEditorService.getPatternForTrip(trip);
  }

  private static void debug(FeedScopedId id, String message, Object... params) {
    debug(id.getFeedId(), id.getId(), message, params);
  }

  private static void debug(String feedId, String tripId, String message, Object... params) {
    String m = "[feedId: %s, tripId: %s] %s".formatted(feedId, tripId, message);
    LOG.debug(m, params);
  }

  private enum CancelationType {
    CANCEL,
    DELETE,
  }

  public void flushBuffer() {
    snapshotManager.purgeAndCommit();
  }
}
