package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.STOP_MISMATCH;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_MANY_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN_STOP;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.siri.mapping.PickDropMapper;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.OccupancyEnumeration;

/**
 * A helper class for creating new StopPattern and TripTimes based on a SIRI-ET
 * EstimatedVehicleJourney.
 */
class ModifiedTripBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(ModifiedTripBuilder.class);

  private final TripTimes existingTripTimes;
  private final TripPattern pattern;
  private final LocalDate serviceDate;
  private final ZoneId zoneId;
  private final EntityResolver entityResolver;
  private final List<CallWrapper> calls;
  private final boolean cancellation;
  private final OccupancyEnumeration occupancy;
  private final boolean predictionInaccurate;
  private final String dataSource;

  public ModifiedTripBuilder(
    TripTimes existingTripTimes,
    TripPattern pattern,
    EstimatedVehicleJourney journey,
    LocalDate serviceDate,
    ZoneId zoneId,
    EntityResolver entityResolver
  ) {
    this.existingTripTimes = existingTripTimes;
    this.pattern = pattern;
    this.serviceDate = serviceDate;
    this.zoneId = zoneId;
    this.entityResolver = entityResolver;

    calls = CallWrapper.of(journey);
    cancellation = TRUE.equals(journey.isCancellation());
    predictionInaccurate = TRUE.equals(journey.isPredictionInaccurate());
    occupancy = journey.getOccupancy();
    dataSource = journey.getDataSource();
  }

  /**
   * Constructor for tests
   */
  public ModifiedTripBuilder(
    TripTimes existingTripTimes,
    TripPattern pattern,
    LocalDate serviceDate,
    ZoneId zoneId,
    EntityResolver entityResolver,
    List<CallWrapper> calls,
    boolean cancellation,
    OccupancyEnumeration occupancy,
    boolean predictionInaccurate,
    String dataSource
  ) {
    this.existingTripTimes = existingTripTimes;
    this.pattern = pattern;
    this.serviceDate = serviceDate;
    this.zoneId = zoneId;
    this.entityResolver = entityResolver;
    this.calls = calls;
    this.cancellation = cancellation;
    this.occupancy = occupancy;
    this.predictionInaccurate = predictionInaccurate;
    this.dataSource = dataSource;
  }

  /**
   * Create a new StopPattern and TripTimes for the trip based on the calls, and other fields read
   * in form the SIRI-ET update.
   */
  public Result<TripUpdate, UpdateError> build() {
    RealTimeTripTimes newTimes = existingTripTimes.copyScheduledTimes();

    if (cancellation) {
      return cancelTrip(newTimes);
    }

    if (calls.size() < existingTripTimes.getNumStops()) {
      return UpdateError.result(existingTripTimes.getTrip().getId(), TOO_FEW_STOPS, dataSource);
    }

    if (calls.size() > existingTripTimes.getNumStops()) {
      return UpdateError.result(existingTripTimes.getTrip().getId(), TOO_MANY_STOPS, dataSource);
    }

    var result = createStopPattern(pattern, calls, entityResolver);
    if (result.isFailure()) {
      int invalidStopIndex = result.failureValue().stopIndex();
      LOG.info(
        "Invalid SIRI-ET data for trip {} - {} at stop index {}",
        existingTripTimes.getTrip().getId(),
        result.failureValue().errorType(),
        invalidStopIndex
      );
      return Result.failure(
        new UpdateError(
          existingTripTimes.getTrip().getId(),
          result.failureValue().errorType(),
          invalidStopIndex,
          dataSource
        )
      );
    }

    StopPattern stopPattern = result.successValue();
    if (stopPattern.isAllStopsNonRoutable()) {
      return cancelTrip(newTimes);
    }

    applyUpdates(newTimes);

    if (pattern.getStopPattern().equals(stopPattern)) {
      // This is the first update, and StopPattern has not been changed
      newTimes.setRealTimeState(RealTimeState.UPDATED);
    } else {
      // This update modified stopPattern
      newTimes.setRealTimeState(RealTimeState.MODIFIED);
    }

    // TODO - Handle DataValidationException at the outermost level (pr trip)
    try {
      newTimes.validateNonIncreasingTimes();
    } catch (DataValidationException e) {
      LOG.info(
        "Invalid SIRI-ET data for trip {} - TripTimes failed to validate after applying SIRI delay propagation. {}",
        newTimes.getTrip().getId(),
        e.getMessage()
      );
      return DataValidationExceptionMapper.toResult(e, dataSource);
    }

    int numStopsInUpdate = newTimes.getNumStops();
    int numStopsInPattern = pattern.numberOfStops();
    if (numStopsInUpdate != numStopsInPattern) {
      LOG.info(
        "Invalid SIRI-ET data for trip {} - Inconsistent number of updated stops ({}) and stops in pattern ({})",
        newTimes.getTrip().getId(),
        numStopsInUpdate,
        numStopsInPattern
      );
      return UpdateError.result(existingTripTimes.getTrip().getId(), TOO_FEW_STOPS, dataSource);
    }

    LOG.debug("A valid TripUpdate object was applied using the Timetable class update method.");
    return Result.success(new TripUpdate(stopPattern, newTimes, serviceDate, dataSource));
  }

  /**
   * Full cancellation of a trip.
   */
  private Result<TripUpdate, UpdateError> cancelTrip(RealTimeTripTimes newTimes) {
    newTimes.cancelTrip();
    return Result.success(
      new TripUpdate(pattern.getStopPattern(), newTimes, serviceDate, dataSource)
    );
  }

  /**
   * Applies real-time updates from the calls into newTimes.
   * Precondition: the number of calls is equal to the number of stops in the pattern (this is
   * verified before calling this method).
   */
  private void applyUpdates(RealTimeTripTimes newTimes) {
    ZonedDateTime startOfService = ServiceDateUtils.asStartOfService(serviceDate, zoneId);
    Set<CallWrapper> alreadyVisited = new HashSet<>();

    List<StopLocation> stopsInPattern = pattern.getStops();
    for (int stopIndex = 0; stopIndex < stopsInPattern.size(); stopIndex++) {
      StopLocation stopInPattern = stopsInPattern.get(stopIndex);
      CallWrapper matchingCall = null;

      for (CallWrapper call : calls) {
        if (alreadyVisited.contains(call)) {
          continue;
        }
        //Current stop is being updated
        RegularStop stopPoint = entityResolver.resolveQuay(call.getStopPointRef());
        if (stopInPattern.equals(stopPoint) || stopInPattern.isPartOfSameStationAs(stopPoint)) {
          matchingCall = call;
          break;
        }
      }

      if (matchingCall == null) {
        throw new IllegalStateException(
          "The stop at index %d on the trip %s cannot be matched with any call. This implies a bug.".formatted(
              stopIndex,
              newTimes.getTrip().getId()
            )
        );
      }

      TimetableHelper.applyUpdates(
        startOfService,
        newTimes,
        stopIndex,
        stopIndex == (stopsInPattern.size() - 1),
        predictionInaccurate,
        matchingCall,
        occupancy
      );

      alreadyVisited.add(matchingCall);
    }
  }

  /**
   * Creates a new StopPattern, based on an existing pattern, and list of calls. The stops can be
   * replaced with stops belonging to the same Station/StopPlace. The PickDrop values are updated
   * as well.
   * Precondition: the number of calls is equal to the number of stops in the pattern (this is
   * verified before calling this method).
   */
  static Result<StopPattern, UpdateError> createStopPattern(
    TripPattern pattern,
    List<CallWrapper> calls,
    EntityResolver entityResolver
  ) {
    int numberOfStops = pattern.numberOfStops();
    var builder = pattern.copyPlannedStopPattern();

    Set<CallWrapper> alreadyVisited = new HashSet<>();
    // modify updated stop-times
    for (int i = 0; i < numberOfStops; i++) {
      StopLocation stop = builder.stops.original(i);

      boolean matchFound = false;
      for (CallWrapper call : calls) {
        if (alreadyVisited.contains(call)) {
          continue;
        }

        //Current stop is being updated
        var callStop = entityResolver.resolveQuay(call.getStopPointRef());
        if (callStop == null) {
          return Result.failure(new UpdateError(null, UNKNOWN_STOP, i));
        }

        if (!stop.equals(callStop) && !stop.isPartOfSameStationAs(callStop)) {
          continue;
        }
        matchFound = true;

        // Used in lambda
        final int stopIndex = i;
        builder.stops.with(stopIndex, callStop);

        PickDropMapper.mapPickUpType(call, builder.pickups.original(stopIndex)).ifPresent(value ->
          builder.pickups.with(stopIndex, value)
        );

        PickDropMapper.mapDropOffType(call, builder.dropoffs.original(stopIndex)).ifPresent(value ->
          builder.dropoffs.with(stopIndex, value)
        );

        alreadyVisited.add(call);
        break;
      }
      if (!matchFound) {
        return Result.failure(new UpdateError(null, STOP_MISMATCH, i));
      }
    }
    var newStopPattern = builder.build();
    return (pattern.isModified() && pattern.getStopPattern().equals(newStopPattern))
      ? Result.success(pattern.getStopPattern())
      : Result.success(newStopPattern);
  }
}
