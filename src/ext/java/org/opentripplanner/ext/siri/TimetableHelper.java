package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.opentripplanner.ext.siri.mapper.OccupancyMapper;
import org.opentripplanner.ext.siri.mapper.PickDropMapper;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
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
  public static Result<TripTimesAndStopPattern, UpdateError> createUpdatedTripTimes(
    Timetable timetable,
    EstimatedVehicleJourney journey,
    FeedScopedId tripId,
    Function<FeedScopedId, StopLocation> getStopById,
    LocalDate serviceDate,
    ZoneId zoneId,
    Deduplicator deduplicator
  ) {
    final TripTimes existingTripTimes = timetable.getTripTimes(tripId);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", tripId);
      return UpdateError.result(tripId, TRIP_NOT_FOUND_IN_PATTERN);
    }

    TripTimes oldTimes = new TripTimes(existingTripTimes);

    if (journey.isCancellation() != null && journey.isCancellation()) {
      oldTimes.cancelTrip();
      return Result.success(
        new TripTimesAndStopPattern(oldTimes, timetable.getPattern().getStopPattern())
      );
    }

    boolean stopPatternChanged = false;

    TripPattern pattern = timetable.getPattern();
    List<CallWrapper> calls = CallWrapper.of(journey);
    List<StopTime> modifiedStopTimes = createModifiedStopTimes(
      pattern,
      oldTimes,
      calls,
      getStopById
    );
    TripTimes newTimes = new TripTimes(oldTimes.getTrip(), modifiedStopTimes, deduplicator);

    //Populate missing data from existing TripTimes
    newTimes.setServiceCode(oldTimes.getServiceCode());

    OccupancyEnumeration journeyOccupancy = journey.getOccupancy();

    int callCounter = 0;

    ZonedDateTime startOfService = ServiceDateUtils.asStartOfService(serviceDate, zoneId);
    Set<CallWrapper> alreadyVisited = new HashSet<>();

    boolean isJourneyPredictionInaccurate =
      (journey.isPredictionInaccurate() != null && journey.isPredictionInaccurate());

    int departureFromPreviousStop = 0;
    int lastArrivalDelay = 0;
    int lastDepartureDelay = 0;
    for (var stop : pattern.getStops()) {
      boolean foundMatch = false;

      for (CallWrapper call : calls) {
        if (alreadyVisited.contains(call)) {
          continue;
        }
        //Current stop is being updated
        foundMatch = stop.getId().getId().equals(call.getStopPointRef());

        if (!foundMatch && stop.isPartOfStation()) {
          var alternativeStop = getStopById.apply(
            new FeedScopedId(stop.getId().getFeedId(), call.getStopPointRef())
          );
          if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
            foundMatch = true;
            stopPatternChanged = true;
          }
        }

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
          break;
        }
      }
      if (!foundMatch) {
        if (pattern.isBoardAndAlightAt(callCounter, NONE)) {
          // When newTimes contains stops without pickup/dropoff - set both arrival/departure to previous stop's departure
          // This necessary to accommodate the case when delay is reduced/eliminated between to stops with pickup/dropoff, and
          // multiple non-pickup/dropoff stops are in between.
          newTimes.updateArrivalTime(callCounter, departureFromPreviousStop);
          newTimes.updateDepartureTime(callCounter, departureFromPreviousStop);
        } else {
          int arrivalDelay = lastArrivalDelay;
          int departureDelay = lastDepartureDelay;

          // TODO - there is something clearly wrong here
          if (lastArrivalDelay == 0 && lastDepartureDelay == 0) {
            //No match has been found yet (i.e. still in RecordedCalls) - keep existing delays
            arrivalDelay = existingTripTimes.getArrivalDelay(callCounter);
            departureDelay = existingTripTimes.getDepartureDelay(callCounter);
          }

          newTimes.updateArrivalDelay(callCounter, arrivalDelay);
          newTimes.updateDepartureDelay(callCounter, departureDelay);
        }

        departureFromPreviousStop = newTimes.getDepartureTime(callCounter);
      }
      callCounter++;
    }

    if (stopPatternChanged) {
      // This update modified stopPattern
      newTimes.setRealTimeState(RealTimeState.MODIFIED);
    } else {
      // This is the first update, and StopPattern has not been changed
      newTimes.setRealTimeState(RealTimeState.UPDATED);
    }

    if (TRUE.equals(journey.isCancellation()) || newTimes.isAllStopsCancelled()) {
      LOG.debug("Trip is cancelled");
      newTimes.cancelTrip();
    }

    var result = newTimes.validateNonIncreasingTimes();
    if (result.isFailure()) {
      var updateError = result.failureValue();
      LOG.info(
        "TripTimes are non-increasing after applying SIRI delay propagation - LineRef {}, TripId {}. Stop index {}",
        journey.getLineRef().getValue(),
        tripId,
        updateError.stopIndex()
      );
      return Result.failure(updateError);
    }

    if (newTimes.getNumStops() != pattern.numberOfStops()) {
      return UpdateError.result(tripId, TOO_FEW_STOPS);
    }

    LOG.debug("A valid TripUpdate object was applied using the Timetable class update method.");
    return Result.success(
      new TripTimesAndStopPattern(newTimes, new StopPattern(modifiedStopTimes))
    );
  }

  /**
   * Apply the SIRI ET to the appropriate TripTimes from this Timetable. Calculate new stoppattern
   * based on single stop cancellations
   *
   * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
   * with the id specified in the trip descriptor of the TripUpdate; null if something went wrong
   */
  public static List<StopTime> createModifiedStopTimes(
    TripPattern pattern,
    TripTimes oldTimes,
    List<CallWrapper> calls,
    Function<FeedScopedId, StopLocation> getStopForId
  ) {
    List<StopTime> modifiedStops = new ArrayList<>();

    Set<CallWrapper> alreadyVisited = new HashSet<>();
    // modify updated stop-times
    for (int i = 0; i < pattern.numberOfStops(); i++) {
      StopLocation stop = pattern.getStop(i);

      final StopTime stopTime = new StopTime();
      stopTime.setStop(stop);
      stopTime.setTrip(oldTimes.getTrip());
      stopTime.setStopSequence(i);
      stopTime.setDropOffType(pattern.getAlightType(i));
      stopTime.setPickupType(pattern.getBoardType(i));
      stopTime.setArrivalTime(oldTimes.getScheduledArrivalTime(i));
      stopTime.setDepartureTime(oldTimes.getScheduledDepartureTime(i));
      stopTime.setStopHeadsign(oldTimes.getHeadsign(i));
      stopTime.setHeadsignVias(oldTimes.getHeadsignVias(i));
      stopTime.setTimepoint(oldTimes.isTimepoint(i) ? 1 : 0);

      boolean foundMatch = false;
      for (CallWrapper call : calls) {
        if (alreadyVisited.contains(call)) {
          continue;
        }

        //Current stop is being updated
        String callStopRef = call.getStopPointRef();
        boolean stopsMatchById = stop.getId().getId().equals(callStopRef);

        if (!stopsMatchById && stop.isPartOfStation()) {
          var alternativeStop = getStopForId.apply(
            new FeedScopedId(stop.getId().getFeedId(), callStopRef)
          );
          if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
            stopsMatchById = true;
            stopTime.setStop(alternativeStop);
          }
        }

        if (stopsMatchById) {
          foundMatch = true;

          PickDropMapper.updatePickDrop(call, stopTime);

          if (call.getDestinationDisplaies() != null && !call.getDestinationDisplaies().isEmpty()) {
            NaturalLanguageStringStructure destinationDisplay = call
              .getDestinationDisplaies()
              .get(0);
            stopTime.setStopHeadsign(new NonLocalizedString(destinationDisplay.getValue()));
          }

          modifiedStops.add(stopTime);
          alreadyVisited.add(call);
          break;
        }
      }

      if (!foundMatch) {
        modifiedStops.add(stopTime);
      }
    }

    return modifiedStops;
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

    int arrivalTime = tripTimes.getArrivalTime(index);

    int realtimeArrivalTime = getAvailableTime(
      departureDate,
      call::getActualArrivalTime,
      call::getExpectedArrivalTime,
      call::getAimedArrivalTime
    );

    int departureTime = tripTimes.getDepartureTime(index);
    int realtimeDepartureTime = getAvailableTime(
      departureDate,
      call::getActualDepartureTime,
      call::getExpectedDepartureTime,
      call::getAimedDepartureTime
    );

    int[] possibleArrivalTimes = index == 0
      ? new int[] { realtimeArrivalTime, realtimeDepartureTime, arrivalTime }
      : new int[] { realtimeArrivalTime, arrivalTime };
    realtimeArrivalTime = handleMissingRealtime(possibleArrivalTimes);

    int[] possibleDepartureTimes = isLastStop
      ? new int[] { realtimeDepartureTime, realtimeArrivalTime, departureTime }
      : new int[] { realtimeDepartureTime, departureTime };
    realtimeDepartureTime = handleMissingRealtime(possibleDepartureTimes);

    OccupancyEnumeration callOccupancy = call.getOccupancy() != null
      ? call.getOccupancy()
      : journeyOccupancy;

    if (callOccupancy != null) {
      tripTimes.setOccupancyStatus(index, OccupancyMapper.mapOccupancyStatus(callOccupancy));
    }

    int arrivalDelay = realtimeArrivalTime - arrivalTime;
    tripTimes.updateArrivalDelay(index, arrivalDelay);

    int departureDelay = realtimeDepartureTime - departureTime;
    tripTimes.updateDepartureDelay(index, departureDelay);
  }
}
