package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NOT_MONITORED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVehicleJourney;

/**
 * This class should be used to create snapshots of lookup tables of real-time data. This is
 * necessary to provide planning threads a consistent constant view of a graph with real-time data at
 * a specific point in time.
 */
public class SiriTimetableSnapshotSource implements TimetableSnapshotProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SiriTimetableSnapshotSource.class);

  /**
   * Use a id generator to generate TripPattern ids for new TripPatterns created by RealTime
   * updates.
   */
  private final SiriTripPatternIdGenerator tripPatternIdGenerator = new SiriTripPatternIdGenerator();
  /**
   * A synchronized cache of trip patterns that are added to the graph due to GTFS-real-time
   * messages.
   */
  private final SiriTripPatternCache tripPatternCache;

  private final TransitEditorService transitService;

  private final TimetableSnapshotManager snapshotManager;

  public SiriTimetableSnapshotSource(
    TimetableSnapshotSourceParameters parameters,
    TransitModel transitModel
  ) {
    this.snapshotManager =
      new TimetableSnapshotManager(
        transitModel.getTransitLayerUpdater(),
        parameters,
        () -> LocalDate.now(transitModel.getTimeZone())
      );
    this.transitService = new DefaultTransitService(transitModel);
    this.tripPatternCache =
      new SiriTripPatternCache(tripPatternIdGenerator, transitService::getPatternForTrip);

    transitModel.initTimetableSnapshotProvider(this);
  }

  /**
   * Method to apply a trip update list to the most recent version of the timetable snapshot.
   * FIXME RT_AB: TripUpdate is the GTFS term, and these SIRI ETs are never converted into that
   *              same internal model.
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

    snapshotManager.withLock(() -> {
      if (incrementality == FULL_DATASET) {
        // Remove all updates from the buffer
        snapshotManager.clearBuffer(feedId);
      }

      for (var etDelivery : updates) {
        for (var estimatedJourneyVersion : etDelivery.getEstimatedJourneyVersionFrames()) {
          var journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
          LOG.debug("Handling {} EstimatedVehicleJourneys.", journeys.size());
          for (EstimatedVehicleJourney journey : journeys) {
            results.add(apply(journey, transitService, fuzzyTripMatcher, entityResolver));
          }
        }
      }

      LOG.debug("message contains {} trip updates", updates.size());

      snapshotManager.purgeAndCommit();
    });

    return UpdateResult.ofResults(results);
  }

  @Override
  public TimetableSnapshot getTimetableSnapshot() {
    return snapshotManager.getTimetableSnapshot();
  }

  private Result<UpdateSuccess, UpdateError> apply(
    EstimatedVehicleJourney journey,
    TransitEditorService transitService,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver
  ) {
    boolean shouldAddNewTrip = false;
    try {
      shouldAddNewTrip = shouldAddNewTrip(journey, entityResolver);
      Result<TripUpdate, UpdateError> result;
      if (shouldAddNewTrip) {
        result =
          new AddedTripBuilder(
            journey,
            transitService,
            entityResolver,
            tripPatternIdGenerator::generateUniqueTripPatternId
          )
            .build();
      } else {
        result = handleModifiedTrip(fuzzyTripMatcher, entityResolver, journey);
      }

      if (result.isFailure()) {
        return result.toFailureResult();
      }

      /* commit */
      return addTripToGraphAndBuffer(result.successValue());
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e);
    } catch (Exception e) {
      LOG.warn(
        "{} EstimatedJourney {} failed.",
        shouldAddNewTrip ? "Adding" : "Updating",
        DebugString.of(journey),
        e
      );
      return Result.failure(UpdateError.noTripId(UNKNOWN));
    }
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

  /**
   * Get the latest timetable for TripPattern for a given service date.
   * <p>
   * Snapshot timetable is used as source if initialised, trip patterns scheduled timetable if not.
   */
  private Timetable getCurrentTimetable(TripPattern tripPattern, LocalDate serviceDate) {
    TimetableSnapshot timetableSnapshot = getTimetableSnapshot();
    if (timetableSnapshot != null) {
      return timetableSnapshot.resolve(tripPattern, serviceDate);
    }
    return tripPattern.getScheduledTimetable();
  }

  private Result<TripUpdate, UpdateError> handleModifiedTrip(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    Trip trip = entityResolver.resolveTrip(estimatedVehicleJourney);

    // Check if EstimatedVehicleJourney is reported as NOT monitored, ignore the notMonitored-flag
    // if the journey is NOT monitored because it has been cancelled
    if (
      !TRUE.equals(estimatedVehicleJourney.isMonitored()) &&
      !TRUE.equals(estimatedVehicleJourney.isCancellation())
    ) {
      return UpdateError.result(trip != null ? trip.getId() : null, NOT_MONITORED);
    }

    LocalDate serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);

    if (serviceDate == null) {
      return UpdateError.result(trip != null ? trip.getId() : null, NO_START_DATE);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitService.getPatternForTrip(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      TripAndPattern tripAndPattern = fuzzyTripMatcher.match(
        estimatedVehicleJourney,
        entityResolver,
        this::getCurrentTimetable,
        snapshotManager::getRealtimeAddedTripPattern
      );

      if (tripAndPattern == null) {
        LOG.debug(
          "No trips found for EstimatedVehicleJourney. {}",
          DebugString.of(estimatedVehicleJourney)
        );
        return UpdateError.result(null, NO_FUZZY_TRIP_MATCH);
      }

      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      return UpdateError.result(null, TRIP_NOT_FOUND);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      return UpdateError.result(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN);
    }
    var updateResult = new ModifiedTripBuilder(
      existingTripTimes,
      pattern,
      estimatedVehicleJourney,
      serviceDate,
      transitService.getTimeZone(),
      entityResolver
    )
      .build();
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
   * Add a (new) trip to the transitModel and the buffer
   */
  private Result<UpdateSuccess, UpdateError> addTripToGraphAndBuffer(TripUpdate tripUpdate) {
    Trip trip = tripUpdate.tripTimes().getTrip();
    LocalDate serviceDate = tripUpdate.serviceDate();

    // Get cached trip pattern or create one if it doesn't exist yet
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      tripUpdate.stopPattern(),
      trip,
      serviceDate
    );
    // Add new trip times to buffer, making protective copies as needed. Bubble success/error up.
    var result = snapshotManager.updateBuffer(pattern, tripUpdate.tripTimes(), serviceDate);
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

    final TripPattern pattern = transitService.getPatternForTrip(trip);

    if (pattern != null) {
      // Mark scheduled trip times for this trip in this pattern as deleted
      final Timetable timetable = pattern.getScheduledTimetable();
      final TripTimes tripTimes = timetable.getTripTimes(trip);

      if (tripTimes == null) {
        LOG.warn("Could not mark scheduled trip as deleted {}", trip.getId());
      } else {
        final RealTimeTripTimes newTripTimes = tripTimes.copyScheduledTimes();
        newTripTimes.deleteTrip();
        snapshotManager.updateBuffer(pattern, newTripTimes, serviceDate);
        success = true;
      }
    }

    return success;
  }
}
