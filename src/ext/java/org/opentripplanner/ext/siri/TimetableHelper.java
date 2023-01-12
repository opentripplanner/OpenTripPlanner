package org.opentripplanner.ext.siri;

import static org.opentripplanner.model.PickDrop.CANCELLED;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.PickDrop.SCHEDULED;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.UNKNOWN;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.CallStatusEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.MonitoredCallStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.OccupancyEnumeration;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.VehicleActivityStructure;

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
  public static Result<TripTimes, UpdateError> createUpdatedTripTimes(
    Timetable timetable,
    EstimatedVehicleJourney journey,
    FeedScopedId tripId,
    Function<FeedScopedId, StopLocation> getStopById,
    ZoneId zoneId,
    Deduplicator deduplicator
  ) {
    if (journey == null) {
      return null;
    }

    final TripTimes existingTripTimes = timetable.getTripTimes(tripId);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", tripId);
      return UpdateError.result(tripId, TRIP_NOT_FOUND_IN_PATTERN);
    }

    TripTimes oldTimes = new TripTimes(existingTripTimes);

    if (journey.isCancellation() != null && journey.isCancellation()) {
      oldTimes.cancelTrip();
      return Result.success(oldTimes);
    }

    List<EstimatedCall> estimatedCalls = getEstimatedCalls(journey);
    List<RecordedCall> recordedCalls = getRecordedCalls(journey);

    EstimatedCall lastEstimatedCall = estimatedCalls.isEmpty()
      ? null
      : estimatedCalls.get(estimatedCalls.size() - 1);

    RecordedCall lastRecordedCall = recordedCalls.isEmpty()
      ? null
      : recordedCalls.get(recordedCalls.size() - 1);

    boolean stopPatternChanged = false;

    TripPattern pattern = timetable.getPattern();
    List<StopTime> modifiedStopTimes = createModifiedStopTimes(
      pattern,
      oldTimes,
      journey,
      getStopById
    );
    if (modifiedStopTimes == null) {
      return UpdateError.result(tripId, UNKNOWN);
    }
    TripTimes newTimes = new TripTimes(oldTimes.getTrip(), modifiedStopTimes, deduplicator);

    //Populate missing data from existing TripTimes
    newTimes.setServiceCode(oldTimes.getServiceCode());

    OccupancyEnumeration journeyOccupancy = journey.getOccupancy();

    int callCounter = 0;

    LocalDate serviceDate = getServiceDate(journey, zoneId, oldTimes);
    ZonedDateTime startOfService = ServiceDateUtils.asStartOfService(serviceDate, zoneId);
    Set<Object> alreadyVisited = new HashSet<>();

    boolean isJourneyPredictionInaccurate =
      (journey.isPredictionInaccurate() != null && journey.isPredictionInaccurate());

    int departureFromPreviousStop = 0;
    int lastArrivalDelay = 0;
    int lastDepartureDelay = 0;
    for (var stop : pattern.getStops()) {
      boolean foundMatch = false;

      for (RecordedCall recordedCall : recordedCalls) {
        if (alreadyVisited.contains(recordedCall)) {
          continue;
        }
        //Current stop is being updated
        foundMatch = stop.getId().getId().equals(recordedCall.getStopPointRef().getValue());

        if (!foundMatch && stop.isPartOfStation()) {
          var alternativeStop = getStopById.apply(
            new FeedScopedId(stop.getId().getFeedId(), recordedCall.getStopPointRef().getValue())
          );
          if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
            foundMatch = true;
            stopPatternChanged = true;
          }
        }

        if (foundMatch) {
          applyUpdates(
            startOfService,
            modifiedStopTimes,
            newTimes,
            callCounter,
            isJourneyPredictionInaccurate,
            recordedCall,
            journeyOccupancy
          );

          alreadyVisited.add(recordedCall);
          break;
        }
      }
      if (!foundMatch) {
        for (EstimatedCall estimatedCall : estimatedCalls) {
          if (alreadyVisited.contains(estimatedCall)) {
            continue;
          }
          //Current stop is being updated
          foundMatch = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

          if (!foundMatch && stop.isPartOfStation()) {
            var alternativeStop = getStopById.apply(
              new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue())
            );
            if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
              foundMatch = true;
              stopPatternChanged = true;
            }
          }

          if (foundMatch) {
            applyUpdates(
              startOfService,
              modifiedStopTimes,
              newTimes,
              callCounter,
              isJourneyPredictionInaccurate,
              estimatedCall,
              journeyOccupancy
            );

            alreadyVisited.add(estimatedCall);
            break;
          }
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

    if (journey.isCancellation() != null && journey.isCancellation()) {
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
    return Result.success(newTimes);
  }

  private static int calculateDayOffset(TripTimes oldTimes) {
    if (oldTimes.getDepartureTime(0) > 86400) {
      // The "departure-date" for this trip is set to "yesterday" (or before) even though it actually departs "today"

      return oldTimes.getDepartureTime(0) / 86400; // calculate number of offset-days
    } else {
      return 0;
    }
  }

  /**
   * Maps the (very limited) SIRI 2.0 OccupancyEnum to internal OccupancyStatus
   * @param occupancy
   * @return
   */
  private static OccupancyStatus resolveOccupancyStatus(OccupancyEnumeration occupancy) {
    if (occupancy != null) {
      return switch (occupancy) {
        case SEATS_AVAILABLE -> OccupancyStatus.MANY_SEATS_AVAILABLE;
        case STANDING_AVAILABLE -> OccupancyStatus.STANDING_ROOM_ONLY;
        case FULL -> OccupancyStatus.FULL;
      };
    }
    return OccupancyStatus.NO_DATA;
  }

  /**
   * Apply the SIRI ET to the appropriate TripTimes from this Timetable. Calculate new stoppattern
   * based on single stop cancellations
   *
   * @param journey SIRI-ET EstimatedVehicleJourney
   * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
   * with the id specified in the trip descriptor of the TripUpdate; null if something went wrong
   */
  public static List<StopLocation> createModifiedStops(
    TripPattern pattern,
    EstimatedVehicleJourney journey,
    Function<FeedScopedId, StopLocation> getStopForId
  ) {
    if (journey == null) {
      return null;
    }

    List<EstimatedCall> estimatedCalls = getEstimatedCalls(journey);
    List<RecordedCall> recordedCalls = getRecordedCalls(journey);

    // Keeping track of visited stop-objects to allow multiple visits to a stop.
    List<Object> alreadyVisited = new ArrayList<>();

    List<StopLocation> modifiedStops = new ArrayList<>();

    for (int i = 0; i < pattern.numberOfStops(); i++) {
      StopLocation stop = pattern.getStop(i);

      boolean foundMatch = false;
      if (i < recordedCalls.size()) {
        for (RecordedCall recordedCall : recordedCalls) {
          if (alreadyVisited.contains(recordedCall)) {
            continue;
          }
          //Current stop is being updated
          boolean stopsMatchById = stop
            .getId()
            .getId()
            .equals(recordedCall.getStopPointRef().getValue());

          if (!stopsMatchById && stop.isPartOfStation()) {
            var alternativeStop = getStopForId.apply(
              new FeedScopedId(stop.getId().getFeedId(), recordedCall.getStopPointRef().getValue())
            );
            if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
              stopsMatchById = true;
              stop = alternativeStop;
            }
          }

          if (stopsMatchById) {
            foundMatch = true;
            modifiedStops.add(stop);
            alreadyVisited.add(recordedCall);
            break;
          }
        }
      } else {
        for (EstimatedCall estimatedCall : estimatedCalls) {
          if (alreadyVisited.contains(estimatedCall)) {
            continue;
          }
          //Current stop is being updated
          boolean stopsMatchById = stop
            .getId()
            .getId()
            .equals(estimatedCall.getStopPointRef().getValue());

          if (!stopsMatchById && stop.isPartOfStation()) {
            var alternativeStop = getStopForId.apply(
              new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue())
            );
            if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
              stopsMatchById = true;
              stop = alternativeStop;
            }
          }

          if (stopsMatchById) {
            foundMatch = true;
            modifiedStops.add(stop);
            alreadyVisited.add(estimatedCall);
            break;
          }
        }
      }
      if (!foundMatch) {
        modifiedStops.add(stop);
      }
    }

    return modifiedStops;
  }

  /**
   * Apply the SIRI ET to the appropriate TripTimes from this Timetable. Calculate new stoppattern
   * based on single stop cancellations
   *
   * @param journey SIRI-ET EstimatedVehicleJourney
   * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
   * with the id specified in the trip descriptor of the TripUpdate; null if something went wrong
   */
  public static List<StopTime> createModifiedStopTimes(
    TripPattern pattern,
    TripTimes oldTimes,
    EstimatedVehicleJourney journey,
    Function<FeedScopedId, StopLocation> getStopForId
  ) {
    if (journey == null) {
      return null;
    }

    List<EstimatedCall> estimatedCalls = getEstimatedCalls(journey);
    List<RecordedCall> recordedCalls = getRecordedCalls(journey);

    var stops = createModifiedStops(pattern, journey, getStopForId);

    List<StopTime> modifiedStops = new ArrayList<>();

    int numberOfRecordedCalls = recordedCalls.size();
    Set<Object> alreadyVisited = new HashSet<>();
    // modify updated stop-times
    for (int i = 0; i < stops.size(); i++) {
      StopLocation stop = stops.get(i);

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

      // TODO: Do we need to set the StopTime.id?
      //stopTime.setId(oldTimes.getStopTimeIdByIndex(i));

      boolean foundMatch = false;
      if (i < numberOfRecordedCalls) {
        for (RecordedCall recordedCall : recordedCalls) {
          if (alreadyVisited.contains(recordedCall)) {
            continue;
          }

          //Current stop is being updated
          var callStopRef = recordedCall.getStopPointRef().getValue();
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

            if (recordedCall.isCancellation() != null && recordedCall.isCancellation()) {
              stopTime.cancel();
            }

            modifiedStops.add(stopTime);
            alreadyVisited.add(recordedCall);
            break;
          }
        }
      } else {
        for (EstimatedCall estimatedCall : estimatedCalls) {
          if (alreadyVisited.contains(estimatedCall)) {
            continue;
          }

          //Current stop is being updated
          boolean stopsMatchById = stop
            .getId()
            .getId()
            .equals(estimatedCall.getStopPointRef().getValue());

          if (!stopsMatchById && stop.isPartOfStation()) {
            var alternativeStop = getStopForId.apply(
              new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue())
            );
            if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
              stopsMatchById = true;
              stopTime.setStop(alternativeStop);
            }
          }

          if (stopsMatchById) {
            foundMatch = true;

            CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
            if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
              stopTime.cancelDropOff();
            }
            var dropOffType = mapDropOffType(
              stopTime.getDropOffType(),
              estimatedCall.getArrivalBoardingActivity()
            );
            dropOffType.ifPresent(stopTime::setDropOffType);

            CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
            if (departureStatus == CallStatusEnumeration.CANCELLED) {
              stopTime.cancelPickup();
            }
            var pickUpType = mapPickUpType(
              stopTime.getPickupType(),
              estimatedCall.getDepartureBoardingActivity()
            );
            pickUpType.ifPresent(stopTime::setPickupType);

            if (estimatedCall.isCancellation() != null && estimatedCall.isCancellation()) {
              stopTime.cancel();
            }

            if (
              estimatedCall.getDestinationDisplaies() != null &&
              !estimatedCall.getDestinationDisplaies().isEmpty()
            ) {
              NaturalLanguageStringStructure destinationDisplay = estimatedCall
                .getDestinationDisplaies()
                .get(0);
              stopTime.setStopHeadsign(new NonLocalizedString(destinationDisplay.getValue()));
            }

            modifiedStops.add(stopTime);
            alreadyVisited.add(estimatedCall);
            break;
          }
        }
      }

      if (!foundMatch) {
        modifiedStops.add(stopTime);
      }
    }

    return modifiedStops;
  }

  /**
   * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
   * must not be modified directly because they may be shared with the underlying
   * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
   * protective copying of this Timetable. It is not done in this update method to avoid repeatedly
   * cloning the same Timetable when several updates are applied to it at once. We assume here that
   * all trips in a timetable are from the same feed, which should always be the case.
   *
   * @param activity SIRI-VM VehicleActivity
   * @return a Result with a copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
   * with the id specified in the trip descriptor of the TripUpdate; a failed Result if something went wrong
   */
  public static Result<TripTimes, UpdateError> createUpdatedTripTimes(
    Timetable timetable,
    VehicleActivityStructure activity,
    FeedScopedId tripId,
    Function<FeedScopedId, StopLocation> getStopById
  ) {
    if (activity == null) {
      return Result.failure(new UpdateError(tripId, INVALID_INPUT_STRUCTURE));
    }

    MonitoredVehicleJourneyStructure mvj = activity.getMonitoredVehicleJourney();

    final TripTimes existingTripTimes = timetable.getTripTimes(tripId);
    if (existingTripTimes == null) {
      LOG.trace("tripId {} not found in pattern.", tripId);
      return Result.failure(new UpdateError(tripId, TRIP_NOT_FOUND_IN_PATTERN));
    }

    TripTimes newTimes = new TripTimes(existingTripTimes);

    MonitoredCallStructure update = mvj.getMonitoredCall();
    if (update == null) {
      return Result.failure(new UpdateError(tripId, INVALID_INPUT_STRUCTURE));
    }

    MonitoredVehicleJourneyStructure monitoredVehicleJourney = activity.getMonitoredVehicleJourney();

    if (monitoredVehicleJourney != null) {
      Duration delay = monitoredVehicleJourney.getDelay();
      int updatedDelay = 0;
      if (delay != null) {
        updatedDelay = (int) delay.toSeconds();
      }

      MonitoredCallStructure monitoredCall = monitoredVehicleJourney.getMonitoredCall();
      if (monitoredCall != null && monitoredCall.getStopPointRef() != null) {
        boolean matchFound = false;

        int arrivalDelay = 0;
        int departureDelay = 0;
        var pattern = timetable.getPattern();

        for (int index = 0; index < newTimes.getNumStops(); ++index) {
          if (!matchFound) {
            // Delay is set on a single stop at a time. When match is found - propagate delay on all following stops
            final var stop = pattern.getStop(index);

            matchFound = stop.getId().getId().equals(monitoredCall.getStopPointRef().getValue());

            if (!matchFound && stop.isPartOfStation()) {
              FeedScopedId alternativeId = new FeedScopedId(
                stop.getId().getFeedId(),
                monitoredCall.getStopPointRef().getValue()
              );
              var alternativeStop = getStopById.apply(alternativeId);
              if (alternativeStop != null && alternativeStop.isPartOfStation()) {
                matchFound = stop.isPartOfSameStationAs(alternativeStop);
              }
            }

            if (matchFound) {
              arrivalDelay = departureDelay = updatedDelay;
            } else {
              /*
               * If updated delay is less than previously set delay, the existing delay needs to be adjusted to avoid
               * non-increasing times causing updates to be rejected. Will only affect historical data.
               */
              arrivalDelay = Math.min(existingTripTimes.getArrivalDelay(index), updatedDelay);
              departureDelay = Math.min(existingTripTimes.getDepartureDelay(index), updatedDelay);
            }
          }
          newTimes.updateArrivalDelay(index, arrivalDelay);
          newTimes.updateDepartureDelay(index, departureDelay);
        }
      }
    }

    var result = newTimes.validateNonIncreasingTimes();
    if (result.isFailure()) {
      var error = result.failureValue();
      LOG.info(
        "TripTimes are non-increasing after applying SIRI delay propagation - LineRef {}, TripId {}. Stop index {}",
        timetable.getPattern().getRoute().getId(),
        tripId,
        error.stopIndex()
      );
      return Result.failure(error);
    }

    //If state is already MODIFIED - keep existing state
    if (newTimes.getRealTimeState() != RealTimeState.MODIFIED) {
      // Make sure that updated trip times have the correct real time state
      newTimes.setRealTimeState(RealTimeState.UPDATED);
    }

    return Result.success(newTimes);
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
   * Get the list of recorded calls for a EstimatedVehicleJourney. Return an empty list if no
   * recorded calls exist.
   */
  private static List<RecordedCall> getRecordedCalls(EstimatedVehicleJourney journey) {
    EstimatedVehicleJourney.RecordedCalls journeyRecordedCalls = journey.getRecordedCalls();
    if (journeyRecordedCalls != null) {
      return journeyRecordedCalls.getRecordedCalls();
    } else {
      return List.of();
    }
  }

  /**
   * Get the list of estimated calls for a EstimatedVehicleJourney. Return an empty list if no
   * estimated calls exist.
   */
  private static List<EstimatedCall> getEstimatedCalls(EstimatedVehicleJourney journey) {
    EstimatedVehicleJourney.EstimatedCalls journeyEstimatedCalls = journey.getEstimatedCalls();
    if (journeyEstimatedCalls != null) {
      return journeyEstimatedCalls.getEstimatedCalls();
    } else {
      return List.of();
    }
  }

  /**
   * This method maps an ArrivalBoardingActivity to a pick drop type.
   *
   * The Siri ArrivalBoardingActivity includes less information than the pick drop type, therefore is it only
   * changed if routability has changed.
   *
   * @param currentValue The current pick drop value on a stopTime
   * @param arrivalBoardingActivityEnumeration The incoming boardingActivity to be mapped
   * @return Mapped PickDrop type, empty if routability is not changed.
   */
  public static Optional<PickDrop> mapDropOffType(
    PickDrop currentValue,
    ArrivalBoardingActivityEnumeration arrivalBoardingActivityEnumeration
  ) {
    if (arrivalBoardingActivityEnumeration == null) {
      return Optional.empty();
    }

    return switch (arrivalBoardingActivityEnumeration) {
      case ALIGHTING -> currentValue.isNotRoutable() ? Optional.of(SCHEDULED) : Optional.empty();
      case NO_ALIGHTING -> Optional.of(NONE);
      case PASS_THRU -> Optional.of(CANCELLED);
    };
  }

  /**
   * This method maps an departureBoardingActivity to a pick drop type.
   *
   * The Siri DepartureBoardingActivity includes less information than the planned data, therefore is it only
   * changed if routability has changed.
   *
   * @param currentValue The current pick drop value on a stopTime
   * @param departureBoardingActivityEnumeration The incoming departureBoardingActivityEnumeration to be mapped
   * @return Mapped PickDrop type, empty if routability is not changed.
   */
  public static Optional<PickDrop> mapPickUpType(
    PickDrop currentValue,
    DepartureBoardingActivityEnumeration departureBoardingActivityEnumeration
  ) {
    if (departureBoardingActivityEnumeration == null) {
      return Optional.empty();
    }

    return switch (departureBoardingActivityEnumeration) {
      case BOARDING -> currentValue.isNotRoutable() ? Optional.of(SCHEDULED) : Optional.empty();
      case NO_BOARDING -> Optional.of(NONE);
      case PASS_THRU -> Optional.of(CANCELLED);
    };
  }

  private static LocalDate getServiceDate(
    EstimatedVehicleJourney journey,
    ZoneId zoneId,
    TripTimes oldTimes
  ) {
    if (
      journey.getFramedVehicleJourneyRef() != null &&
      journey.getFramedVehicleJourneyRef().getDataFrameRef() != null
    ) {
      var dataFrame = journey.getFramedVehicleJourneyRef().getDataFrameRef();
      if (dataFrame != null) {
        try {
          return LocalDate.parse(dataFrame.getValue());
        } catch (DateTimeParseException ignored) {
          LOG.warn("Invalid dataFrame format: {}", dataFrame.getValue());
        }
      }
    }

    var recordedCalls = getRecordedCalls(journey);
    var estimatedCalls = getEstimatedCalls(journey);
    ZonedDateTime firstDeparture;
    if (recordedCalls.isEmpty()) {
      firstDeparture = estimatedCalls.get(0).getAimedDepartureTime();
    } else {
      firstDeparture = recordedCalls.get(0).getAimedDepartureTime();
    }

    return firstDeparture
      .minusDays(calculateDayOffset(oldTimes))
      .withZoneSameInstant(zoneId)
      .toLocalDate();
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

  /**
   * Function to check if an Object boolean is true,
   * this will return false if the value is null or false
   * @param value nullable boolean
   * @return a primitive boolean
   */
  public static boolean isTrue(Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  public static void applyUpdates(
    ZonedDateTime departureDate,
    List<StopTime> stopTimes,
    TripTimes tripTimes,
    int index,
    boolean isJourneyPredictionInaccurate,
    RecordedCall recordedCall,
    OccupancyEnumeration journeyOccupancy
  ) {
    if (recordedCall.getActualDepartureTime() != null) {
      //Flag as recorded
      tripTimes.setRecorded(index);
    }

    // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
    boolean isCallPredictionInaccurate = isTrue(recordedCall.isPredictionInaccurate());
    if (isJourneyPredictionInaccurate || isCallPredictionInaccurate) {
      tripTimes.setPredictionInaccurate(index);
    }

    if (isTrue(recordedCall.isCancellation())) {
      stopTimes.get(index).cancel();
      tripTimes.setCancelled(index);
    }

    int arrivalTime = tripTimes.getArrivalTime(index);
    if (recordedCall.getActualArrivalTime() != null) {
      //Flag as recorded
      tripTimes.setRecorded(index);
    }

    int realtimeArrivalTime = getAvailableTime(
      departureDate,
      recordedCall::getActualArrivalTime,
      recordedCall::getExpectedArrivalTime,
      recordedCall::getAimedArrivalTime
    );

    int departureTime = tripTimes.getDepartureTime(index);
    int realtimeDepartureTime = getAvailableTime(
      departureDate,
      recordedCall::getActualDepartureTime,
      recordedCall::getExpectedDepartureTime,
      recordedCall::getAimedDepartureTime
    );

    if (index == 0) {
      realtimeArrivalTime =
        handleMissingRealtime(realtimeArrivalTime, realtimeDepartureTime, arrivalTime);
    } else {
      realtimeArrivalTime = handleMissingRealtime(realtimeArrivalTime, arrivalTime);
    }

    if (index == (stopTimes.size() - 1)) {
      realtimeDepartureTime =
        handleMissingRealtime(realtimeDepartureTime, realtimeArrivalTime, departureTime);
    } else {
      realtimeDepartureTime = handleMissingRealtime(realtimeDepartureTime, departureTime);
    }

    OccupancyEnumeration callOccupancy = recordedCall.getOccupancy() != null
      ? recordedCall.getOccupancy()
      : journeyOccupancy;

    if (callOccupancy != null) {
      tripTimes.setOccupancyStatus(index, resolveOccupancyStatus(callOccupancy));
    }

    int arrivalDelay = realtimeArrivalTime - arrivalTime;
    tripTimes.updateArrivalDelay(index, arrivalDelay);

    int departureDelay = realtimeDepartureTime - departureTime;
    tripTimes.updateDepartureDelay(index, departureDelay);
  }

  public static void applyUpdates(
    ZonedDateTime departureDate,
    List<StopTime> stopTimes,
    TripTimes tripTimes,
    int index,
    boolean isJourneyPredictionInaccurate,
    EstimatedCall estimatedCall,
    OccupancyEnumeration journeyOccupancy
  ) {
    // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
    boolean isCallPredictionInaccurate = isTrue(estimatedCall.isPredictionInaccurate());
    if (isJourneyPredictionInaccurate || isCallPredictionInaccurate) {
      tripTimes.setPredictionInaccurate(index);
    }

    if (estimatedCall.isCancellation() != null && estimatedCall.isCancellation()) {
      stopTimes.get(index).cancel();
      tripTimes.setCancelled(index);
    }

    // Update dropoff-/pickuptype only if status is cancelled
    CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
    if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
      stopTimes.get(index).cancelDropOff();
    }

    CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
    if (departureStatus == CallStatusEnumeration.CANCELLED) {
      stopTimes.get(index).cancelPickup();
    }

    int arrivalTime = tripTimes.getArrivalTime(index);
    int realtimeArrivalTime = getAvailableTime(
      departureDate,
      estimatedCall::getExpectedArrivalTime,
      estimatedCall::getAimedArrivalTime
    );

    int departureTime = tripTimes.getDepartureTime(index);
    int realtimeDepartureTime = getAvailableTime(
      departureDate,
      estimatedCall::getExpectedDepartureTime,
      estimatedCall::getAimedDepartureTime
    );

    if (index == 0) {
      realtimeArrivalTime =
        handleMissingRealtime(realtimeArrivalTime, realtimeDepartureTime, arrivalTime);
    } else {
      realtimeArrivalTime = handleMissingRealtime(realtimeArrivalTime, arrivalTime);
    }

    if (index == (stopTimes.size() - 1)) {
      realtimeDepartureTime =
        handleMissingRealtime(realtimeDepartureTime, realtimeArrivalTime, departureTime);
    } else {
      realtimeDepartureTime = handleMissingRealtime(realtimeDepartureTime, departureTime);
    }

    OccupancyEnumeration callOccupancy = estimatedCall.getOccupancy() != null
      ? estimatedCall.getOccupancy()
      : journeyOccupancy;

    if (callOccupancy != null) {
      tripTimes.setOccupancyStatus(index, resolveOccupancyStatus(callOccupancy));
    }

    int arrivalDelay = realtimeArrivalTime - arrivalTime;
    tripTimes.updateArrivalDelay(index, arrivalDelay);

    int departureDelay = realtimeDepartureTime - departureTime;
    tripTimes.updateDepartureDelay(index, departureDelay);
  }
}
