package org.opentripplanner.updater.stoptime;

import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.PickDrop.SCHEDULED;

import com.google.common.base.Preconditions;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimesPatch;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
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
   * Number of milliseconds per second
   */
  private static final int MILLIS_PER_SECOND = 1000;

  /**
   * Maximum time in seconds since midnight for arrivals and departures
   */
  private static final long MAX_ARRIVAL_DEPARTURE_TIME = 48 * 60 * 60;

  public int logFrequency = 2000;

  private int appliedBlockCount = 0;

  /**
   * If a timetable snapshot is requested less than this number of milliseconds after the previous
   * snapshot, just return the same one. Throttles the potentially resource-consuming task of
   * duplicating a TripPattern → Timetable map and indexing the new Timetables.
   */
  public int maxSnapshotFrequency = 1000; // msec

  /**
   * The last committed snapshot that was handed off to a routing thread. This snapshot may be
   * given to more than one routing thread if the maximum snapshot frequency is exceeded.
   */
  private volatile TimetableSnapshot snapshot = null;

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
   * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime messages.
   */
  private final TripPatternCache tripPatternCache = new TripPatternCache();

  /** Should expired realtime data be purged from the graph. */
  public boolean purgeExpiredData = true;

  protected ServiceDate lastPurgeDate = null;

  /** Epoch time in milliseconds at which the last snapshot was generated. */
  protected long lastSnapshotTime = -1;

  private final TimeZone timeZone;

  private final RoutingService routingService;

  public GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

  private final TransitLayer realtimeTransitLayer;

  private final TransitLayerUpdater transitLayerUpdater;

  public TimetableSnapshotSource(final Graph graph) {
    timeZone = graph.getTimeZone();
    routingService = new RoutingService(graph);
    realtimeTransitLayer = graph.getRealtimeTransitLayer();
    transitLayerUpdater = graph.transitLayerUpdater;
  }

  /**
   * @return an up-to-date snapshot mapping TripPatterns to Timetables. This snapshot and the
   *         timetable objects it references are guaranteed to never change, so the requesting
   *         thread is provided a consistent view of all TripTimes. The routing thread need only
   *         release its reference to the snapshot to release resources.
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

  private TimetableSnapshot getTimetableSnapshot(final boolean force) {
    final long now = System.currentTimeMillis();
    if (force || now - lastSnapshotTime > maxSnapshotFrequency) {
      if (force || buffer.isDirty()) {
        LOG.debug("Committing {}", buffer.toString());
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
   * Method to apply a trip update list to the most recent version of the timetable snapshot. A
   * GTFS-RT feed is always applied against a single static feed (indicated by feedId).
   *
   * However, multi-feed support is not completed and we currently assume there is only one static
   * feed when matching IDs.
   *
   * @param calendarService graph's calendar service
   * @param deduplicator deduplicator for different types
   * @param graphIndex graph's index
   * @param serviceCodes graph's service codes
   * @param fullDataset true iff the list with updates represent all updates that are active right
   *        now, i.e. all previous updates should be disregarded
   * @param updates GTFS-RT TripUpdate's that should be applied atomically
   * @param feedId
   */
  public void applyTripUpdates(
    final CalendarService calendarService,
    final Deduplicator deduplicator,
    final GraphIndex graphIndex,
    final Map<FeedScopedId, Integer> serviceCodes,
    final boolean fullDataset,
    final List<TripUpdate> updates,
    final String feedId
  ) {
    if (updates == null) {
      LOG.warn("updates is null");
      return;
    }

    // Acquire lock on buffer
    bufferLock.lock();

    try {
      if (fullDataset) {
        // Remove all updates from the buffer
        buffer.clear(feedId);
      }

      LOG.debug("message contains {} trip updates", updates.size());
      int uIndex = 0;
      for (TripUpdate tripUpdate : updates) {
        if (fuzzyTripMatcher != null && tripUpdate.hasTrip()) {
          final TripDescriptor trip = fuzzyTripMatcher.match(feedId, tripUpdate.getTrip());
          tripUpdate = tripUpdate.toBuilder().setTrip(trip).build();
        }

        if (!tripUpdate.hasTrip()) {
          LOG.warn("Missing TripDescriptor in gtfs-rt trip update: \n{}", tripUpdate);
          continue;
        }

        ServiceDate serviceDate = new ServiceDate();
        final TripDescriptor tripDescriptor = tripUpdate.getTrip();

        if (tripDescriptor.hasStartDate()) {
          try {
            serviceDate = ServiceDate.parseString(tripDescriptor.getStartDate());
          } catch (final ParseException e) {
            LOG.warn("Failed to parse start date in gtfs-rt trip update: \n{}", tripUpdate);
            continue;
          }
        } else {
          // TODO: figure out the correct service date. For the special case that a trip
          // starts for example at 40:00, yesterday would probably be a better guess.
        }

        if (!tripDescriptor.hasTripId()) {
          LOG.warn("No trip id found for gtfs-rt trip update: \n{}", tripUpdate);
          continue;
        }

        uIndex += 1;
        LOG.debug("trip update #{} ({} updates) :", uIndex, tripUpdate.getStopTimeUpdateCount());
        LOG.trace("{}", tripUpdate);

        // Determine what kind of trip update this is
        boolean applied = false;
        final TripDescriptor.ScheduleRelationship tripScheduleRelationship = determineTripScheduleRelationship(
          tripDescriptor
        );
        switch (tripScheduleRelationship) {
          case SCHEDULED:
            applied =
              handleScheduledTrip(serviceCodes, tripUpdate, tripDescriptor, feedId, serviceDate);
            break;
          case ADDED:
            applied =
              validateAndHandleAddedTrip(
                calendarService,
                deduplicator,
                graphIndex,
                serviceCodes,
                tripUpdate,
                tripDescriptor,
                feedId,
                serviceDate
              );
            break;
          case UNSCHEDULED:
            applied = handleUnscheduledTrip();
            break;
          case CANCELED:
            applied = handleCanceledTrip(tripDescriptor, feedId, serviceDate);
            break;
          case MODIFIED:
            applied =
              validateAndHandleModifiedTrip(
                calendarService,
                deduplicator,
                graphIndex,
                serviceCodes,
                tripUpdate,
                tripDescriptor,
                feedId,
                serviceDate
              );
            break;
        }

        if (applied) {
          appliedBlockCount++;
        } else {
          LOG.warn("Failed to apply TripUpdate.");
          LOG.trace(" Contents: {}", tripUpdate);
        }

        if (appliedBlockCount % logFrequency == 0) {
          LOG.info("Applied {} trip updates.", appliedBlockCount);
        }
      }
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

  private boolean handleScheduledTrip(
    final Map<FeedScopedId, Integer> serviceCodes,
    final TripUpdate tripUpdate,
    final TripDescriptor tripDescriptor,
    final String feedId,
    final ServiceDate serviceDate
  ) {
    // This does not include Agency ID or feed ID, trips are feed-unique and we currently assume a single static feed.
    final String tripId = tripDescriptor.getTripId();
    final TripPattern pattern = getPatternForTripId(feedId, tripId);

    if (pattern == null) {
      LOG.warn("No pattern found for tripId {}, skipping TripUpdate.", tripId);
      return false;
    }

    if (tripUpdate.getStopTimeUpdateCount() < 1) {
      LOG.warn("TripUpdate contains no updates, skipping.");
      return false;
    }

    // If this trip_id has been used for previously ADDED/MODIFIED trip message (e.g. when the sequence of stops has
    // changed, and is now changing back to the originally scheduled one) cancel that previously created trip.
    cancelPreviouslyAddedTrip(new FeedScopedId(feedId, tripId), serviceDate);

    // Get new TripTimes based on scheduled timetable
    final TripTimesPatch tripTimesPatch = pattern
      .getScheduledTimetable()
      .createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);

    if (tripTimesPatch == null) {
      return false;
    }

    TripTimes updatedTripTimes = tripTimesPatch.getTripTimes();

    List<Integer> skippedStopIndices = tripTimesPatch.getSkippedStopIndices();

    // Make sure that updated trip times have the correct real time state
    updatedTripTimes.setRealTimeState(RealTimeState.UPDATED);

    // If there are skipped stops, we need to change the pattern from the scheduled one
    if (skippedStopIndices.size() > 0) {
      StopPattern newStopPattern = pattern
        .getStopPattern()
        .mutate()
        .cancelStops(skippedStopIndices)
        .build();

      final Trip trip = getTripForTripId(feedId, tripId);
      // Get cached trip pattern or create one if it doesn't exist yet
      final TripPattern newPattern = tripPatternCache.getOrCreateTripPattern(
        newStopPattern,
        trip,
        serviceCodes,
        pattern
      );

      // Add service code to bitset of pattern if needed (using copy on write)
      final int serviceCode = serviceCodes.get(trip.getServiceId());
      newPattern.setServiceCode(serviceCode);

      cancelScheduledTrip(feedId, tripId, serviceDate);
      return buffer.update(newPattern, updatedTripTimes, serviceDate);
    } else {
      // Set the updated trip times in the buffer
      return buffer.update(pattern, updatedTripTimes, serviceDate);
    }
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing an ADDED trip.
   *
   * @param calendarService graph's calendar service
   * @param deduplicator deduplicator for different types
   * @param graphIndex graph's index
   * @param serviceCodes graph's service codes
   * @param tripUpdate GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @param feedId
   * @param serviceDate
   * @return true iff successful
   */
  private boolean validateAndHandleAddedTrip(
    final CalendarService calendarService,
    final Deduplicator deduplicator,
    final GraphIndex graphIndex,
    final Map<FeedScopedId, Integer> serviceCodes,
    final TripUpdate tripUpdate,
    final TripDescriptor tripDescriptor,
    final String feedId,
    final ServiceDate serviceDate
  ) {
    // Preconditions
    Preconditions.checkNotNull(calendarService);
    Preconditions.checkNotNull(deduplicator);
    Preconditions.checkNotNull(graphIndex);
    Preconditions.checkNotNull(tripUpdate);
    Preconditions.checkNotNull(serviceDate);

    //
    // Validate added trip
    //

    // Check whether trip id already exists in graph
    final String tripId = tripDescriptor.getTripId();
    final Trip trip = getTripForTripId(feedId, tripId);
    if (trip != null) {
      // TODO: should we support this and add a new instantiation of this trip (making it
      // frequency based)?
      LOG.warn("Graph already contains trip id of ADDED trip, skipping.");
      return false;
    }

    // Check whether a start date exists
    if (!tripDescriptor.hasStartDate()) {
      // TODO: should we support this and apply update to all days?
      LOG.warn("ADDED trip doesn't have a start date in TripDescriptor, skipping.");
      return false;
    }

    // Check whether at least two stop updates exist
    if (tripUpdate.getStopTimeUpdateCount() < 2) {
      LOG.warn("ADDED trip has less then two stops, skipping.");
      return false;
    }

    // Check whether all stop times are available and all stops exist
    final var stops = checkNewStopTimeUpdatesAndFindStops(feedId, tripUpdate);
    if (stops == null) {
      return false;
    }

    //
    // Handle added trip
    //
    return handleAddedTrip(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
      tripUpdate,
      tripDescriptor,
      stops,
      feedId,
      serviceDate
    );
  }

  /**
   * Check stop time updates of trip update that results in a new trip (ADDED or MODIFIED) and
   * find all stops of that trip.
   *
   * @param feedId feed id this trip update is intented for
   * @param tripUpdate trip update
   * @return stops when stop time updates are correct; null if there are errors
   */
  private List<StopLocation> checkNewStopTimeUpdatesAndFindStops(
    final String feedId,
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
          LOG.warn("Trip update contains negative stop sequence, skipping.");
          return null;
        }

        // Check whether sequence is increasing
        if (previousStopSequence != null && previousStopSequence > stopSequence) {
          LOG.warn("Trip update contains decreasing stop sequence, skipping.");
          return null;
        }
        previousStopSequence = stopSequence;
      } else {
        // Allow missing stop sequences for ADDED and MODIFIED trips
      }

      // Find stops
      if (stopTimeUpdate.hasStopId()) {
        // Find stop
        final var stop = getStopForStopId(feedId, stopTimeUpdate.getStopId());
        if (stop != null) {
          // Remember stop
          stops.add(stop);
        } else {
          LOG.warn(
            "Graph doesn't contain stop id \"{}\" of trip update, skipping.",
            stopTimeUpdate.getStopId()
          );
          return null;
        }
      } else {
        LOG.warn("Trip update misses some stop ids, skipping.");
        return null;
      }

      // Check arrival time
      if (stopTimeUpdate.hasArrival() && stopTimeUpdate.getArrival().hasTime()) {
        // Check for increasing time
        final Long time = stopTimeUpdate.getArrival().getTime();
        if (previousTime != null && previousTime > time) {
          LOG.warn("Trip update contains decreasing times, skipping.");
          return null;
        }
        previousTime = time;
      } else {
        LOG.warn("Trip update misses arrival time, skipping.");
        return null;
      }

      // Check departure time
      if (stopTimeUpdate.hasDeparture() && stopTimeUpdate.getDeparture().hasTime()) {
        // Check for increasing time
        final Long time = stopTimeUpdate.getDeparture().getTime();
        if (previousTime != null && previousTime > time) {
          LOG.warn("Trip update contains decreasing times, skipping.");
          return null;
        }
        previousTime = time;
      } else {
        LOG.warn("Trip update misses departure time, skipping.");
        return null;
      }
    }
    return stops;
  }

  /**
   * Handle GTFS-RT TripUpdate message containing an ADDED trip.
   *
   * @param calendarService graph's calendar service
   * @param deduplicator deduplicator for different types
   * @param graphIndex graph's index
   * @param serviceCodes graph's service codes
   * @param tripUpdate GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @param stops the stops of each StopTimeUpdate in the TripUpdate message
   * @param feedId
   * @param serviceDate service date for added trip
   * @return true iff successful
   */
  private boolean handleAddedTrip(
    final CalendarService calendarService,
    final Deduplicator deduplicator,
    final GraphIndex graphIndex,
    final Map<FeedScopedId, Integer> serviceCodes,
    final TripUpdate tripUpdate,
    final TripDescriptor tripDescriptor,
    final List<StopLocation> stops,
    final String feedId,
    final ServiceDate serviceDate
  ) {
    // Preconditions
    Preconditions.checkNotNull(stops);
    Preconditions.checkArgument(
      tripUpdate.getStopTimeUpdateCount() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Check whether trip id has been used for previously ADDED trip message and cancel
    // previously created trip
    final String tripId = tripDescriptor.getTripId();
    cancelPreviouslyAddedTrip(new FeedScopedId(feedId, tripId), serviceDate);

    //
    // Create added trip
    //

    Route route = null;
    if (tripDescriptor.hasRouteId()) {
      // Try to find route
      route = getRouteForRouteId(feedId, tripDescriptor.getRouteId());
    }

    if (route == null) {
      // Create new Route
      // Use route id of trip descriptor if available
      FeedScopedId id = tripDescriptor.hasRouteId()
        ? new FeedScopedId(feedId, tripDescriptor.getRouteId())
        : new FeedScopedId(feedId, tripId);

      route = new Route(id);
      // Create dummy agency for added trips
      Agency dummyAgency = new Agency(new FeedScopedId(feedId, "Dummy"), "Dummy", "Europe/Paris");
      route.setAgency(dummyAgency);
      // Guess the route type as it doesn't exist yet in the specifications
      // Bus. Used for short- and long-distance bus routes.
      route.setGtfsType(3);
      route.setMode(TransitMode.BUS);
      // Create route name
      route.setLongName(tripId);
    }

    // Create new Trip

    // TODO: which Agency ID to use? Currently use feed id.
    final Trip trip = new Trip(new FeedScopedId(feedId, tripId));
    trip.setRoute(route);

    // Find service ID running on this service date
    final Set<FeedScopedId> serviceIds = calendarService.getServiceIdsOnDate(serviceDate);
    if (serviceIds.isEmpty()) {
      // No service id exists: return error for now
      LOG.warn("ADDED trip has service date for which no service id is available, skipping.");
      return false;
    } else {
      // Just use first service id of set
      trip.setServiceId(serviceIds.iterator().next());
    }

    return addTripToGraphAndBuffer(
      deduplicator,
      graphIndex,
      serviceCodes,
      trip,
      tripUpdate,
      stops,
      serviceDate,
      RealTimeState.ADDED
    );
  }

  /**
   * Add a (new) trip to the graph and the buffer
   *
   * @param deduplicator deduplicator for different types
   * @param graphIndex graph's index
   * @param serviceCodes graph's service codes
   * @param trip trip
   * @param tripUpdate trip update containing stop time updates
   * @param stops list of stops corresponding to stop time updates
   * @param serviceDate service date of trip
   * @param realTimeState real-time state of new trip
   * @return true iff successful
   */
  private boolean addTripToGraphAndBuffer(
    final Deduplicator deduplicator,
    final GraphIndex graphIndex,
    final Map<FeedScopedId, Integer> serviceCodes,
    final Trip trip,
    final TripUpdate tripUpdate,
    final List<StopLocation> stops,
    final ServiceDate serviceDate,
    final RealTimeState realTimeState
  ) {
    // Preconditions
    Preconditions.checkNotNull(stops);
    Preconditions.checkArgument(
      tripUpdate.getStopTimeUpdateCount() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Calculate seconds since epoch on GTFS midnight (noon minus 12h) of service date
    final Calendar serviceCalendar = serviceDate.getAsCalendar(timeZone);
    final long midnightSecondsSinceEpoch = serviceCalendar.getTimeInMillis() / MILLIS_PER_SECOND;

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
          LOG.warn(
            "ADDED trip has invalid arrival time (compared to start date in " +
            "TripDescriptor), skipping."
          );
          return false;
        }
        stopTime.setArrivalTime((int) arrivalTime);
      }
      // Set departure time
      if (stopTimeUpdate.hasDeparture() && stopTimeUpdate.getDeparture().hasTime()) {
        final long departureTime =
          stopTimeUpdate.getDeparture().getTime() - midnightSecondsSinceEpoch;
        if (departureTime < 0 || departureTime > MAX_ARRIVAL_DEPARTURE_TIME) {
          LOG.warn(
            "ADDED trip has invalid departure time (compared to start date in " +
            "TripDescriptor), skipping."
          );
          return false;
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

    final TripPattern originalTripPattern = graphIndex.getPatternForTrip().get(trip);
    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      stopPattern,
      trip,
      serviceCodes,
      originalTripPattern
    );

    // Add service code to bitset of pattern if needed (using copy on write)
    final int serviceCode = serviceCodes.get(trip.getServiceId());
    pattern.setServiceCode(serviceCode);

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
    newTripTimes.setServiceCode(serviceCode);

    // Make sure that updated trip times have the correct real time state
    newTripTimes.setRealTimeState(realTimeState);

    // Add new trip times to the buffer
    return buffer.update(pattern, newTripTimes, serviceDate);
  }

  /**
   * Cancel scheduled trip in buffer given trip id (without agency id) on service date
   *
   * @param tripId trip id without agency id
   * @param serviceDate service date
   * @return true if scheduled trip was cancelled
   */
  private boolean cancelScheduledTrip(String feedId, String tripId, final ServiceDate serviceDate) {
    boolean success = false;

    final TripPattern pattern = getPatternForTripId(feedId, tripId);

    if (pattern != null) {
      // Cancel scheduled trip times for this trip in this pattern
      final Timetable timetable = pattern.getScheduledTimetable();
      final int tripIndex = timetable.getTripIndex(tripId);
      if (tripIndex == -1) {
        LOG.warn("Could not cancel scheduled trip {}", tripId);
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
   * Cancel previously added trip from buffer if there is a previously added trip with given trip
   * id (without agency id) on service date. This does not remove the modified/added trip from the buffer, it just
   * marks it as canceled. This also does not remove the corresponding vertices and edges from the Graph. Any
   * TripPattern that was created for the added/modified trip continues to exist, and will be reused if a similar
   * added/modified trip message is received with the same route and stop sequence.
   *
   * @param tripId trip id without agency id
   * @param serviceDate service date
   * @return true if a previously added trip was cancelled
   */
  private boolean cancelPreviouslyAddedTrip(FeedScopedId tripId, final ServiceDate serviceDate) {
    boolean success = false;

    final TripPattern pattern = buffer.getLastAddedTripPattern(tripId, serviceDate);
    if (pattern != null) {
      // Cancel trip times for this trip in this pattern
      final Timetable timetable = buffer.resolve(pattern, serviceDate);
      final int tripIndex = timetable.getTripIndex(tripId);
      if (tripIndex == -1) {
        LOG.warn("Could not cancel previously added trip {}", tripId);
      } else {
        final TripTimes newTripTimes = new TripTimes(timetable.getTripTimes(tripIndex));
        newTripTimes.cancelTrip();
        buffer.update(pattern, newTripTimes, serviceDate);
        success = true;
      }
    }

    return success;
  }

  private boolean handleUnscheduledTrip() {
    // TODO: Handle unscheduled trip
    LOG.warn("Unscheduled trips are currently unsupported. Skipping TripUpdate.");
    return false;
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing a MODIFIED trip.
   *
   * @param calendarService graph's calendar service
   * @param deduplicator deduplicator for different types
   * @param graphIndex graph's index
   * @param serviceCodes graph's service codes
   * @param tripUpdate GTFS-RT TripUpdate message
   * @param tripDescriptor GTFS-RT TripDescriptor
   * @param feedId
   * @param serviceDate
   * @return true if successful
   */
  private boolean validateAndHandleModifiedTrip(
    final CalendarService calendarService,
    final Deduplicator deduplicator,
    final GraphIndex graphIndex,
    final Map<FeedScopedId, Integer> serviceCodes,
    final TripUpdate tripUpdate,
    final TripDescriptor tripDescriptor,
    final String feedId,
    final ServiceDate serviceDate
  ) {
    // Preconditions
    Preconditions.checkNotNull(calendarService);
    Preconditions.checkNotNull(deduplicator);
    Preconditions.checkNotNull(graphIndex);
    Preconditions.checkNotNull(tripUpdate);
    Preconditions.checkNotNull(serviceDate);

    //
    // Validate modified trip
    //

    // Check whether trip id already exists in graph
    String tripId = tripDescriptor.getTripId();
    Trip trip = getTripForTripId(feedId, tripId);
    if (trip == null) {
      // TODO: should we support this and consider it an ADDED trip?
      LOG.warn("Graph does not contain trip id of MODIFIED trip, skipping.");
      return false;
    }

    // Check whether a start date exists
    if (!tripDescriptor.hasStartDate()) {
      // TODO: should we support this and apply update to all days?
      LOG.warn("MODIFIED trip doesn't have a start date in TripDescriptor, skipping.");
      return false;
    } else {
      // Check whether service date is served by trip
      final Set<FeedScopedId> serviceIds = calendarService.getServiceIdsOnDate(serviceDate);
      if (!serviceIds.contains(trip.getServiceId())) {
        // TODO: should we support this and change service id of trip?
        LOG.warn("MODIFIED trip has a service date that is not served by trip, skipping.");
        return false;
      }
    }

    // Check whether at least two stop updates exist
    if (tripUpdate.getStopTimeUpdateCount() < 2) {
      LOG.warn("MODIFIED trip has less then two stops, skipping.");
      return false;
    }

    // Check whether all stop times are available and all stops exist
    var stops = checkNewStopTimeUpdatesAndFindStops(feedId, tripUpdate);
    if (stops == null) {
      return false;
    }

    //
    // Handle modified trip
    //

    return handleModifiedTrip(
      deduplicator,
      graphIndex,
      serviceCodes,
      trip,
      tripUpdate,
      stops,
      feedId,
      serviceDate
    );
  }

  /**
   * Handle GTFS-RT TripUpdate message containing a MODIFIED trip.
   *
   * @param deduplicator deduplicator for different types
   * @param graphIndex graph's index
   * @param serviceCodes graph's service codes
   * @param trip trip that is modified
   * @param tripUpdate GTFS-RT TripUpdate message
   * @param stops the stops of each StopTimeUpdate in the TripUpdate message
   * @param feedId
   * @param serviceDate service date for modified trip
   * @return true iff successful
   */
  private boolean handleModifiedTrip(
    final Deduplicator deduplicator,
    final GraphIndex graphIndex,
    final Map<FeedScopedId, Integer> serviceCodes,
    final Trip trip,
    final TripUpdate tripUpdate,
    final List<StopLocation> stops,
    final String feedId,
    final ServiceDate serviceDate
  ) {
    // Preconditions
    Preconditions.checkNotNull(stops);
    Preconditions.checkArgument(
      tripUpdate.getStopTimeUpdateCount() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Cancel scheduled trip

    final String tripId = tripUpdate.getTrip().getTripId();
    cancelScheduledTrip(feedId, tripId, serviceDate);

    // Check whether trip id has been used for previously ADDED/MODIFIED trip message and cancel
    // previously created trip
    cancelPreviouslyAddedTrip(new FeedScopedId(feedId, tripId), serviceDate);

    // Add new trip
    return addTripToGraphAndBuffer(
      deduplicator,
      graphIndex,
      serviceCodes,
      trip,
      tripUpdate,
      stops,
      serviceDate,
      RealTimeState.MODIFIED
    );
  }

  private boolean handleCanceledTrip(
    final TripDescriptor tripDescriptor,
    final String feedId,
    final ServiceDate serviceDate
  ) {
    // Try to cancel scheduled trip
    final String tripId = tripDescriptor.getTripId();
    final boolean cancelScheduledSuccess = cancelScheduledTrip(feedId, tripId, serviceDate);

    // Try to cancel previously added trip
    final boolean cancelPreviouslyAddedSuccess = cancelPreviouslyAddedTrip(
      new FeedScopedId(feedId, tripId),
      serviceDate
    );

    if (!cancelScheduledSuccess && !cancelPreviouslyAddedSuccess) {
      LOG.warn("No pattern found for tripId {}, skipping TripUpdate.", tripId);
      return false;
    }
    return true;
  }

  private boolean purgeExpiredData() {
    final ServiceDate today = new ServiceDate();
    // TODO: Base this on numberOfDaysOfLongestTrip for tripPatterns
    final ServiceDate previously = today.previous().previous(); // Just to be safe...

    // Purge data only if we have changed date
    if (lastPurgeDate != null && lastPurgeDate.compareTo(previously) >= 0) {
      return false;
    }

    LOG.debug("purging expired realtime data");

    lastPurgeDate = previously;

    return buffer.purgeExpiredData(previously);
  }

  /**
   * Retrieve a trip pattern given a feed id and trip id.
   *
   * @param feedId feed id for the trip id
   * @param tripId trip id without agency
   * @return trip pattern or null if no trip pattern was found
   */
  private TripPattern getPatternForTripId(String feedId, String tripId) {
    Trip trip = routingService.getTripForId().get(new FeedScopedId(feedId, tripId));
    return routingService.getPatternForTrip().get(trip);
  }

  /**
   * Retrieve route given a route id without an agency
   *
   * @param feedId feed id for the route id
   * @param routeId route id without the agency
   * @return route or null if route can't be found in graph index
   */
  private Route getRouteForRouteId(String feedId, String routeId) {
    return routingService.getRouteForId(new FeedScopedId(feedId, routeId));
  }

  /**
   * Retrieve trip given a trip id without an agency
   *
   * @param feedId feed id for the trip id
   * @param tripId trip id without the agency
   * @return trip or null if trip can't be found in graph index
   */
  private Trip getTripForTripId(String feedId, String tripId) {
    return routingService.getTripForId().get(new FeedScopedId(feedId, tripId));
  }

  /**
   * Retrieve stop given a feed id and stop id.
   *
   * @param feedId feed id for the stop id
   * @param stopId trip id without the agency
   * @return stop or null if stop doesn't exist
   */
  private StopLocation getStopForStopId(String feedId, String stopId) {
    return routingService.getStopForId(new FeedScopedId(feedId, stopId));
  }
}
