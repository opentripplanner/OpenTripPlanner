package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.ext.siri.mapper.PickDropMapper;
import org.opentripplanner.framework.time.ServiceDateUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.OccupancyEnumeration;

/**
 * A helper class for creating new StopPattern and TripTimes based on a SIRI-ET
 * EstimatedVehicleJourney.
 */
public class ModifiedTripBuilder {

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
    boolean predictionInaccurate
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
  }

  /**
   * Create a new StopPattern and TripTimes for the trip based on the calls, and other fields read
   * in form the SIRI-ET update.
   */
  public Result<TripUpdate, UpdateError> build() {
    RealTimeTripTimes newTimes = existingTripTimes.copyScheduledTimes();

    var stopPattern = createStopPattern(pattern, calls, entityResolver);

    if (cancellation || stopPattern.isAllStopsNonRoutable()) {
      LOG.debug("Trip is cancelled");
      newTimes.cancelTrip();
      return Result.success(new TripUpdate(pattern.getStopPattern(), newTimes, serviceDate));
    }

    applyUpdates(newTimes);

    if (pattern.getStopPattern().equals(stopPattern)) {
      // This is the first update, and StopPattern has not been changed
      newTimes.setRealTimeState(RealTimeState.UPDATED);
    } else {
      // This update modified stopPattern
      newTimes.setRealTimeState(RealTimeState.MODIFIED);
    }

    // TODO - Handle DataValidationException at the outemost level(pr trip)
    try {
      newTimes.validateNonIncreasingTimes();
    } catch (DataValidationException e) {
      LOG.info(
        "Invalid SIRI-ET data for trip {} - TripTimes failed to validate after applying SIRI delay propagation. {}",
        newTimes.getTrip().getId(),
        e.getMessage()
      );
      return DataValidationExceptionMapper.toResult(e);
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
      return UpdateError.result(existingTripTimes.getTrip().getId(), TOO_FEW_STOPS);
    }

    LOG.debug("A valid TripUpdate object was applied using the Timetable class update method.");
    return Result.success(new TripUpdate(stopPattern, newTimes, serviceDate));
  }

  /**
   * Applies real-time updates from the calls into newTimes.
   */
  private void applyUpdates(RealTimeTripTimes newTimes) {
    ZonedDateTime startOfService = ServiceDateUtils.asStartOfService(serviceDate, zoneId);
    Set<CallWrapper> alreadyVisited = new HashSet<>();

    int departureFromPreviousStop = 0;
    int lastDepartureDelay = 0;
    List<StopLocation> stops = pattern.getStops();
    for (int callCounter = 0; callCounter < stops.size(); callCounter++) {
      StopLocation stop = stops.get(callCounter);
      boolean foundMatch = false;

      for (CallWrapper call : calls) {
        if (alreadyVisited.contains(call)) {
          continue;
        }
        //Current stop is being updated
        RegularStop stopPoint = entityResolver.resolveQuay(call.getStopPointRef());
        foundMatch = stop.equals(stopPoint) || stop.isPartOfSameStationAs(stopPoint);
        if (foundMatch) {
          TimetableHelper.applyUpdates(
            startOfService,
            newTimes,
            callCounter,
            callCounter == (stops.size() - 1),
            predictionInaccurate,
            call,
            occupancy
          );

          alreadyVisited.add(call);

          lastDepartureDelay = newTimes.getDepartureDelay(callCounter);
          break;
        }
      }
      if (!foundMatch) {
        // No update found in calls
        if (pattern.isBoardAndAlightAt(callCounter, NONE)) {
          // When newTimes contains stops without pickup/dropoff - set both arrival/departure to previous stop's departure
          // This necessary to accommodate the case when delay is reduced/eliminated between to stops with pickup/dropoff, and
          // multiple non-pickup/dropoff stops are in between.
          newTimes.updateArrivalTime(callCounter, departureFromPreviousStop);
          newTimes.updateDepartureTime(callCounter, departureFromPreviousStop);
        } else {
          int arrivalDelay = lastDepartureDelay;
          int departureDelay = lastDepartureDelay;

          if (lastDepartureDelay == 0) {
            //No match has been found yet (i.e. still in RecordedCalls) - keep existing delays
            arrivalDelay = existingTripTimes.getArrivalDelay(callCounter);
            departureDelay = existingTripTimes.getDepartureDelay(callCounter);
          }

          newTimes.updateArrivalDelay(callCounter, arrivalDelay);
          newTimes.updateDepartureDelay(callCounter, departureDelay);
        }
      }

      departureFromPreviousStop = newTimes.getDepartureTime(callCounter);
    }
  }

  /**
   * Creates a new StopPattern, based on an existing pattern, and list of calls. The stops can be
   * replaced with stops belonging to the same Station/StopPlace. The PickDrop values are updated
   * as well.
   */
  static StopPattern createStopPattern(
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

      for (CallWrapper call : calls) {
        if (alreadyVisited.contains(call)) {
          continue;
        }

        //Current stop is being updated
        var callStop = entityResolver.resolveQuay(call.getStopPointRef());
        if (!stop.equals(callStop) && !stop.isPartOfSameStationAs(callStop)) {
          continue;
        }

        // Used in lambda
        final int stopIndex = i;
        builder.stops.with(stopIndex, callStop);

        PickDropMapper
          .mapPickUpType(call, builder.pickups.original(stopIndex))
          .ifPresent(value -> builder.pickups.with(stopIndex, value));

        PickDropMapper
          .mapDropOffType(call, builder.dropoffs.original(stopIndex))
          .ifPresent(value -> builder.dropoffs.with(stopIndex, value));

        alreadyVisited.add(call);
        break;
      }
    }
    var newStopPattern = builder.build();
    return (pattern.isModified() && pattern.getStopPattern().equals(newStopPattern))
      ? pattern.getStopPattern()
      : newStopPattern;
  }
}
