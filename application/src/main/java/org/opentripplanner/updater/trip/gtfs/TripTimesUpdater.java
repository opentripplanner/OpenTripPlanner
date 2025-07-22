package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.transit.model.framework.Result.success;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripTimesPatch;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.gtfs.model.StopTimeUpdate;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TripTimesUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(TripTimesUpdater.class);

  /**
   * Maximum time in seconds since midnight for arrivals and departures
   */
  private static final long MAX_ARRIVAL_DEPARTURE_TIME = 48 * 60 * 60;

  /**
   * Apply the TripUpdate to the appropriate TripTimes from a Timetable. The existing TripTimes
   * must not be modified directly because they may be shared with the underlying
   * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
   * protective copying of this Timetable. It is not done in this update method to avoid repeatedly
   * cloning the same Timetable when several updates are applied to it at once. We assume here that
   * all trips in a timetable are from the same feed, which should always be the case.
   *
   * @param tripUpdate                    GTFS-RT trip update
   * @param timeZone                      time zone of trip update
   * @param updateServiceDate             service date of trip update
   * @param backwardsDelayPropagationType Defines when delays are propagated to previous stops and
   *                                      if these stops are given the NO_DATA flag
   * @return {@link Result < TripTimesPatch ,    UpdateError   >} contains either a new copy of updated
   * TripTimes after TripUpdate has been applied on TripTimes of trip with the id specified in the
   * trip descriptor of the TripUpdate and a list of stop indices that have been skipped with the
   * realtime update; or an error if something went wrong
   */
  public static Result<TripTimesPatch, UpdateError> createUpdatedTripTimesFromGTFSRT(
    Timetable timetable,
    TripUpdate tripUpdate,
    ZoneId timeZone,
    LocalDate updateServiceDate,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    var optionalTripId = tripUpdate.tripDescriptor().tripId();
    if (optionalTripId.isEmpty()) {
      // I don't think it should happen here as an empty trip id was already rejected in the adapter
      LOG.debug("TripDescriptor object has no TripId field");
      return Result.failure(UpdateError.noTripId(TRIP_NOT_FOUND));
    }
    var tripId = optionalTripId.get();

    var feedScopedTripId = new FeedScopedId(timetable.getPattern().getFeedId(), tripId);

    var tripTimes = timetable.getTripTimes(feedScopedTripId);
    if (tripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", tripId);
      return Result.failure(new UpdateError(feedScopedTripId, TRIP_NOT_FOUND_IN_PATTERN));
    } else {
      LOG.trace("tripId {} found in timetable.", tripId);
    }

    RealTimeTripTimesBuilder builder = tripTimes.createRealTimeWithoutScheduledTimes();
    tripUpdate.tripHeadsign().ifPresent(builder::withTripHeadsign);
    // TODO: add support for changing trip short name

    Map<Integer, PickDrop> updatedPickups = new HashMap<>();
    Map<Integer, PickDrop> updatedDropoffs = new HashMap<>();
    Map<Integer, String> replacedStopIndices = new HashMap<>();

    // The GTFS-RT reference specifies that StopTimeUpdates are sorted by stop_sequence.
    Iterator<StopTimeUpdate> updates = tripUpdate.stopTimeUpdates().iterator();
    StopTimeUpdate update = null;
    if (!updates.hasNext()) {
      LOG.warn("Zero-length trip update to trip {}.", tripId);
    } else {
      update = updates.next();
    }

    int numStops = tripTimes.getNumStops();

    final long today = ServiceDateUtils.asStartOfService(
      updateServiceDate,
      timeZone
    ).toEpochSecond();

    for (int i = 0; i < numStops; i++) {
      var index = i;
      boolean match = false;
      if (update != null) {
        if (update.stopSequence().isPresent()) {
          match = update.stopSequence().getAsInt() == tripTimes.gtfsSequenceOfStopIndex(i);
        } else if (update.stopId().isPresent()) {
          match = timetable.getPattern().getStop(i).getId().getId().equals(update.stopId().get());
        }
      }

      if (match) {
        update.stopHeadsign().ifPresent(x -> builder.withStopHeadsign(index, x));
        update.pickup().ifPresent(x -> updatedPickups.put(index, x));
        update.dropoff().ifPresent(x -> updatedDropoffs.put(index, x));
        update.assignedStopId().ifPresent(x -> replacedStopIndices.put(index, x));

        var scheduleRelationship = update.scheduleRelationship();
        // Handle each schedule relationship case
        if (scheduleRelationship == ScheduleRelationship.SKIPPED) {
          // Set status to cancelled
          updatedPickups.put(i, PickDrop.CANCELLED);
          updatedDropoffs.put(i, PickDrop.CANCELLED);
          builder.withCanceled(i);
        } else if (scheduleRelationship == ScheduleRelationship.NO_DATA) {
          // Set status to NO_DATA and delays to 0.
          // Note: GTFS-RT requires NO_DATA stops to have no arrival departure times.
          builder.withNoData(i);
        } else {
          // Else the status is SCHEDULED, update times as needed.
          if (!update.isArrivalValid()) {
            LOG.debug(
              "Arrival time at index {} of trip {} has neither a delay nor a time.",
              i,
              tripId
            );
            return Result.failure(new UpdateError(feedScopedTripId, INVALID_ARRIVAL_TIME, i));
          }
          if (!update.isDepartureValid()) {
            LOG.debug(
              "Departure time at index {} of trip {} has neither a delay nor a time.",
              i,
              tripId
            );
            return Result.failure(new UpdateError(feedScopedTripId, INVALID_DEPARTURE_TIME, i));
          }
          setArrivalAndDeparture(builder, i, update, today);
        }

        if (updates.hasNext()) {
          update = updates.next();
        } else {
          update = null;
        }
      }
    }
    if (update != null) {
      LOG.debug(
        "Part of a TripUpdate object could not be applied successfully to trip {}.",
        tripId
      );
      return Result.failure(new UpdateError(feedScopedTripId, INVALID_STOP_SEQUENCE));
    }

    // Interpolate missing times for stops which don't have times associated. Note: Currently for
    // GTFS-RT updates ONLY not for SIRI updates.
    if (
      ForwardsDelayInterpolator.getInstance(forwardsDelayPropagationType).interpolateDelay(builder)
    ) {
      LOG.debug("Interpolated delays for for missing stops on trip {}.", tripId);
    }

    var backwardPropagationIndex = BackwardsDelayInterpolator.getInstance(
      backwardsDelayPropagationType
    ).propagateBackwards(builder);
    backwardPropagationIndex.ifPresent(index ->
      LOG.debug("Propagated delay from stop index {} backwards on trip {}.", index, tripId)
    );

    getWheelchairAccessibility(tripUpdate).ifPresent(builder::withWheelchairAccessibility);

    // Make sure that updated trip times have the correct real time state
    builder.withRealTimeState(RealTimeState.UPDATED);

    // Validate for non-increasing times. Log error if present.
    try {
      var result = builder.build();
      LOG.trace(
        "A valid TripUpdate object was applied to trip {} using the Timetable class update method.",
        tripId
      );
      return success(
        new TripTimesPatch(result, updatedPickups, updatedDropoffs, replacedStopIndices)
      );
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e);
    }
  }

  /**
   * Add a new or replacement trip to the snapshot
   *
   * @param trip              trip
   * @param wheelchairAccessibility accessibility information of the vehicle
   * @param serviceDate       service date of trip
   * @param realTimeState     real-time state of new trip
   * @return empty Result if successful or one containing an error
   */
  public static Result<TripTimesWithStopPattern, UpdateError> createNewTripTimesFromGTFSRT(
    Trip trip,
    @Nullable Accessibility wheelchairAccessibility,
    List<StopAndStopTimeUpdate> stopAndStopTimeUpdates,
    ZoneId timeZone,
    LocalDate serviceDate,
    RealTimeState realTimeState,
    @Nullable I18NString tripHeadsign,
    Deduplicator deduplicator,
    int serviceCode
  ) {
    // Calculate seconds since epoch on GTFS midnight (noon minus 12h) of service date
    final long midnightSecondsSinceEpoch = ServiceDateUtils.asStartOfService(
      serviceDate,
      timeZone
    ).toEpochSecond();

    // Create StopTimes based on the scheduled times
    final List<StopTime> stopTimes = new ArrayList<>(stopAndStopTimeUpdates.size());
    var lastStopSequence = -1;
    for (final StopAndStopTimeUpdate item : stopAndStopTimeUpdates) {
      final var update = item.stopTimeUpdate();
      final var stop = item.stop();

      // validate stop sequence
      OptionalInt stopSequence = update.stopSequence();
      if (stopSequence.isPresent()) {
        var seq = stopSequence.getAsInt();
        if (seq < 0) {
          LOG.debug(
              "{} trip {} on {} contains negative stop sequence, skipping.",
            realTimeState,
            trip.getId(),
            serviceDate
          );
          return UpdateError.result(trip.getId(), INVALID_STOP_SEQUENCE);
        }
        if (seq <= lastStopSequence) {
          LOG.debug(
            "{} trip {} on {} contains decreasing stop sequence, skipping.",
            realTimeState,
            trip.getId(),
            serviceDate
          );
          return UpdateError.result(trip.getId(), INVALID_STOP_SEQUENCE);
        }
        lastStopSequence = seq;
      }

      // Create stop time
      final StopTime stopTime = new StopTime();
      stopTime.setTrip(trip);
      stopTime.setStop(stop);
      // Set arrival time
      final var arrival = update.scheduledArrivalTimeWithRealTimeFallback();
      if (arrival.isPresent()) {
        final var arrivalTime = arrival.getAsLong() - midnightSecondsSinceEpoch;
        if (arrivalTime < 0 || arrivalTime > MAX_ARRIVAL_DEPARTURE_TIME) {
          LOG.debug(
            "NEW trip {} on {} has invalid arrival time (compared to start date in " +
            "TripDescriptor), skipping.",
            trip.getId(),
            serviceDate
          );
          return UpdateError.result(trip.getId(), INVALID_ARRIVAL_TIME);
        }
        stopTime.setArrivalTime((int) arrivalTime);
      }
      // Set departure time
      final var departure = update.scheduledDepartureTimeWithRealTimeFallback();
      if (departure.isPresent()) {
        final long departureTime = departure.getAsLong() - midnightSecondsSinceEpoch;
        if (departureTime < 0 || departureTime > MAX_ARRIVAL_DEPARTURE_TIME) {
          LOG.debug(
            "NEW trip {} on {} has invalid departure time (compared to start date in " +
            "TripDescriptor), skipping.",
            trip.getId(),
            serviceDate
          );
          return UpdateError.result(trip.getId(), INVALID_DEPARTURE_TIME);
        }
        stopTime.setDepartureTime((int) departureTime);
      }
      stopTime.setTimepoint(1); // Exact time
      stopSequence.ifPresent(stopTime::setStopSequence);
      stopTime.setPickupType(update.effectivePickup());
      stopTime.setDropOffType(update.effectiveDropoff());
      update.stopHeadsign().ifPresent(stopTime::setStopHeadsign);
      // Add stop time to list
      stopTimes.add(stopTime);
    }

    // Create new trip times
    final RealTimeTripTimesBuilder builder = TripTimesFactory.tripTimes(
      trip,
      stopTimes,
      deduplicator
    ).createRealTimeFromScheduledTimes();
    if (tripHeadsign != null) {
      builder.withTripHeadsign(tripHeadsign);
    }

    // Update all times to mark trip times as realtime
    for (int stopIndex = 0; stopIndex < builder.numberOfStops(); stopIndex++) {
      final var addedStopTime = stopAndStopTimeUpdates.get(stopIndex).stopTimeUpdate();

      if (addedStopTime.isSkipped()) {
        builder.withCanceled(stopIndex);
      }

      setArrivalAndDeparture(builder, stopIndex, addedStopTime, midnightSecondsSinceEpoch);
      if (builder.getArrivalTime(stopIndex) == null) {
        builder.withArrivalDelay(stopIndex, 0);
      }
      if (builder.getDepartureTime(stopIndex) == null) {
        builder.withDepartureDelay(stopIndex, 0);
      }
    }

    // Set service code of new trip times
    builder.withServiceCode(serviceCode);

    // Make sure that updated trip times have the correct real time state
    builder.withRealTimeState(realTimeState);

    if (wheelchairAccessibility != null) {
      builder.withWheelchairAccessibility(wheelchairAccessibility);
    }

    RealTimeTripTimes tripTimes = builder.build();

    // Add new trip times to the buffer
    return success(new TripTimesWithStopPattern(tripTimes, new StopPattern(stopTimes)));
  }

  private static void setArrivalAndDeparture(
    RealTimeTripTimesBuilder builder,
    int stopPositionInPattern,
    StopTimeUpdate update,
    long midnightSecondsSinceEpoch
  ) {
    var arrivalTime = update.arrivalTime();
    var departureTime = update.departureTime();
    var arrivalDelay = update.arrivalDelay();
    var departureDelay = update.departureDelay();
    arrivalTime.ifPresentOrElse(
      time ->
        builder.withArrivalTime(stopPositionInPattern, (int) (time - midnightSecondsSinceEpoch)),
      () -> arrivalDelay.ifPresent(delay -> builder.withArrivalDelay(stopPositionInPattern, delay))
    );
    departureTime.ifPresentOrElse(
      time ->
        builder.withDepartureTime(stopPositionInPattern, (int) (time - midnightSecondsSinceEpoch)),
      () ->
        departureDelay.ifPresent(delay -> builder.withDepartureDelay(stopPositionInPattern, delay))
    );
  }

  static Optional<Accessibility> getWheelchairAccessibility(TripUpdate tripUpdate) {
    return tripUpdate
      .vehicle()
      .flatMap(vehicleDescriptor ->
        vehicleDescriptor.hasWheelchairAccessible()
          ? GtfsRealtimeMapper.mapWheelchairAccessible(vehicleDescriptor.getWheelchairAccessible())
          : Optional.empty()
      );
  }
}
