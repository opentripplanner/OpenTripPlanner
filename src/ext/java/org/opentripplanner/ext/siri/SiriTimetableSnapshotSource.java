package org.opentripplanner.ext.siri;

import static org.opentripplanner.ext.siri.TimetableHelper.createModifiedStopTimes;
import static org.opentripplanner.ext.siri.TimetableHelper.createModifiedStops;
import static org.opentripplanner.ext.siri.TimetableHelper.createUpdatedTripTimes;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.UNKNOWN;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.trip.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.DatedVehicleJourneyRef;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.EstimatedVersionFrameStructure;
import uk.org.siri.siri20.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.VehicleActivityCancellationStructure;
import uk.org.siri.siri20.VehicleActivityStructure;
import uk.org.siri.siri20.VehicleMonitoringDeliveryStructure;

/**
 * This class should be used to create snapshots of lookup tables of realtime data. This is
 * necessary to provide planning threads a consistent constant view of a graph with realtime data at
 * a specific point in time.
 */
public class SiriTimetableSnapshotSource implements TimetableSnapshotProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SiriTimetableSnapshotSource.class);

  private static boolean keepLogging = true;
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
  private final SiriTripPatternCache tripPatternCache = new SiriTripPatternCache(
    tripPatternIdGenerator
  );
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
   * Method to apply a trip update list to the most recent version of the timetable snapshot.
   *
   * @param transitModel transitModel to update (needed for adding/changing stop patterns)
   * @param fullDataset  true if the list with updates represent all updates that are active right
   *                     now, i.e. all previous updates should be disregarded
   * @param updates      SIRI VehicleMonitoringDeliveries that should be applied atomically
   */
  public void applyVehicleMonitoring(
    TransitModel transitModel,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    String feedId,
    boolean fullDataset,
    List<VehicleMonitoringDeliveryStructure> updates
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

      for (VehicleMonitoringDeliveryStructure vmDelivery : updates) {
        LocalDate serviceDate = LocalDate.now(transitService.getTimeZone());

        List<VehicleActivityStructure> activities = vmDelivery.getVehicleActivities();
        if (activities != null) {
          //Handle activities
          LOG.info("Handling {} VM-activities.", activities.size());
          int handledCounter = 0;
          int skippedCounter = 0;
          for (VehicleActivityStructure activity : activities) {
            boolean handled = handleModifiedTrip(
              transitModel,
              fuzzyTripMatcher,
              activity,
              serviceDate,
              entityResolver
            );
            if (handled) {
              handledCounter++;
            } else {
              skippedCounter++;
            }
          }
          LOG.info("Applied {} VM-activities, skipped {}.", handledCounter, skippedCounter);
        }
        List<VehicleActivityCancellationStructure> cancellations = vmDelivery.getVehicleActivityCancellations();
        if (cancellations != null && !cancellations.isEmpty()) {
          //Handle cancellations
          LOG.info("TODO: Handle {} cancellations.", cancellations.size());
        }

        List<NaturalLanguageStringStructure> notes = vmDelivery.getVehicleActivityNotes();
        if (notes != null && !notes.isEmpty()) {
          //Handle notes
          LOG.info("TODO: Handle {} notes.", notes.size());
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
      if (keepLogging) {
        LOG.info("Reducing SIRI-VM logging until restart");
        keepLogging = false;
      }
    }
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

    List<Result<?, UpdateError>> results = new ArrayList<>();

    try {
      if (fullDataset) {
        // Remove all updates from the buffer
        buffer.clear(feedId);
      }

      for (EstimatedTimetableDeliveryStructure etDelivery : updates) {
        var res = apply(etDelivery, transitModel, feedId, fuzzyTripMatcher, entityResolver);
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

  private List<Result<?, UpdateError>> apply(
    EstimatedTimetableDeliveryStructure etDelivery,
    TransitModel transitModel,
    String feedId,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver
  ) {
    List<Result<?, UpdateError>> results = new ArrayList<>();
    List<EstimatedVersionFrameStructure> estimatedJourneyVersions = etDelivery.getEstimatedJourneyVersionFrames();
    if (estimatedJourneyVersions != null) {
      //Handle deliveries
      for (EstimatedVersionFrameStructure estimatedJourneyVersion : estimatedJourneyVersions) {
        List<EstimatedVehicleJourney> journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
        LOG.debug("Handling {} EstimatedVehicleJourneys.", journeys.size());
        for (EstimatedVehicleJourney journey : journeys) {
          if (isReplacementDeparture(journey, feedId)) {
            // Added trip
            try {
              Result<?, UpdateError> res = handleAddedTrip(transitModel, feedId, journey);
              results.add(res);
            } catch (Throwable t) {
              // Since this is work in progress - catch everything to continue processing updates
              LOG.warn(
                "Adding ExtraJourney with id='{}' failed, caused by '{}'.",
                journey.getEstimatedVehicleJourneyCode(),
                t.getMessage()
              );
              results.add(Result.failure(UpdateError.noTripId(UNKNOWN)));
            }
          } else {
            // Updated trip
            var result = handleModifiedTrip(
              transitModel,
              fuzzyTripMatcher,
              entityResolver,
              feedId,
              journey
            );
            result.ifSuccess(ignored -> results.add(Result.success()));
            result.ifFailure(failures -> {
              failures.stream().map(Result::failure).forEach(results::add);

              if (journey.isMonitored() != null && !journey.isMonitored()) {
                results.add(
                  Result.success(UpdateError.noTripId(UpdateError.UpdateErrorType.NOT_MONITORED))
                );
              }
            });
          }
        }
      }
    }
    return results;
  }

  /**
   * Check if VehicleJourney is an replacement departure according to SIRI-ET requirements.
   */
  private boolean isReplacementDeparture(EstimatedVehicleJourney vehicleJourney, String feedId) {
    // Replacement departure only if ExtraJourney is true
    if (!Optional.ofNullable(vehicleJourney.isExtraJourney()).orElse(false)) {
      return false;
    }

    // If Trip exists by DatedServiceJourney then this is not replacement departure
    var datedServiceJourneyId = resolveDatedServiceJourneyId(vehicleJourney);
    return datedServiceJourneyId
      .map(dsjId ->
        getTimetableSnapshot()
          .getRealtimeAddedTripOnServiceDate()
          .containsKey(new FeedScopedId(feedId, dsjId))
      )
      .orElse(false);
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

  private boolean handleModifiedTrip(
    TransitModel transitModel,
    SiriFuzzyTripMatcher fuzzyTripMatcher,
    VehicleActivityStructure activity,
    LocalDate serviceDate,
    EntityResolver entityResolver
  ) {
    if (activity.getValidUntilTime().isBefore(ZonedDateTime.now())) {
      //Activity has expired
      return false;
    }

    MonitoredVehicleJourneyStructure monitoredVehicleJourney = activity.getMonitoredVehicleJourney();
    if (
      monitoredVehicleJourney == null ||
      monitoredVehicleJourney.getVehicleRef() == null ||
      monitoredVehicleJourney.getLineRef() == null
    ) {
      //No vehicle reference or line reference
      return false;
    }

    Boolean isMonitored = monitoredVehicleJourney.isMonitored();
    if (isMonitored != null && !isMonitored) {
      //Vehicle is reported as NOT monitored
      return false;
    }

    Set<Trip> trips;
    Trip serviceJourney = entityResolver.resolveTrip(monitoredVehicleJourney);
    if (serviceJourney != null) {
      trips = Set.of(serviceJourney);
    } else {
      trips = fuzzyTripMatcher.match(monitoredVehicleJourney, entityResolver);
    }

    if (trips == null || trips.isEmpty()) {
      if (keepLogging) {
        String lineRef =
          (
            monitoredVehicleJourney.getLineRef() != null
              ? monitoredVehicleJourney.getLineRef().getValue()
              : null
          );
        String vehicleRef =
          (
            monitoredVehicleJourney.getVehicleRef() != null
              ? monitoredVehicleJourney.getVehicleRef().getValue()
              : null
          );
        String tripId =
          (
            monitoredVehicleJourney.getCourseOfJourneyRef() != null
              ? monitoredVehicleJourney.getCourseOfJourneyRef().getValue()
              : null
          );
        LOG.debug(
          "No trip found for [isMonitored={}, lineRef={}, vehicleRef={}, tripId={}], skipping VehicleActivity.",
          isMonitored,
          lineRef,
          vehicleRef,
          tripId
        );
      }

      return false;
    }

    //Find the trip that best corresponds to MonitoredVehicleJourney
    Trip trip = getTripForJourney(trips, monitoredVehicleJourney);

    if (trip == null) {
      return false;
    }

    final Set<TripPattern> patterns = getPatternsForTrip(trips, monitoredVehicleJourney);

    if (patterns == null) {
      return false;
    }
    boolean success = false;
    for (TripPattern pattern : patterns) {
      if (handleTripPatternUpdate(transitModel, pattern, activity, trip, serviceDate).isSuccess()) {
        success = true;
      }
    }

    if (!success) {
      LOG.info("Pattern not updated for trip {}", trip.getId());
    }
    return success;
  }

  private Result<?, UpdateError> handleTripPatternUpdate(
    TransitModel transitModel,
    TripPattern pattern,
    VehicleActivityStructure activity,
    Trip trip,
    LocalDate serviceDate
  ) {
    // Apply update on the *scheduled* timetable and set the updated trip times in the buffer
    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    var result = createUpdatedTripTimes(
      currentTimetable,
      activity,
      trip.getId(),
      transitModel::getStopLocationById
    );
    if (result.isFailure()) {
      return result;
    } else {
      return buffer.update(pattern, result.successValue(), serviceDate);
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

  private Result<?, UpdateError> handleAddedTrip(
    TransitModel transitModel,
    String feedId,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    // Verifying values required in SIRI Profile

    // Added ServiceJourneyId
    String newServiceJourneyRef = estimatedVehicleJourney.getEstimatedVehicleJourneyCode();
    Objects.requireNonNull(newServiceJourneyRef, "EstimatedVehicleJourneyCode is required");

    // LineRef of added trip
    Objects.requireNonNull(estimatedVehicleJourney.getLineRef(), "LineRef is required");
    String lineRef = estimatedVehicleJourney.getLineRef().getValue();

    //OperatorRef of added trip
    Objects.requireNonNull(estimatedVehicleJourney.getOperatorRef(), "OperatorRef is required");
    String operatorRef = estimatedVehicleJourney.getOperatorRef().getValue();

    String externalLineRef;
    if (estimatedVehicleJourney.getExternalLineRef() != null) {
      externalLineRef = estimatedVehicleJourney.getExternalLineRef().getValue();
    } else {
      externalLineRef = lineRef;
    }

    Operator operator = transitModel
      .getTransitModelIndex()
      .getOperatorForId()
      .get(new FeedScopedId(feedId, operatorRef));

    FeedScopedId tripId = new FeedScopedId(feedId, newServiceJourneyRef);

    Route replacedRoute = getReplacedRoute(transitModel, feedId, externalLineRef);

    FeedScopedId routeId = new FeedScopedId(feedId, lineRef);
    Route route = transitModel.getTransitModelIndex().getRouteForId(routeId);

    Mode transitMode = AddedTripHelper.getTransitMode(
      estimatedVehicleJourney.getVehicleModes(),
      replacedRoute
    );

    if (route == null) {
      route =
        AddedTripHelper.getRoute(
          transitModel.getTransitModelIndex().getAllRoutes(),
          estimatedVehicleJourney.getPublishedLineNames(),
          operator,
          replacedRoute,
          routeId,
          transitMode
        );
      LOG.info("Adding route {} to transitModel.", routeId);
      transitModel.getTransitModelIndex().addRoutes(route);
    }

    LocalDate serviceDate = getServiceDateForEstimatedVehicleJourney(estimatedVehicleJourney);
    if (serviceDate == null) {
      return Result.failure(new UpdateError(tripId, NO_START_DATE));
    }
    FeedScopedId calServiceId = transitModel.getOrCreateServiceIdForDate(serviceDate);

    var tripResult = AddedTripHelper.getTrip(
      tripId,
      route,
      operator,
      transitMode,
      estimatedVehicleJourney.getDestinationNames(),
      calServiceId
    );

    if (tripResult.isFailure()) {
      return tripResult;
    }
    Trip trip = tripResult.successValue();

    List<StopLocation> addedStops = new ArrayList<>();
    List<StopTime> aimedStopTimes = new ArrayList<>();
    List<EstimatedCall> estimatedCalls;
    List<RecordedCall> recordedCalls;

    if (
      estimatedVehicleJourney.getRecordedCalls() != null &&
      estimatedVehicleJourney.getRecordedCalls().getRecordedCalls() != null
    ) {
      recordedCalls = estimatedVehicleJourney.getRecordedCalls().getRecordedCalls();
    } else {
      recordedCalls = List.of();
    }

    if (
      estimatedVehicleJourney.getEstimatedCalls() != null &&
      estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls() != null
    ) {
      estimatedCalls = estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls();
    } else {
      estimatedCalls = List.of();
    }

    int numStops = recordedCalls.size() + estimatedCalls.size();

    ZonedDateTime departureDate = serviceDate.atStartOfDay(timeZone);

    int stopSequence = 0;
    stopSequence =
      handleRecordedCalls(
        transitModel,
        feedId,
        trip,
        addedStops,
        aimedStopTimes,
        recordedCalls,
        numStops,
        departureDate,
        stopSequence
      );

    handleEstimatedCalls(
      transitModel,
      feedId,
      trip,
      addedStops,
      aimedStopTimes,
      estimatedCalls,
      numStops,
      departureDate,
      stopSequence
    );

    StopPattern stopPattern = new StopPattern(aimedStopTimes);

    var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);

    // TODO: We always create a new TripPattern to be able to modify its scheduled timetable
    TripPattern pattern = TripPattern
      .of(id)
      .withRoute(trip.getRoute())
      .withMode(trip.getMode())
      .withStopPattern(stopPattern)
      .build();

    TripTimes tripTimes = new TripTimes(trip, aimedStopTimes, transitModel.getDeduplicator());

    boolean isJourneyPredictionInaccurate =
      (
        estimatedVehicleJourney.isPredictionInaccurate() != null &&
        estimatedVehicleJourney.isPredictionInaccurate()
      );

    // Loop through calls again and apply updates
    stopSequence = 0;
    for (var recordedCall : recordedCalls) {
      TimetableHelper.applyUpdates(
        departureDate,
        aimedStopTimes,
        tripTimes,
        stopSequence,
        isJourneyPredictionInaccurate,
        recordedCall,
        estimatedVehicleJourney.getOccupancy()
      );
      stopSequence++;
    }
    for (var estimatedCall : estimatedCalls) {
      TimetableHelper.applyUpdates(
        departureDate,
        aimedStopTimes,
        tripTimes,
        stopSequence,
        isJourneyPredictionInaccurate,
        estimatedCall,
        estimatedVehicleJourney.getOccupancy()
      );
      stopSequence++;
    }

    // Adding trip to index necessary to include values in graphql-queries
    // TODO - SIRI: should more data be added to index?
    transitModel.getTransitModelIndex().getTripForId().put(tripId, trip);
    transitModel.getTransitModelIndex().getPatternForTrip().put(trip, pattern);

    if (
      estimatedVehicleJourney.isCancellation() != null && estimatedVehicleJourney.isCancellation()
    ) {
      tripTimes.cancelTrip();
    } else {
      tripTimes.setRealTimeState(RealTimeState.ADDED);
    }

    tripTimes.setServiceCode(transitModel.getServiceCodes().get(calServiceId));

    pattern.add(tripTimes);

    /* Validate */
    tripTimes
      .validateNonIncreasingTimes()
      .ifFailure(error -> {
        throw new IllegalStateException(
          String.format(
            "Non-increasing triptimes for added trip at stop index %d, error %s",
            error.stopIndex(),
            error.errorType()
          )
        );
      });

    /* commit */
    return addTripToGraphAndBuffer(
      feedId,
      transitModel,
      trip,
      aimedStopTimes,
      addedStops,
      tripTimes,
      serviceDate,
      estimatedVehicleJourney
    );
  }

  private void handleEstimatedCalls(
    TransitModel transitModel,
    String feedId,
    Trip trip,
    List<StopLocation> addedStops,
    List<StopTime> aimedStopTimes,
    List<EstimatedCall> estimatedCalls,
    int numStops,
    ZonedDateTime departureDate,
    int stopSequence
  ) {
    for (EstimatedCall estimatedCall : estimatedCalls) {
      var stop = getStopForStopId(transitModel, feedId, estimatedCall.getStopPointRef().getValue());

      // Update destination display
      String destinationDisplay = AddedTripHelper.getFirstNameFromList(
        estimatedCall.getDestinationDisplaies()
      );

      StopTime stopTime = createStopTime(
        trip,
        numStops,
        departureDate,
        stopSequence,
        estimatedCall.getAimedArrivalTime(),
        estimatedCall.getAimedDepartureTime(),
        stop,
        destinationDisplay
      );

      // Update pickup / dropof
      var pickUpType = TimetableHelper.mapPickUpType(
        stopTime.getPickupType(),
        estimatedCall.getDepartureBoardingActivity()
      );
      pickUpType.ifPresent(stopTime::setPickupType);

      var dropOffType = TimetableHelper.mapDropOffType(
        stopTime.getDropOffType(),
        estimatedCall.getArrivalBoardingActivity()
      );
      dropOffType.ifPresent(stopTime::setDropOffType);

      addedStops.add(stop);
      aimedStopTimes.add(stopTime);
      stopSequence++;
    }
  }

  private StopTime createStopTime(
    Trip trip,
    int numStops,
    ZonedDateTime departureDate,
    int stopSequence,
    ZonedDateTime aimedArrivalTime,
    ZonedDateTime aimedDepartureTime,
    StopLocation stop,
    String destinationDisplay
  ) {
    TimeForStop arrivalAndDepartureTime = getArrivalAndDepartureTime(
      numStops,
      departureDate,
      stopSequence,
      aimedArrivalTime,
      aimedDepartureTime
    );

    return AddedTripHelper.createStopTime(
      trip,
      stopSequence,
      stop,
      arrivalAndDepartureTime.arrivalTime(),
      arrivalAndDepartureTime.departureTime(),
      destinationDisplay
    );
  }

  private int handleRecordedCalls(
    TransitModel transitModel,
    String feedId,
    Trip trip,
    List<StopLocation> addedStops,
    List<StopTime> aimedStopTimes,
    List<RecordedCall> recordedCalls,
    int numStops,
    ZonedDateTime departureDate,
    int stopSequence
  ) {
    for (RecordedCall recordedCall : recordedCalls) {
      var stop = getStopForStopId(transitModel, feedId, recordedCall.getStopPointRef().getValue());
      StopTime stopTime = createStopTime(
        trip,
        numStops,
        departureDate,
        stopSequence,
        recordedCall.getAimedArrivalTime(),
        recordedCall.getAimedDepartureTime(),
        stop,
        "" // Destination display not present on recorded call
      );

      addedStops.add(stop);
      aimedStopTimes.add(stopTime);
      stopSequence++;
    }
    return stopSequence;
  }

  private TimeForStop getArrivalAndDepartureTime(
    int numStops,
    ZonedDateTime departureDate,
    int i,
    ZonedDateTime aimedArrivalTime,
    ZonedDateTime aimedDepartureTime
  ) {
    if (aimedArrivalTime == null) {
      aimedArrivalTime = aimedDepartureTime;
    } else if (aimedDepartureTime == null) {
      aimedDepartureTime = aimedArrivalTime;
    }

    var aimedDepartureTimeSeconds = calculateSecondsSinceMidnight(
      departureDate,
      aimedDepartureTime
    );
    var aimedArrivalTimeSeconds = calculateSecondsSinceMidnight(departureDate, aimedArrivalTime);

    return AddedTripHelper.getTimeForStop(
      aimedArrivalTimeSeconds,
      aimedDepartureTimeSeconds,
      i,
      numStops
    );
  }

  private StopLocation getStopForStopId(TransitModel transitModel, String feedId, String id) {
    return transitModel.getStopLocationById(new FeedScopedId(feedId, id));
  }

  private Route getReplacedRoute(TransitModel transitModel, String feedId, String externalLineRef) {
    return externalLineRef != null
      ? transitModel.getTransitModelIndex().getRouteForId(new FeedScopedId(feedId, externalLineRef))
      : null;
  }

  private Result<?, List<UpdateError>> handleModifiedTrip(
    TransitModel transitModel,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    String feedId,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    //Check if EstimatedVehicleJourney is reported as NOT monitored
    if (estimatedVehicleJourney.isMonitored() != null && !estimatedVehicleJourney.isMonitored()) {
      //Ignore the notMonitored-flag if the journey is NOT monitored because it has been cancelled
      if (
        estimatedVehicleJourney.isCancellation() != null &&
        !estimatedVehicleJourney.isCancellation()
      ) {
        return Result.success();
      }
    }

    //Values used in logging
    String operatorRef =
      (
        estimatedVehicleJourney.getOperatorRef() != null
          ? estimatedVehicleJourney.getOperatorRef().getValue()
          : null
      );
    String vehicleModes = "" + estimatedVehicleJourney.getVehicleModes();
    String lineRef = estimatedVehicleJourney.getLineRef().getValue();
    String vehicleRef =
      (
        estimatedVehicleJourney.getVehicleRef() != null
          ? estimatedVehicleJourney.getVehicleRef().getValue()
          : null
      );

    LocalDate serviceDate = getServiceDateForEstimatedVehicleJourney(estimatedVehicleJourney);

    if (serviceDate == null) {
      return Result.success();
    }

    Set<TripTimes> times = new HashSet<>();
    Set<TripPattern> patterns = new HashSet<>();

    Trip tripMatchedByServiceJourneyId = entityResolver.resolveTrip(estimatedVehicleJourney);

    if (tripMatchedByServiceJourneyId != null) {
      /*
              Found exact match
             */
      TripPattern exactPattern = transitService.getPatternForTrip(tripMatchedByServiceJourneyId);

      if (exactPattern != null) {
        Timetable currentTimetable = getCurrentTimetable(exactPattern, serviceDate);
        var updateResult = createUpdatedTripTimes(
          currentTimetable,
          estimatedVehicleJourney,
          tripMatchedByServiceJourneyId.getId(),
          transitModel::getStopLocationById,
          timeZone,
          transitModel.getDeduplicator()
        );
        if (updateResult.isSuccess()) {
          times.add(updateResult.successValue());
          patterns.add(exactPattern);
        } else {
          LOG.info(
            "Failed to update TripTimes for trip found by exact match {}",
            tripMatchedByServiceJourneyId.getId()
          );
          return Result.failure(List.of(updateResult.failureValue()));
        }
      }
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      Set<Trip> trips = fuzzyTripMatcher.match(estimatedVehicleJourney, entityResolver);

      if (trips == null || trips.isEmpty()) {
        LOG.debug(
          "No trips found for EstimatedVehicleJourney. [operator={}, vehicleModes={}, lineRef={}, vehicleRef={}]",
          operatorRef,
          vehicleModes,
          lineRef,
          vehicleRef
        );
        return Result.failure(List.of(new UpdateError(null, NO_FUZZY_TRIP_MATCH)));
      }

      //Find the trips that best corresponds to EstimatedVehicleJourney
      Set<Trip> matchingTrips = getTripForJourney(trips, estimatedVehicleJourney);

      if (matchingTrips == null || matchingTrips.isEmpty()) {
        LOG.debug(
          "Found no matching trip for SIRI ET (serviceDate, departureTime). [operator={}, vehicleModes={}, lineRef={}, vehicleJourneyRef={}]",
          operatorRef,
          vehicleModes,
          lineRef,
          vehicleRef
        );
        return Result.failure(List.of(new UpdateError(null, NO_FUZZY_TRIP_MATCH)));
      }

      for (Trip matchingTrip : matchingTrips) {
        TripPattern pattern = getPatternForTrip(matchingTrip, estimatedVehicleJourney);
        if (pattern != null) {
          Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
          var updateResult = createUpdatedTripTimes(
            currentTimetable,
            estimatedVehicleJourney,
            matchingTrip.getId(),
            transitModel::getStopLocationById,
            timeZone,
            transitModel.getDeduplicator()
          );
          updateResult.ifSuccess(tripTimes -> {
            patterns.add(pattern);
            times.add(tripTimes);
          });
        }
      }
    }

    if (patterns.isEmpty()) {
      LOG.debug(
        "Found no matching pattern for SIRI ET (firstStopId, lastStopId, numberOfStops). [operator={}, vehicleModes={}, lineRef={}, vehicleRef={}]",
        operatorRef,
        vehicleModes,
        lineRef,
        vehicleRef
      );
      return Result.failure(List.of(new UpdateError(null, TRIP_NOT_FOUND_IN_PATTERN)));
    }

    if (times.isEmpty()) {
      return Result.failure(List.of(new UpdateError(null, NO_UPDATES)));
    }

    List<UpdateError> errors = new ArrayList<>();
    for (TripTimes tripTimes : times) {
      Trip trip = tripTimes.getTrip();
      for (TripPattern pattern : patterns) {
        if (tripTimes.getNumStops() == pattern.numberOfStops()) {
          // All tripTimes should be handled the same way to always allow latest realtime-update to
          // replace previous update regardless of realtimestate
          cancelScheduledTrip(trip, serviceDate);

          // Also check whether trip id has been used for previously ADDED/MODIFIED trip message and
          // remove the previously created trip
          removePreviousRealtimeUpdate(trip, serviceDate);

          if (!tripTimes.isCanceled()) {
            // Calculate modified stop-pattern
            var modifiedStops = createModifiedStops(
              pattern,
              estimatedVehicleJourney,
              transitModel::getStopLocationById
            );
            List<StopTime> modifiedStopTimes = createModifiedStopTimes(
              pattern,
              tripTimes,
              estimatedVehicleJourney,
              transitModel::getStopLocationById
            );

            if (modifiedStops != null && modifiedStops.isEmpty()) {
              tripTimes.cancelTrip();
            } else {
              // Add new trip
              addTripToGraphAndBuffer(
                feedId,
                transitModel,
                trip,
                modifiedStopTimes,
                modifiedStops,
                tripTimes,
                serviceDate,
                estimatedVehicleJourney
              )
                .ifFailure(errors::add);
            }
          }

          LOG.debug("Applied realtime data for trip {}", trip.getId().getId());
        } else {
          LOG.debug("Ignoring update since number of stops do not match");
        }
      }
    }

    if (errors.isEmpty()) {
      return Result.success();
    } else {
      return Result.failure(errors);
    }
  }

  private LocalDate getServiceDateForEstimatedVehicleJourney(
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    ZonedDateTime date;
    if (
      estimatedVehicleJourney.getRecordedCalls() != null &&
      !estimatedVehicleJourney.getRecordedCalls().getRecordedCalls().isEmpty()
    ) {
      date =
        estimatedVehicleJourney
          .getRecordedCalls()
          .getRecordedCalls()
          .get(0)
          .getAimedDepartureTime();
    } else {
      EstimatedCall firstCall = estimatedVehicleJourney
        .getEstimatedCalls()
        .getEstimatedCalls()
        .get(0);
      date = firstCall.getAimedDepartureTime();
    }

    if (date == null) {
      return null;
    }

    return date.toLocalDate();
  }

  private int calculateSecondsSinceMidnight(ZonedDateTime dateTime) {
    return ServiceDateUtils.secondsSinceStartOfService(
      dateTime,
      dateTime,
      transitService.getTimeZone()
    );
  }

  private int calculateSecondsSinceMidnight(ZonedDateTime startOfService, ZonedDateTime dateTime) {
    return ServiceDateUtils.secondsSinceStartOfService(
      startOfService,
      dateTime,
      transitService.getTimeZone()
    );
  }

  /**
   * Add a (new) trip to the transitModel and the buffer
   */
  private Result<?, UpdateError> addTripToGraphAndBuffer(
    final String feedId,
    final TransitModel transitModel,
    final Trip trip,
    final List<StopTime> stopTimes,
    final List<StopLocation> stops,
    TripTimes updatedTripTimes,
    final LocalDate serviceDate,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    // Preconditions
    Objects.requireNonNull(stops);
    Preconditions.checkArgument(
      stopTimes.size() == stops.size(),
      "number of stop should match the number of stop time updates"
    );

    // Create StopPattern
    final StopPattern stopPattern = new StopPattern(stopTimes);

    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      stopPattern,
      trip,
      transitModel,
      serviceDate
    );

    // Add new trip times to the buffer and return success
    var result = buffer.update(pattern, updatedTripTimes, serviceDate);

    // Add TripOnServiceDate to buffer if a dated service journey id is supplied in the SIRI message
    addTripOnServiceDateToBuffer(trip, serviceDate, estimatedVehicleJourney, feedId);

    return result;
  }

  private void addTripOnServiceDateToBuffer(
    Trip trip,
    LocalDate serviceDate,
    EstimatedVehicleJourney estimatedVehicleJourney,
    String feedId
  ) {
    var datedServiceJourney = resolveDatedServiceJourneyId(estimatedVehicleJourney);

    datedServiceJourney.ifPresent(datedServiceJourneyId -> {
      // VehicleJourneyRef is the reference to the serviceJourney being replaced.
      var replacementRef = Optional.ofNullable(estimatedVehicleJourney.getVehicleJourneyRef());
      List<TripOnServiceDate> listOfReplacedVehicleJourneys = new ArrayList<>();
      replacementRef.ifPresent(ref -> {
        var id = new FeedScopedId(feedId, ref.getValue());
        var replacedTrip = buffer.getRealtimeAddedTripOnServiceDate().get(id);
        listOfReplacedVehicleJourneys.add(replacedTrip);
      });

      // Add additional replaced service journeys if present.
      Optional
        .ofNullable(estimatedVehicleJourney.getAdditionalVehicleJourneyReves())
        .ifPresent(additionalVehicleJourneyReves -> {
          additionalVehicleJourneyReves.forEach(vehicleJourneyRef -> {
            var id = new FeedScopedId(feedId, vehicleJourneyRef.getDatedVehicleJourneyRef());
            var replacedTrip = buffer.getRealtimeAddedTripOnServiceDate().get(id);
            listOfReplacedVehicleJourneys.add(replacedTrip);
          });
        });

      buffer.addLastAddedTripOnServiceDate(
        TripOnServiceDate
          .of(new FeedScopedId(feedId, datedServiceJourneyId))
          .withTrip(trip)
          .withReplacementFor(listOfReplacedVehicleJourneys)
          .withServiceDate(serviceDate)
          .build()
      );
    });
  }

  private Optional<String> resolveDatedServiceJourneyId(
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    // Fallback to FramedVehicleJourneyRef if DatedVehicleJourneyRef is not set
    Supplier<Optional<String>> getFramedVehicleJourney = () ->
      Optional
        .ofNullable(estimatedVehicleJourney.getFramedVehicleJourneyRef())
        .map(FramedVehicleJourneyRefStructure::getDatedVehicleJourneyRef);

    // Support for an added DatedServiceJourney ID.
    Supplier<Optional<String>> getEstimatedVehicleJourneyCode = () ->
      Optional.ofNullable(estimatedVehicleJourney.getEstimatedVehicleJourneyCode());

    return Optional
      .ofNullable(estimatedVehicleJourney.getDatedVehicleJourneyRef())
      .map(DatedVehicleJourneyRef::getValue)
      .or(getFramedVehicleJourney)
      .or(getEstimatedVehicleJourneyCode);
  }

  /**
   * Cancel scheduled trip in buffer given trip on service date
   *
   * @param serviceDate service date
   * @return true if scheduled trip was cancelled
   */
  private boolean cancelScheduledTrip(Trip trip, final LocalDate serviceDate) {
    boolean success = false;

    final TripPattern pattern = transitService.getPatternForTrip(trip);

    if (pattern != null) {
      // Cancel scheduled trip times for this trip in this pattern
      final Timetable timetable = pattern.getScheduledTimetable();
      final TripTimes tripTimes = timetable.getTripTimes(trip);

      if (tripTimes == null) {
        LOG.warn("Could not cancel scheduled trip {}", trip.getId());
      } else {
        final TripTimes newTripTimes = new TripTimes(tripTimes);
        newTripTimes.cancelTrip();
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

  private Set<TripPattern> getPatternsForTrip(
    Set<Trip> matches,
    MonitoredVehicleJourneyStructure monitoredVehicleJourney
  ) {
    if (monitoredVehicleJourney.getOriginRef() == null) {
      return null;
    }

    ZonedDateTime date = monitoredVehicleJourney.getOriginAimedDepartureTime();
    if (date == null) {
      //If no date is set - assume Realtime-data is reported for 'today'.
      date = ZonedDateTime.now(transitService.getTimeZone());
    }
    LocalDate realTimeReportedServiceDate = date.toLocalDate();

    Set<TripPattern> patterns = new HashSet<>();
    for (Trip currentTrip : matches) {
      TripPattern tripPattern = transitService.getPatternForTrip(currentTrip);
      Set<LocalDate> serviceDates = transitService
        .getCalendarService()
        .getServiceDatesForServiceId(currentTrip.getServiceId());

      if (!serviceDates.contains(realTimeReportedServiceDate)) {
        // Current trip has no service on the date of the 'MonitoredVehicleJourney'
        continue;
      }

      var firstStop = tripPattern.getStop(0);
      var lastStop = tripPattern.lastStop();

      String siriOriginRef = monitoredVehicleJourney.getOriginRef().getValue();

      if (monitoredVehicleJourney.getDestinationRef() != null) {
        String siriDestinationRef = monitoredVehicleJourney.getDestinationRef().getValue();

        boolean firstStopIsMatch = firstStop.getId().getId().equals(siriOriginRef);
        boolean lastStopIsMatch = lastStop.getId().getId().equals(siriDestinationRef);

        if (!firstStopIsMatch && firstStop.isPartOfStation()) {
          var otherFirstStop = transitService.getRegularStop(
            new FeedScopedId(firstStop.getId().getFeedId(), siriOriginRef)
          );
          firstStopIsMatch = firstStop.isPartOfSameStationAs(otherFirstStop);
        }

        if (!lastStopIsMatch && lastStop.isPartOfStation()) {
          var otherLastStop = transitService.getRegularStop(
            new FeedScopedId(lastStop.getId().getFeedId(), siriDestinationRef)
          );
          lastStopIsMatch = lastStop.isPartOfSameStationAs(otherLastStop);
        }

        if (firstStopIsMatch & lastStopIsMatch) {
          // Origin and destination matches
          TripPattern realtimeAddedTripPattern = buffer.getRealtimeAddedTripPattern(
            currentTrip.getId(),
            realTimeReportedServiceDate
          );
          if (realtimeAddedTripPattern != null) {
            patterns.add(realtimeAddedTripPattern);
          } else {
            patterns.add(tripPattern);
          }
        }
      } else {
        //Match origin only - since destination is not defined
        if (firstStop.getId().getId().equals(siriOriginRef)) {
          tripPattern.getScheduledTimetable().getTripTimes().get(0).getDepartureTime(0); // TODO does this line do anything?
          patterns.add(tripPattern);
        }
      }
    }
    return patterns;
  }

  private TripPattern getPatternForTrip(Trip trip, EstimatedVehicleJourney journey) {
    Set<LocalDate> serviceDates = transitService
      .getCalendarService()
      .getServiceDatesForServiceId(trip.getServiceId());

    List<RecordedCall> recordedCalls =
      (
        journey.getRecordedCalls() != null
          ? journey.getRecordedCalls().getRecordedCalls()
          : new ArrayList<>()
      );
    List<EstimatedCall> estimatedCalls;
    if (journey.getEstimatedCalls() != null) {
      estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();
    } else {
      return null;
    }

    String journeyFirstStopId;
    String journeyLastStopId;
    LocalDate journeyDate;
    //Resolve first stop - check recordedCalls, then estimatedCalls
    if (recordedCalls != null && !recordedCalls.isEmpty()) {
      RecordedCall recordedCall = recordedCalls.get(0);
      journeyFirstStopId = recordedCall.getStopPointRef().getValue();
      journeyDate = recordedCall.getAimedDepartureTime().toLocalDate();
    } else if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
      EstimatedCall estimatedCall = estimatedCalls.get(0);
      journeyFirstStopId = estimatedCall.getStopPointRef().getValue();
      journeyDate = estimatedCall.getAimedDepartureTime().toLocalDate();
    } else {
      return null;
    }

    //Resolve last stop - check estimatedCalls, then recordedCalls
    if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
      EstimatedCall estimatedCall = estimatedCalls.get(estimatedCalls.size() - 1);
      journeyLastStopId = estimatedCall.getStopPointRef().getValue();
    } else if (recordedCalls != null && !recordedCalls.isEmpty()) {
      RecordedCall recordedCall = recordedCalls.get(recordedCalls.size() - 1);
      journeyLastStopId = recordedCall.getStopPointRef().getValue();
    } else {
      return null;
    }

    TripPattern realtimeAddedTripPattern = null;
    TimetableSnapshot timetableSnapshot = snapshot;
    if (timetableSnapshot != null) {
      realtimeAddedTripPattern =
        timetableSnapshot.getRealtimeAddedTripPattern(trip.getId(), journeyDate);
    }

    TripPattern tripPattern;
    if (realtimeAddedTripPattern != null) {
      tripPattern = realtimeAddedTripPattern;
    } else {
      tripPattern = transitService.getPatternForTrip(trip);
    }

    var firstStop = tripPattern.firstStop();
    var lastStop = tripPattern.lastStop();

    if (serviceDates.contains(journeyDate)) {
      boolean firstStopIsMatch = firstStop.getId().getId().equals(journeyFirstStopId);
      boolean lastStopIsMatch = lastStop.getId().getId().equals(journeyLastStopId);

      if (!firstStopIsMatch && firstStop.isPartOfStation()) {
        var otherFirstStop = transitService.getRegularStop(
          new FeedScopedId(firstStop.getId().getFeedId(), journeyFirstStopId)
        );
        firstStopIsMatch = firstStop.isPartOfSameStationAs(otherFirstStop);
      }

      if (!lastStopIsMatch && lastStop.isPartOfStation()) {
        var otherLastStop = transitService.getRegularStop(
          new FeedScopedId(lastStop.getId().getFeedId(), journeyLastStopId)
        );
        lastStopIsMatch = lastStop.isPartOfSameStationAs(otherLastStop);
      }

      if (firstStopIsMatch & lastStopIsMatch) {
        // Found matches
        return tripPattern;
      }

      return null;
    }

    return null;
  }

  /**
   * Finds the correct trip based on OTP-ServiceDate and SIRI-DepartureTime
   */
  private Trip getTripForJourney(
    Set<Trip> trips,
    MonitoredVehicleJourneyStructure monitoredVehicleJourney
  ) {
    ZonedDateTime date = monitoredVehicleJourney.getOriginAimedDepartureTime();
    if (date == null) {
      //If no date is set - assume Realtime-data is reported for 'today'.
      date = ZonedDateTime.now();
    }
    LocalDate serviceDate = date.toLocalDate();

    List<Trip> results = new ArrayList<>();
    for (Trip trip : trips) {
      Set<LocalDate> serviceDatesForServiceId = transitService
        .getCalendarService()
        .getServiceDatesForServiceId(trip.getServiceId());

      for (LocalDate next : serviceDatesForServiceId) {
        if (next.equals(serviceDate)) {
          results.add(trip);
        }
      }
    }

    if (results.size() == 1) {
      return results.get(0);
    } else if (results.size() > 1) {
      // Multiple possible matches - check if lineRef/routeId matches
      if (
        monitoredVehicleJourney.getLineRef() != null &&
        monitoredVehicleJourney.getLineRef().getValue() != null
      ) {
        String lineRef = monitoredVehicleJourney.getLineRef().getValue();
        for (Trip trip : results) {
          if (lineRef.equals(trip.getRoute().getId().getId())) {
            // Return first trip where the lineRef matches routeId
            return trip;
          }
        }
      }

      // Line does not match any routeId - return first result.
      return results.get(0);
    }

    return null;
  }

  /**
   * Finds the correct trip based on OTP-ServiceDate and SIRI-DepartureTime
   */
  private Set<Trip> getTripForJourney(Set<Trip> trips, EstimatedVehicleJourney journey) {
    List<RecordedCall> recordedCalls =
      (
        journey.getRecordedCalls() != null
          ? journey.getRecordedCalls().getRecordedCalls()
          : new ArrayList<>()
      );
    List<EstimatedCall> estimatedCalls =
      (
        journey.getEstimatedCalls() != null ? journey.getEstimatedCalls().getEstimatedCalls() : null
      );

    ZonedDateTime date;
    int stopNumber = 1;
    String firstStopId;
    if (recordedCalls != null && !recordedCalls.isEmpty()) {
      RecordedCall recordedCall = recordedCalls.get(0);
      date = recordedCall.getAimedDepartureTime();
      firstStopId = recordedCall.getStopPointRef().getValue();
    } else if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
      EstimatedCall estimatedCall = estimatedCalls.get(0);
      if (estimatedCall.getOrder() != null) {
        stopNumber = estimatedCall.getOrder().intValue();
      } else if (estimatedCall.getVisitNumber() != null) {
        stopNumber = estimatedCall.getVisitNumber().intValue();
      }
      firstStopId = estimatedCall.getStopPointRef().getValue();
      date = estimatedCall.getAimedDepartureTime();
    } else {
      return null;
    }

    if (date == null) {
      //If no date is set - assume Realtime-data is reported for 'today'.
      date = ZonedDateTime.now(transitService.getTimeZone());
    }
    LocalDate serviceDate = date.toLocalDate();

    int departureInSecondsSinceMidnight = calculateSecondsSinceMidnight(date);
    Set<Trip> result = new HashSet<>();
    for (Trip trip : trips) {
      Set<LocalDate> serviceDatesForServiceId = transitService
        .getCalendarService()
        .getServiceDatesForServiceId(trip.getServiceId());
      if (serviceDatesForServiceId.contains(serviceDate)) {
        TripPattern pattern = transitService.getPatternForTrip(trip);

        if (stopNumber < pattern.numberOfStops()) {
          boolean firstReportedStopIsFound = false;
          var stop = pattern.getStop(stopNumber - 1);
          if (firstStopId.equals(stop.getId().getId())) {
            firstReportedStopIsFound = true;
          } else {
            if (stop.isPartOfStation()) {
              var alternativeStop = transitService.getRegularStop(
                new FeedScopedId(stop.getId().getFeedId(), firstStopId)
              );
              if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                firstReportedStopIsFound = true;
              }
            }
          }
          if (firstReportedStopIsFound) {
            for (TripTimes times : getCurrentTimetable(pattern, serviceDate).getTripTimes()) {
              if (
                times.getScheduledDepartureTime(stopNumber - 1) == departureInSecondsSinceMidnight
              ) {
                if (
                  transitService
                    .getCalendarService()
                    .getServiceDatesForServiceId(times.getTrip().getServiceId())
                    .contains(serviceDate)
                ) {
                  result.add(times.getTrip());
                }
              }
            }
          }
        }
      }
    }

    if (result.size() >= 1) {
      return result;
    } else {
      return null;
    }
  }
}
