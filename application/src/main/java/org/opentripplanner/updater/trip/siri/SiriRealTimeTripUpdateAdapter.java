package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.spi.UpdateErrorType.NOT_MONITORED;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.updater.spi.UpdateErrorType.UNKNOWN;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;
import org.opentripplanner.updater.trip.patterncache.TripPatternIdGenerator;
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

  private final DeduplicatorService deduplicator;
  private final TimetableSnapshotManager snapshotManager;

  public SiriRealTimeTripUpdateAdapter(
    TimetableRepository timetableRepository,
    DeduplicatorService deduplicator,
    TimetableSnapshotManager snapshotManager
  ) {
    this.deduplicator = deduplicator;
    this.snapshotManager = snapshotManager;
    this.transitEditorService = new DefaultTransitService(
      timetableRepository,
      snapshotManager.getTimetableSnapshotBuffer()
    );
    this.tripPatternCache = new TripPatternCache(
      tripPatternIdGenerator,
      transitEditorService::findPattern
    );
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

    List<UpdateSuccess> successes = new ArrayList<>();
    List<UpdateError> errors = new ArrayList<>();

    if (incrementality == FULL_DATASET) {
      // Remove all updates from the buffer
      snapshotManager.clearBuffer(feedId);
    }

    for (var etDelivery : updates) {
      for (var estimatedJourneyVersion : etDelivery.getEstimatedJourneyVersionFrames()) {
        var journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
        LOG.debug("Handling {} EstimatedVehicleJourneys.", journeys.size());
        for (EstimatedVehicleJourney journey : journeys) {
          try {
            successes.add(apply(journey, transitEditorService, fuzzyTripMatcher, entityResolver));
          } catch (UpdateException e) {
            // TODO: remove data source from all creations on lower levels
            errors.add(e.withDataSource(journey.getDataSource()).toError());
          }
        }
      }
    }

    LOG.debug("message contains {} trip updates", updates.size());

    return UpdateResult.of(successes, errors);
  }

  private UpdateSuccess apply(
    EstimatedVehicleJourney journey,
    TransitEditorService transitService,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver
  ) throws UpdateException {
    List<CallWrapper> calls = CallWrapper.of(journey);
    SiriUpdateType siriUpdateType = null;
    try {
      siriUpdateType = updateType(journey, calls, entityResolver);
      TripUpdate result = switch (siriUpdateType) {
        case REPLACEMENT_DEPARTURE -> new AddedTripBuilder(
          journey,
          transitService,
          deduplicator,
          entityResolver,
          tripPatternIdGenerator::generateUniqueTripPatternId,
          calls
        ).build();
        case EXTRA_CALL -> handleExtraCall(fuzzyTripMatcher, entityResolver, journey, calls);
        case TRIP_UPDATE -> handleModifiedTrip(fuzzyTripMatcher, entityResolver, journey, calls);
      };

      /* commit */
      return addTripToGraphAndBuffer(result);
    } catch (UpdateException e) {
      throw e;
    } catch (DataValidationException e) {
      throw DataValidationExceptionMapper.map(e, journey.getDataSource());
    } catch (Exception e) {
      LOG.warn("{} EstimatedJourney {} failed.", siriUpdateType, DebugString.of(journey), e);
      throw UpdateException.noTripId(UNKNOWN);
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
    return snapshotManager.resolve(tripPattern, serviceDate);
  }

  private TripUpdate handleModifiedTrip(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney,
    List<CallWrapper> calls
  ) throws UpdateException {
    Trip trip = entityResolver.resolveTrip(estimatedVehicleJourney);
    String dataSource = estimatedVehicleJourney.getDataSource();

    // Check if EstimatedVehicleJourney is reported as NOT monitored, ignore the notMonitored-flag
    // if the journey is NOT monitored because it has been cancelled
    if (
      !TRUE.equals(estimatedVehicleJourney.isMonitored()) &&
      !TRUE.equals(estimatedVehicleJourney.isCancellation())
    ) {
      throw UpdateException.of(trip != null ? trip.getId() : null, NOT_MONITORED, dataSource);
    }

    LocalDate serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney, calls);

    if (serviceDate == null) {
      throw UpdateException.of(trip != null ? trip.getId() : null, NO_START_DATE, dataSource);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitEditorService.findPattern(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      var tripAndPattern = fuzzyTripMatcher.match(
        estimatedVehicleJourney,
        calls,
        entityResolver,
        this::getCurrentTimetable,
        snapshotManager::getNewTripPatternForModifiedTrip
      );
      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      throw UpdateException.of(null, TRIP_NOT_FOUND, dataSource);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      throw UpdateException.of(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN, dataSource);
    }
    var tripUpdate = new ModifiedTripBuilder(
      existingTripTimes,
      pattern,
      estimatedVehicleJourney,
      serviceDate,
      transitEditorService.getTimeZone(),
      entityResolver,
      calls
    ).build();

    TripPattern deleteFrom = !tripUpdate.stopPattern().equals(pattern.getStopPattern())
      ? pattern
      : null;

    return tripUpdate.withHideTripInScheduledPattern(deleteFrom);
  }

  private TripUpdate handleExtraCall(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney,
    List<CallWrapper> calls
  ) throws UpdateException {
    Trip trip = entityResolver.resolveTrip(estimatedVehicleJourney);
    String dataSource = estimatedVehicleJourney.getDataSource();

    // Check if EstimatedVehicleJourney is reported as NOT monitored, ignore the notMonitored-flag
    // if the journey is NOT monitored because it has been cancelled
    if (
      !TRUE.equals(estimatedVehicleJourney.isMonitored()) &&
      !TRUE.equals(estimatedVehicleJourney.isCancellation())
    ) {
      throw UpdateException.of(trip != null ? trip.getId() : null, NOT_MONITORED, dataSource);
    }

    LocalDate serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney, calls);

    if (serviceDate == null) {
      throw UpdateException.of(trip != null ? trip.getId() : null, NO_START_DATE, dataSource);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitEditorService.findPattern(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      var tripAndPattern = fuzzyTripMatcher.match(
        estimatedVehicleJourney,
        calls,
        entityResolver,
        this::getCurrentTimetable,
        snapshotManager::getNewTripPatternForModifiedTrip
      );

      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      throw UpdateException.of(null, TRIP_NOT_FOUND, dataSource);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      throw UpdateException.of(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN, dataSource);
    }
    var tripUpdate = new ExtraCallTripBuilder(
      estimatedVehicleJourney,
      transitEditorService,
      deduplicator,
      entityResolver,
      tripPatternIdGenerator::generateUniqueTripPatternId,
      trip,
      calls
    ).build();

    TripPattern deleteFrom = !tripUpdate.stopPattern().equals(pattern.getStopPattern())
      ? pattern
      : null;

    return tripUpdate.withHideTripInScheduledPattern(deleteFrom);
  }

  /**
   * Add a (new) trip to the timetableRepository and the buffer
   */
  private UpdateSuccess addTripToGraphAndBuffer(TripUpdate tripUpdate) throws UpdateException {
    Trip trip = tripUpdate.tripTimes().getTrip();
    LocalDate serviceDate = tripUpdate.serviceDate();

    final TripPattern pattern;
    if (tripUpdate.tripPatternCreation()) {
      pattern = tripUpdate.addedTripPattern();
    } else {
      // Get cached trip pattern or create one if it doesn't exist yet
      pattern = tripPatternCache.getOrCreateTripPattern(tripUpdate.stopPattern(), trip);
    }

    // Revert for TRIP_UPDATE and EXTRA_CALL, but NOT for REPLACEMENT_DEPARTURE (new trips)
    boolean revertPreviousRealTimeUpdates = !tripUpdate.tripCreation();

    // Add new trip times to buffer, making protective copies as needed. Bubble success/error up.
    RealTimeTripUpdate realTimeTripUpdate = RealTimeTripUpdate.of(
      pattern,
      tripUpdate.tripTimes(),
      serviceDate
    )
      .withAddedTripOnServiceDate(tripUpdate.addedTripOnServiceDate())
      .withTripCreation(tripUpdate.tripCreation())
      .withRouteCreation(tripUpdate.routeCreation())
      .withProducer(tripUpdate.dataSource())
      .withRevertPreviousRealTimeUpdates(revertPreviousRealTimeUpdates)
      .withHideTripInScheduledPattern(tripUpdate.hideTripInScheduledPattern())
      .build();
    var result = snapshotManager.updateBuffer(realTimeTripUpdate);
    LOG.debug("Applied real-time data for trip {} on {}", trip, serviceDate);
    return result;
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
