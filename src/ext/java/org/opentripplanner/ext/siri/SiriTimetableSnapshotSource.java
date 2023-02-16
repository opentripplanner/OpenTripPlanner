package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.siri.TimetableHelper.createModifiedStopTimes;
import static org.opentripplanner.ext.siri.TimetableHelper.createUpdatedTripTimes;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.UNKNOWN;
import static org.opentripplanner.model.UpdateSuccess.WarningType.NOT_MONITORED;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.model.UpdateSuccess;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.transit.model.framework.Deduplicator;
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
import org.opentripplanner.updater.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.DataFrameRefStructure;
import uk.org.siri.siri20.DatedVehicleJourneyRef;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.EstimatedVersionFrameStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.OperatorRefStructure;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.VehicleActivityCancellationStructure;
import uk.org.siri.siri20.VehicleActivityStructure;
import uk.org.siri.siri20.VehicleJourneyRef;
import uk.org.siri.siri20.VehicleMonitoringDeliveryStructure;
import uk.org.siri.siri20.VehicleRef;

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
        LocalDate serviceDate = LocalDate.now(timeZone);

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
              Result<UpdateSuccess, UpdateError> res = handleAddedTrip(
                transitModel,
                journey,
                entityResolver
              );
              results.add(res);
            } catch (Throwable t) {
              // Since this is work in progress - catch everything to continue processing updates
              LOG.warn("Adding ExtraJourney {} failed.", debugString(journey), t);
              results.add(Result.failure(UpdateError.noTripId(UNKNOWN)));
            }
          } else {
            // Updated trip
            var result = handleModifiedTrip(
              fuzzyTripMatcher,
              entityResolver,
              journey,
              transitModel::getStopLocationById,
              transitModel.getDeduplicator()
            );
            // need to put it in a new instance so the type is correct
            result.ifSuccess(value -> results.add(Result.success(value)));
            result.ifFailure(failures -> {
              List<Result<UpdateSuccess, UpdateError>> f = failures
                .stream()
                .map(Result::<UpdateSuccess, UpdateError>failure)
                .toList();
              results.addAll(f);
            });
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

    Trip trip = entityResolver.resolveTrip(monitoredVehicleJourney);
    if (trip == null) {
      trip = fuzzyTripMatcher.match(monitoredVehicleJourney, entityResolver);
    }

    if (trip == null) {
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

    final Set<TripPattern> patterns = getPatternsForTrip(Set.of(trip), monitoredVehicleJourney);

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

  private Result<UpdateSuccess, UpdateError> handleAddedTrip(
    TransitModel transitModel,
    EstimatedVehicleJourney estimatedVehicleJourney,
    EntityResolver entityResolver
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

    Operator operator = entityResolver.resolveOperator(operatorRef);
    FeedScopedId tripId = entityResolver.resolveId(newServiceJourneyRef);
    Route replacedRoute = entityResolver.resolveRoute(externalLineRef);
    Route route = entityResolver.resolveRoute(lineRef);

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
          entityResolver.resolveId(lineRef),
          transitMode
        );
      LOG.info("Adding route {} to transitModel.", route);
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
      // need to create a new result so the success type is correct
      return tripResult.toFailureResult();
    }
    Trip trip = tripResult.successValue();

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
        trip,
        aimedStopTimes,
        recordedCalls,
        numStops,
        departureDate,
        stopSequence,
        entityResolver
      );

    handleEstimatedCalls(
      trip,
      aimedStopTimes,
      estimatedCalls,
      numStops,
      departureDate,
      stopSequence,
      entityResolver
    );

    StopPattern stopPattern = new StopPattern(aimedStopTimes);

    var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);

    // TODO: We always create a new TripPattern to be able to modify its scheduled timetable
    TripPattern pattern = TripPattern
      .of(id)
      .withRoute(trip.getRoute())
      .withMode(trip.getMode())
      .withNetexSubmode(trip.getNetexSubMode())
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
    } else if (tripTimes.isAllStopsCancelled()) {
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
      trip,
      aimedStopTimes,
      tripTimes,
      serviceDate,
      estimatedVehicleJourney,
      entityResolver
    );
  }

  private void handleEstimatedCalls(
    Trip trip,
    List<StopTime> aimedStopTimes,
    List<EstimatedCall> estimatedCalls,
    int numStops,
    ZonedDateTime departureDate,
    int stopSequence,
    EntityResolver entityResolver
  ) {
    for (EstimatedCall estimatedCall : estimatedCalls) {
      var stop = entityResolver.resolveQuay(estimatedCall.getStopPointRef().getValue());

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
    Trip trip,
    List<StopTime> aimedStopTimes,
    List<RecordedCall> recordedCalls,
    int numStops,
    ZonedDateTime departureDate,
    int stopSequence,
    EntityResolver entityResolver
  ) {
    for (RecordedCall recordedCall : recordedCalls) {
      var stop = entityResolver.resolveQuay(recordedCall.getStopPointRef().getValue());
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

  private Result<UpdateSuccess, List<UpdateError>> handleModifiedTrip(
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

    LocalDate serviceDate = getServiceDateForEstimatedVehicleJourney(estimatedVehicleJourney);

    final Result<UpdateSuccess, List<UpdateError>> successNoWarnings = Result.success(
      UpdateSuccess.noWarnings()
    );
    if (serviceDate == null) {
      return successNoWarnings;
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
          getStopLocationById,
          timeZone,
          deduplicator
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
      Set<Trip> trips = fuzzyTripMatcher.match(
        estimatedVehicleJourney,
        entityResolver,
        this::getCurrentTimetable
      );

      if (trips == null || trips.isEmpty()) {
        LOG.debug(
          "No trips found for EstimatedVehicleJourney. {}",
          debugString(estimatedVehicleJourney)
        );
        return Result.failure(List.of(new UpdateError(null, NO_FUZZY_TRIP_MATCH)));
      }

      for (Trip matchingTrip : trips) {
        TripPattern pattern = getPatternForTrip(matchingTrip, estimatedVehicleJourney);
        if (pattern != null) {
          Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
          var updateResult = createUpdatedTripTimes(
            currentTimetable,
            estimatedVehicleJourney,
            matchingTrip.getId(),
            getStopLocationById,
            timeZone,
            deduplicator
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
        "Found no matching pattern for SIRI ET (firstStopId, lastStopId, numberOfStops). {}",
        debugString(estimatedVehicleJourney)
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
          markScheduledTripAsDeleted(trip, serviceDate);

          // Also check whether trip id has been used for previously ADDED/MODIFIED trip message and
          // remove the previously created trip
          removePreviousRealtimeUpdate(trip, serviceDate);

          if (!tripTimes.isDeleted()) {
            List<StopTime> modifiedStopTimes = createModifiedStopTimes(
              pattern,
              tripTimes,
              estimatedVehicleJourney,
              getStopLocationById
            );

            // Update realtime state to cancelled, if all stops have been cancelled
            if (tripTimes.isAllStopsCancelled()) {
              tripTimes.cancelTrip();
            }

            // Add new trip
            addTripToGraphAndBuffer(
              trip,
              modifiedStopTimes,
              tripTimes,
              serviceDate,
              estimatedVehicleJourney,
              entityResolver
            )
              .ifFailure(errors::add);
          }

          LOG.debug("Applied realtime data for trip {}", trip.getId().getId());
        } else {
          LOG.debug("Ignoring update since number of stops do not match");
        }
      }
    }

    if (errors.isEmpty()) {
      return successNoWarnings;
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
    return ServiceDateUtils.secondsSinceStartOfService(dateTime, dateTime, timeZone);
  }

  private int calculateSecondsSinceMidnight(ZonedDateTime startOfService, ZonedDateTime dateTime) {
    return ServiceDateUtils.secondsSinceStartOfService(startOfService, dateTime, timeZone);
  }

  /**
   * Add a (new) trip to the transitModel and the buffer
   */
  private Result<UpdateSuccess, UpdateError> addTripToGraphAndBuffer(
    final Trip trip,
    final List<StopTime> stopTimes,
    TripTimes updatedTripTimes,
    final LocalDate serviceDate,
    EstimatedVehicleJourney estimatedVehicleJourney,
    EntityResolver entityResolver
  ) {
    // Preconditions
    Objects.requireNonNull(updatedTripTimes);

    // Create StopPattern
    final StopPattern stopPattern = new StopPattern(stopTimes);

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
      date = ZonedDateTime.now(timeZone);
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

  private static String debugString(EstimatedVehicleJourney estimatedVehicleJourney) {
    return ToStringBuilder
      .of(estimatedVehicleJourney.getClass())
      .addStr(
        "EstimatedVehicleJourneyCode",
        estimatedVehicleJourney.getEstimatedVehicleJourneyCode()
      )
      .addObjOp(
        "DatedVehicleJourney",
        estimatedVehicleJourney.getDatedVehicleJourneyRef(),
        DatedVehicleJourneyRef::getValue
      )
      .addObjOp(
        "FramedVehicleJourney",
        estimatedVehicleJourney.getFramedVehicleJourneyRef(),
        it ->
          ToStringBuilder
            .of(it.getClass())
            .addStr("VehicleJourney", it.getDatedVehicleJourneyRef())
            .addObjOp("Date", it.getDataFrameRef(), DataFrameRefStructure::getValue)
            .toString()
      )
      .addObjOp(
        "Operator",
        estimatedVehicleJourney.getOperatorRef(),
        OperatorRefStructure::getValue
      )
      .addCol("VehicleModes", estimatedVehicleJourney.getVehicleModes())
      .addStr("Line", estimatedVehicleJourney.getLineRef().getValue())
      .addObjOp("Vehicle", estimatedVehicleJourney.getVehicleRef(), VehicleRef::getValue)
      .toString();
  }
}
