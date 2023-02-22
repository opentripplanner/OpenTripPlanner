package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TOO_FEW_STOPS;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.opentripplanner.ext.siri.mapper.OccupancyMapper;
import org.opentripplanner.ext.siri.mapper.PickDropMapper;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.OccupancyEnumeration;

public class TimetableHelper {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableHelper.class);

  /**
   * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
   * must not be modified directly because they may be shared with the underlying
   * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
   * protective copying of this Timetable. It is not done in this update method to avoid repeatedly
   * cloning the same Timetable when several updates are applied to it at once. We assume here that
   * all trips in a timetable are from the same feed, which should always be the case.
   *
   * @param journey SIRI-ET EstimatedVehicleJourney
   * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
   * with the id specified in the trip descriptor of the TripUpdate; null if something went wrong
   */
  public static Result<TripUpdate, UpdateError> createUpdatedTripTimes(
    TripTimes existingTripTimes,
    TripPattern pattern,
    EstimatedVehicleJourney journey,
    LocalDate serviceDate,
    ZoneId zoneId,
    EntityResolver entityResolver
  ) {
    TripTimes newTimes = new TripTimes(existingTripTimes);
    List<CallWrapper> calls = CallWrapper.of(journey);
    StopPattern stopPattern = createStopPattern(pattern, calls, entityResolver);

    if (TRUE.equals(journey.isCancellation()) || stopPattern.isAllStopsCancelled()) {
      LOG.debug("Trip is cancelled");
      newTimes.cancelTrip();
      return Result.success(new TripUpdate(pattern.getStopPattern(), newTimes, serviceDate));
    }

    OccupancyEnumeration journeyOccupancy = journey.getOccupancy();

    ZonedDateTime startOfService = ServiceDateUtils.asStartOfService(serviceDate, zoneId);
    Set<CallWrapper> alreadyVisited = new HashSet<>();

    boolean isJourneyPredictionInaccurate =
      (journey.isPredictionInaccurate() != null && journey.isPredictionInaccurate());

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
          applyUpdates(
            startOfService,
            newTimes,
            callCounter,
            callCounter == (calls.size() - 1),
            isJourneyPredictionInaccurate,
            call,
            journeyOccupancy
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

    if (pattern.getStopPattern().equals(stopPattern)) {
      // This is the first update, and StopPattern has not been changed
      newTimes.setRealTimeState(RealTimeState.UPDATED);
    } else {
      // This update modified stopPattern
      newTimes.setRealTimeState(RealTimeState.MODIFIED);
    }

    var result = newTimes.validateNonIncreasingTimes();
    if (result.isFailure()) {
      var updateError = result.failureValue();
      LOG.info(
        "TripTimes are non-increasing after applying SIRI delay propagation - LineRef {}, TripId {}. Stop index {}",
        journey.getLineRef().getValue(),
        existingTripTimes.getTrip().getId(),
        updateError.stopIndex()
      );
      return Result.failure(updateError);
    }

    if (newTimes.getNumStops() != pattern.numberOfStops()) {
      return UpdateError.result(existingTripTimes.getTrip().getId(), TOO_FEW_STOPS);
    }

    LOG.debug("A valid TripUpdate object was applied using the Timetable class update method.");
    return Result.success(new TripUpdate(stopPattern, newTimes, serviceDate));
  }

  static StopPattern createStopPattern(
    TripPattern pattern,
    List<CallWrapper> calls,
    EntityResolver entityResolver
  ) {
    int numberOfStops = pattern.numberOfStops();
    var builder = pattern.getStopPattern().mutate();

    Set<CallWrapper> alreadyVisited = new HashSet<>();
    // modify updated stop-times
    for (int i = 0; i < numberOfStops; i++) {
      StopLocation stop = pattern.getStop(i);
      builder.stops[i] = stop;
      builder.dropoffs[i] = pattern.getAlightType(i);
      builder.pickups[i] = pattern.getBoardType(i);

      for (CallWrapper call : calls) {
        if (alreadyVisited.contains(call)) {
          continue;
        }

        //Current stop is being updated
        var callStop = entityResolver.resolveQuay(call.getStopPointRef());
        if (!stop.equals(callStop) && !stop.isPartOfSameStationAs(callStop)) {
          continue;
        }

        int stopIndex = i;
        builder.stops[stopIndex] = callStop;

        PickDropMapper
          .mapPickUpType(call, builder.pickups[stopIndex])
          .ifPresent(value -> builder.pickups[stopIndex] = value);

        PickDropMapper
          .mapDropOffType(call, builder.dropoffs[stopIndex])
          .ifPresent(value -> builder.dropoffs[stopIndex] = value);

        alreadyVisited.add(call);
        break;
      }
    }

    return builder.build();
  }

  /**
   * Get the first non-null time from a list of suppliers, and convert that to seconds past start of
   * service time. If none of the suppliers provide a time, return null.
   */
  @SafeVarargs
  private static int getAvailableTime(
    ZonedDateTime startOfService,
    Supplier<ZonedDateTime>... timeSuppliers
  ) {
    for (var supplier : timeSuppliers) {
      final ZonedDateTime time = supplier.get();
      if (time != null) {
        return ServiceDateUtils.secondsSinceStartOfService(startOfService, time);
      }
    }
    return -1;
  }

  /**
   * Loop through all passed times, return the first non-negative one or the last one
   */
  private static int handleMissingRealtime(int... times) {
    if (times.length == 0) {
      throw new IllegalArgumentException("Need at least one value");
    }

    int time = -1;
    for (int t : times) {
      time = t;
      if (time >= 0) {
        break;
      }
    }

    return time;
  }

  public static void applyUpdates(
    ZonedDateTime departureDate,
    TripTimes tripTimes,
    int index,
    boolean isLastStop,
    boolean isJourneyPredictionInaccurate,
    CallWrapper call,
    OccupancyEnumeration journeyOccupancy
  ) {
    if (call.getActualDepartureTime() != null || call.getActualArrivalTime() != null) {
      //Flag as recorded
      tripTimes.setRecorded(index);
    }

    // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
    boolean isCallPredictionInaccurate = TRUE.equals(call.isPredictionInaccurate());
    if (isJourneyPredictionInaccurate || isCallPredictionInaccurate) {
      tripTimes.setPredictionInaccurate(index);
    }

    if (TRUE.equals(call.isCancellation())) {
      tripTimes.setCancelled(index);
    }

    int scheduledArrivalTime = tripTimes.getArrivalTime(index);
    int realtimeArrivalTime = getAvailableTime(
      departureDate,
      call::getActualArrivalTime,
      call::getExpectedArrivalTime
    );

    int scheduledDepartureTime = tripTimes.getDepartureTime(index);
    int realtimeDepartureTime = getAvailableTime(
      departureDate,
      call::getActualDepartureTime,
      call::getExpectedDepartureTime
    );

    int[] possibleArrivalTimes = index == 0
      ? new int[] { realtimeArrivalTime, realtimeDepartureTime, scheduledArrivalTime }
      : new int[] { realtimeArrivalTime, scheduledArrivalTime };
    var arrivalTime = handleMissingRealtime(possibleArrivalTimes);
    int arrivalDelay = arrivalTime - scheduledArrivalTime;
    tripTimes.updateArrivalDelay(index, arrivalDelay);

    int[] possibleDepartureTimes = isLastStop
      ? new int[] { realtimeDepartureTime, realtimeArrivalTime, scheduledDepartureTime }
      : new int[] { realtimeDepartureTime, scheduledDepartureTime };
    var departureTime = handleMissingRealtime(possibleDepartureTimes);
    int departureDelay = departureTime - scheduledDepartureTime;
    tripTimes.updateDepartureDelay(index, departureDelay);

    OccupancyEnumeration callOccupancy = call.getOccupancy() != null
      ? call.getOccupancy()
      : journeyOccupancy;

    if (callOccupancy != null) {
      tripTimes.setOccupancyStatus(index, OccupancyMapper.mapOccupancyStatus(callOccupancy));
    }

    if (call.getDestinationDisplaies() != null && !call.getDestinationDisplaies().isEmpty()) {
      NaturalLanguageStringStructure destinationDisplay = call.getDestinationDisplaies().get(0);
      tripTimes.setHeadsign(index, new NonLocalizedString(destinationDisplay.getValue()));
    }
  }
}
