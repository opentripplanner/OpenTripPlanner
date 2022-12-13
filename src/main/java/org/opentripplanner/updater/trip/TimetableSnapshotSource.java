package org.opentripplanner.updater.trip;

import static org.opentripplanner.model.PickDrop.SCHEDULED;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_DUPLICATED;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NOT_IMPLEMENTED_UNSCHEDULED;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_TRIP_FOR_CANCELLATION_FOUND;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_VALID_STOPS;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;

import com.google.common.base.Preconditions;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.GtfsRealtimeMapper;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
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
   * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime
   * messages.
   */

  private final TripPatternCache tripPatternCache = new TripPatternCache();

  private final ZoneId timeZone;
  private final TransitService transitService;
  private final TransitLayerUpdater transitLayerUpdater;

  /**
   * If a timetable snapshot is requested less than this number of milliseconds after the previous
   * snapshot, just return the same one. Throttles the potentially resource-consuming task of
   * duplicating a TripPattern → Timetable map and indexing the new Timetables.
   */
  private final int maxSnapshotFrequencyMs;

  /**
   * The last committed snapshot that was handed off to a routing thread. This snapshot may be given
   * to more than one routing thread if the maximum snapshot frequency is exceeded.
   */
  private volatile TimetableSnapshot snapshot = null;

  /** Should expired realtime data be purged from the graph. */
  private final boolean purgeExpiredData;

  protected LocalDate lastPurgeDate = null;

  /** Epoch time in milliseconds at which the last snapshot was generated. */
  protected long lastSnapshotTime = -1;

  private final Deduplicator deduplicator;

  private final Map<FeedScopedId, Integer> serviceCodes;

  /**
   * We inject a provider to retrieve the current service-date(now). This enables us to unit-test
   * the purgeExpiredData feature.
   */
  private final Supplier<LocalDate> localDateNow;

  public TimetableSnapshotSource(
    TimetableSnapshotSourceParameters parameters,
    TransitModel transitModel
  ) {
    this(parameters, transitModel, () -> LocalDate.now(transitModel.getTimeZone()));
  }

  /**
   * Constructor is package local to allow unit-tests to provide their own clock, not using
   * system time.
   */
  TimetableSnapshotSource(
    TimetableSnapshotSourceParameters parameters,
    TransitModel transitModel,
    Supplier<LocalDate> localDateNow
  ) {
    this.timeZone = transitModel.getTimeZone();
    this.transitService = new DefaultTransitService(transitModel);
    this.transitLayerUpdater = transitModel.getTransitLayerUpdater();
    this.deduplicator = transitModel.getDeduplicator();
    this.serviceCodes = transitModel.getServiceCodes();
    this.maxSnapshotFrequencyMs = parameters.maxSnapshotFrequencyMs();
    this.purgeExpiredData = parameters.purgeExpiredData();
    this.localDateNow = localDateNow;

    // Inject this into the transit model
    transitModel.initTimetableSnapshotProvider(this);
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
   * Method to apply a trip update list to the most recent version of the timetable snapshot. A
   * GTFS-RT feed is always applied against a single static feed (indicated by feedId).
   * <p>
   * However, multi-feed support is not completed and we currently assume there is only one static
   * feed when matching IDs.
   *
   * @param backwardsDelayPropagationType Defines when delays are propagated to previous stops and
   *                                     if these stops are given the NO_DATA flag.
   * @param fullDataset true if the list with updates represent all updates that are active right
   *                    now, i.e. all previous updates should be disregarded
   * @param updates     GTFS-RT TripUpdate's that should be applied atomically
   */
  public UpdateResult applyTripUpdates(
    GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    boolean fullDataset,
    List<TripUpdate> updates,
    String feedId
  ) {
    if (updates == null) {
      LOG.warn("updates is null");
      return UpdateResult.empty();
    }

    // Acquire lock on buffer
    bufferLock.lock();

    Map<TripDescriptor.ScheduleRelationship, Integer> failuresByRelationship = new HashMap<>();
    List<Result<?, UpdateError>> results = new ArrayList<>();

    try {
      if (fullDataset) {
        // Remove all updates from the buffer
        buffer.clear(feedId);
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
          serviceDate = LocalDate.now(timeZone);
        }

        uIndex += 1;
        LOG.debug("trip update #{} ({} updates) :", uIndex, tripUpdate.getStopTimeUpdateCount());
        LOG.trace("{}", tripUpdate);

        // Determine what kind of trip update this is
        final TripDescriptor.ScheduleRelationship tripScheduleRelationship = determineTripScheduleRelationship(
          tripDescriptor
        );

        Result<?, UpdateError> result =
          switch (tripScheduleRelationship) {
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
            case CANCELED -> handleCanceledTrip(tripId, serviceDate);
            case REPLACEMENT -> validateAndHandleModifiedTrip(
              tripUpdate,
              tripDescriptor,
              tripId,
              serviceDate
            );
            case UNSCHEDULED -> UpdateError.result(tripId, NOT_IMPLEMENTED_UNSCHEDULED);
            case DUPLICATED -> UpdateError.result(tripId, NOT_IMPLEMENTED_DUPLICATED);
          };

        results.add(result);
        if (result.isFailure()) {
          debug(tripId, "Failed to apply TripUpdate.");
          LOG.trace(" Contents: {}", tripUpdate);
          if (failuresByRelationship.containsKey(tripScheduleRelationship)) {
            var c = failuresByRelationship.get(tripScheduleRelationship);
            failuresByRelationship.put(tripScheduleRelationship, ++c);
          } else {
            failuresByRelationship.put(tripScheduleRelationship, 1);
          }
        }
      }

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

    var updateResult = UpdateResult.ofResults(results);

    if (fullDataset) {
      LOG.info(
        "[feedId: {}] {} of {} update messages were applied successfully (success rate: {}%)",
        feedId,
        updateResult.successful(),
        updates.size(),
        DoubleUtils.roundTo2Decimals((double) updateResult.successful() / updates.size() * 100)
      );

      var errorIndex = updateResult.failures();

      errorIndex
        .keySet()
        .forEach(key -> {
          var value = errorIndex.get(key);
          var tripIds = value.stream().map(UpdateError::debugId).collect(Collectors.toSet());
          LOG.error("[feedId: {}] {} failures of type {}: {}", feedId, value.size(), key, tripIds);
        });

      if (!failuresByRelationship.isEmpty()) {
        LOG.info(
          "[feedId: {}] Failures by scheduleRelationship {}",
          feedId,
          failuresByRelationship
        );
      }
    }
    return updateResult;
  }

  private TimetableSnapshot getTimetableSnapshot(final boolean force) {
    final long now = System.currentTimeMillis();
    if (force || now - lastSnapshotTime > maxSnapshotFrequencyMs) {
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
   * Determine how the trip update should be handled.
   *
   * @param tripDescriptor trip descriptor
   * @return TripDescriptor.ScheduleRelationship indicating how the trip update should be handled
   */
  private TripDescriptor.ScheduleRelationship determineTripScheduleRelationship(
    final TripDescriptor tripDescriptor
  ) {
    // Assume default value
    TripDescriptor.ScheduleRelationship tripScheduleRelationship =
      TripDescriptor.ScheduleRelationship.SCHEDULED;

    // If trip update contains schedule relationship, use it
    if (tripDescriptor.hasScheduleRelationship()) {
      tripScheduleRelationship = tripDescriptor.getScheduleRelationship();
    }

    return tripScheduleRelationship;
  }

  private Result<?, UpdateError> handleScheduledTrip(
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

    // If this trip_id has been used for previously ADDED/MODIFIED trip message (e.g. when the sequence of stops has
    // changed, and is now changing back to the originally scheduled one) cancel that previously created trip.
    cancelPreviouslyAddedTrip(tripId, serviceDate);

    // Get new TripTimes based on scheduled timetable
    var result = pattern
      .getScheduledTimetable()
      .createUpdatedTripTimes(tripUpdate, timeZone, serviceDate, backwardsDelayPropagationType);

    if (result.isFailure()) {
      return result;
    }

    var tripTimesPatch = result.successValue();

    List<Integer> skippedStopIndices = tripTimesPatch.getSkippedStopIndices();

    var updatedTripTimes = tripTimesPatch.getTripTimes();

    // Make sure that updated trip times have the correct real time state
    updatedTripTimes.setRealTimeState(RealTimeState.UPDATED);

    // If there are skipped stops, we need to change the pattern from the scheduled one
    if (skippedStopIndices.size() > 0) {
      StopPattern newStopPattern = pattern
        .getStopPattern()
        .mutate()
        .cancelStops(skippedStopIndices)
        .build();

      final Trip trip = getTripForTripId(tripId);
      // Get cached trip pattern or create one if it doesn't exist yet
      final TripPattern newPattern = tripPatternCache.getOrCreateTripPattern(
        newStopPattern,
        trip,
        pattern
      );

      cancelScheduledTrip(tripId, serviceDate);
      return buffer.update(newPattern, updatedTripTimes, serviceDate);
    } else {
      // Set the updated trip times in the buffer
      return buffer.update(pattern, updatedTripTimes, serviceDate);
    }
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing an ADDED trip.
   *
   * @param tripUpdate     GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @return empty Result if successful or one containing an error
   */
  private Result<?, UpdateError> validateAndHandleAddedTrip(
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
    final Trip trip = getTripForTripId(tripId);

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

    // Check whether at least two stop updates exist
    if (tripUpdate.getStopTimeUpdateCount() < 2) {
      debug(tripId, "ADDED trip has fewer than two stops, skipping.");
      return UpdateError.result(tripId, TOO_FEW_STOPS);
    }

    // Check whether all stop times are available and all stops exist
    final var stops = checkNewStopTimeUpdatesAndFindStops(tripId, tripUpdate);
    if (stops == null) {
      return UpdateError.result(tripId, NO_VALID_STOPS);
    }

    //
    // Handle added trip
    //
    return handleAddedTrip(tripUpdate, tripDescriptor, stops, tripId, serviceDate);
  }

  /**
   * Check stop time updates of trip update that results in a new trip (ADDED or MODIFIED) and find
   * all stops of that trip.
   *
   * @return stops when stop time updates are correct; null if there are errors
   */
  private List<StopLocation> checkNewStopTimeUpdatesAndFindStops(
    final FeedScopedId tripId,
    final TripUpdate tripUpdate
  ) {
    Integer previousStopSequence = null;
    Long previousTime = null;
    final List<StopTimeUpdate> stopTimeUpdates = tripUpdate.getStopTimeUpdateList();
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
        final var stop = getStopForStopId(
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
   * @param tripUpdate     GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @param stops          the stops of each StopTimeUpdate in the TripUpdate message
   * @param serviceDate    service date for added trip
   * @return empty Result if successful or one containing an error
   */
  private Result<?, UpdateError> handleAddedTrip(
    final TripUpdate tripUpdate,
    final TripDescriptor tripDescriptor,
    final List<StopLocation> stops,
    final FeedScopedId tripId,
    final LocalDate serviceDate
  ) {
    // Preconditions
    Objects.requireNonNull(stops);
    Preconditions.checkArgument(
      tripUpdate.getStopTimeUpdateCount() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Check whether trip id has been used for previously ADDED trip message and cancel
    // previously created trip
    cancelPreviouslyAddedTrip(tripId, serviceDate);

    //
    // Create added trip
    //

    Route route = null;
    if (tripDescriptor.hasRouteId()) {
      // Try to find route
      route = getRouteForRouteId(new FeedScopedId(tripId.getFeedId(), tripDescriptor.getRouteId()));
    }

    if (route == null) {
      // Create new Route
      // Use route id of trip descriptor if available
      FeedScopedId id = tripDescriptor.hasRouteId()
        ? new FeedScopedId(tripId.getFeedId(), tripDescriptor.getRouteId())
        : tripId;

      var builder = Route.of(id);
      // Create dummy agency for added trips
      Agency dummyAgency = Agency
        .of(new FeedScopedId(tripId.getFeedId(), "Dummy"))
        .withName("Dummy")
        .withTimezone("Europe/Paris")
        .build();
      builder.withAgency(dummyAgency);
      // Guess the route type as it doesn't exist yet in the specifications
      // Bus. Used for short- and long-distance bus routes.
      builder.withGtfsType(3);
      builder.withMode(TransitMode.BUS);
      // Create route name
      I18NString longName = NonLocalizedString.ofNullable(tripDescriptor.getTripId());
      builder.withLongName(longName);
      route = builder.build();
    }

    // Create new Trip

    // TODO: which Agency ID to use? Currently use feed id.
    var tripBuilder = Trip.of(tripId);
    tripBuilder.withRoute(route);

    // Find service ID running on this service date
    final Set<FeedScopedId> serviceIds = transitService
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
      tripUpdate,
      stops,
      serviceDate,
      RealTimeState.ADDED
    );
  }

  /**
   * Add a (new) trip to the graph and the buffer
   *
   * @param trip          trip
   * @param tripUpdate    trip update containing stop time updates
   * @param stops         list of stops corresponding to stop time updates
   * @param serviceDate   service date of trip
   * @param realTimeState real-time state of new trip
   * @return empty Result if successful or one containing an error
   */
  private Result<?, UpdateError> addTripToGraphAndBuffer(
    final Trip trip,
    final TripUpdate tripUpdate,
    final List<StopLocation> stops,
    final LocalDate serviceDate,
    final RealTimeState realTimeState
  ) {
    // Preconditions
    Objects.requireNonNull(stops);
    Preconditions.checkArgument(
      tripUpdate.getStopTimeUpdateCount() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Calculate seconds since epoch on GTFS midnight (noon minus 12h) of service date
    final long midnightSecondsSinceEpoch = ServiceDateUtils
      .asStartOfService(serviceDate, timeZone)
      .toEpochSecond();

    // Create StopTimes
    final List<StopTime> stopTimes = new ArrayList<>(tripUpdate.getStopTimeUpdateCount());
    for (int index = 0; index < tripUpdate.getStopTimeUpdateCount(); ++index) {
      final StopTimeUpdate stopTimeUpdate = tripUpdate.getStopTimeUpdate(index);
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
      stopTime.setPickupType(SCHEDULED); // Regularly scheduled pickup
      stopTime.setDropOffType(SCHEDULED); // Regularly scheduled drop off
      // Add stop time to list
      stopTimes.add(stopTime);
    }

    // TODO: filter/interpolate stop times like in PatternHopFactory?

    // Create StopPattern
    final StopPattern stopPattern = new StopPattern(stopTimes);

    final TripPattern originalTripPattern = transitService.getPatternForTrip(trip);
    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      stopPattern,
      trip,
      originalTripPattern
    );

    // Create new trip times
    final TripTimes newTripTimes = new TripTimes(trip, stopTimes, deduplicator);

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

    if (tripUpdate.hasVehicle()) {
      var vehicleDescriptor = tripUpdate.getVehicle();
      if (vehicleDescriptor.hasWheelchairAccessible()) {
        GtfsRealtimeMapper
          .mapWheelchairAccessible(vehicleDescriptor.getWheelchairAccessible())
          .ifPresent(newTripTimes::updateWheelchairAccessibility);
      }
    }

    // Add new trip times to the buffer
    return buffer.update(pattern, newTripTimes, serviceDate);
  }

  /**
   * Cancel scheduled trip in buffer given trip id  on service date
   *
   * @param tripId      trip id
   * @param serviceDate service date
   * @return true if scheduled trip was cancelled
   */
  private boolean cancelScheduledTrip(final FeedScopedId tripId, final LocalDate serviceDate) {
    boolean success = false;

    final TripPattern pattern = getPatternForTripId(tripId);

    if (pattern != null) {
      // Cancel scheduled trip times for this trip in this pattern
      final Timetable timetable = pattern.getScheduledTimetable();
      final int tripIndex = timetable.getTripIndex(tripId);
      if (tripIndex == -1) {
        debug(tripId, "Could not cancel scheduled trip because it's not in the timetable");
      } else {
        final TripTimes newTripTimes = new TripTimes(timetable.getTripTimes(tripIndex));
        newTripTimes.cancelTrip();
        buffer.update(pattern, newTripTimes, serviceDate);
        success = true;
      }
    }

    return success;
  }

  /**
   * Cancel previously added trip from buffer if there is a previously added trip with given trip id
   * (without agency id) on service date. This does not remove the modified/added trip from the
   * buffer, it just marks it as canceled. This also does not remove the corresponding vertices and
   * edges from the Graph. Any TripPattern that was created for the added/modified trip continues to
   * exist, and will be reused if a similar added/modified trip message is received with the same
   * route and stop sequence.
   *
   * @param tripId      trip id without agency id
   * @param serviceDate service date
   * @return true if a previously added trip was cancelled
   */
  private boolean cancelPreviouslyAddedTrip(
    final FeedScopedId tripId,
    final LocalDate serviceDate
  ) {
    boolean success = false;

    final TripPattern pattern = buffer.getRealtimeAddedTripPattern(tripId, serviceDate);
    if (pattern != null) {
      // Cancel trip times for this trip in this pattern
      final Timetable timetable = buffer.resolve(pattern, serviceDate);
      final int tripIndex = timetable.getTripIndex(tripId);
      if (tripIndex == -1) {
        debug(tripId, "Could not cancel previously added trip on {}", serviceDate);
      } else {
        final TripTimes newTripTimes = new TripTimes(timetable.getTripTimes(tripIndex));
        newTripTimes.cancelTrip();
        buffer.update(pattern, newTripTimes, serviceDate);
        success = true;
      }
    }

    return success;
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing a MODIFIED trip.
   *
   * @param tripUpdate     GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @return empty Result if successful or one containing an error
   */
  private Result<?, UpdateError> validateAndHandleModifiedTrip(
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
    Trip trip = getTripForTripId(tripId);

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
      final Set<FeedScopedId> serviceIds = transitService
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
    var stops = checkNewStopTimeUpdatesAndFindStops(tripId, tripUpdate);
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
  private Result<?, UpdateError> handleModifiedTrip(
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

    // Cancel scheduled trip
    var tripId = trip.getId();
    cancelScheduledTrip(tripId, serviceDate);

    // Check whether trip id has been used for previously ADDED/REPLACEMENT trip message and cancel
    // previously created trip
    cancelPreviouslyAddedTrip(tripId, serviceDate);

    // Add new trip
    return addTripToGraphAndBuffer(trip, tripUpdate, stops, serviceDate, RealTimeState.MODIFIED);
  }

  private Result<?, UpdateError> handleCanceledTrip(
    FeedScopedId tripId,
    final LocalDate serviceDate
  ) {
    // Try to cancel scheduled trip
    final boolean cancelScheduledSuccess = cancelScheduledTrip(tripId, serviceDate);

    // Try to cancel previously added trip
    final boolean cancelPreviouslyAddedSuccess = cancelPreviouslyAddedTrip(tripId, serviceDate);

    if (!cancelScheduledSuccess && !cancelPreviouslyAddedSuccess) {
      debug(tripId, "No pattern found for tripId. Skipping cancellation.");
      return UpdateError.result(tripId, NO_TRIP_FOR_CANCELLATION_FOUND);
    }
    return Result.success();
  }

  private boolean purgeExpiredData() {
    final LocalDate today = localDateNow.get();
    // TODO: Base this on numberOfDaysOfLongestTrip for tripPatterns
    final LocalDate previously = today.minusDays(2); // Just to be safe...

    // Purge data only if we have changed date
    if (lastPurgeDate != null && lastPurgeDate.compareTo(previously) >= 0) {
      return false;
    }

    LOG.debug("purging expired realtime data");

    lastPurgeDate = previously;

    return buffer.purgeExpiredData(previously);
  }

  /**
   * Retrieve a trip pattern given a trip id.
   *
   * @param tripId trip id
   * @return trip pattern or null if no trip pattern was found
   */
  private TripPattern getPatternForTripId(FeedScopedId tripId) {
    Trip trip = transitService.getTripForId(tripId);
    return transitService.getPatternForTrip(trip);
  }

  /**
   * Retrieve route given a route id
   *
   * @return route or null if route can't be found in graph index
   */
  private Route getRouteForRouteId(FeedScopedId routeId) {
    return transitService.getRouteForId(routeId);
  }

  /**
   * Retrieve trip given a trip id
   *
   * @return trip or null if trip can't be found in graph index
   */
  private Trip getTripForTripId(FeedScopedId tripId) {
    return transitService.getTripForId(tripId);
  }

  /**
   * Retrieve stop given a stop id.
   *
   * @return stop or null if stop doesn't exist
   */
  private StopLocation getStopForStopId(FeedScopedId stopId) {
    return transitService.getRegularStop(stopId);
  }

  private static void debug(FeedScopedId id, String message, Object... params) {
    debug(id.getFeedId(), id.getId(), message, params);
  }

  private static void debug(String feedId, String tripId, String message, Object... params) {
    String m = "[feedId: %s, tripId: %s] %s".formatted(feedId, tripId, message);
    LOG.debug(m, params);
  }
}
