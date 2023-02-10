package org.opentripplanner.updater.vehicle_position;

import static org.opentripplanner.model.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.model.UpdateSuccess;
import org.opentripplanner.service.vehiclepositions.VehiclePositionRepository;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition.StopStatus;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.ResultLogger;
import org.opentripplanner.updater.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for converting vehicle positions in memory to exportable ones, and associating each
 * position with a pattern.
 */
public class VehiclePositionPatternMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(VehiclePositionPatternMatcher.class);

  private final String feedId;
  private final VehiclePositionRepository repository;
  private final ZoneId timeZoneId;

  private final Function<FeedScopedId, Trip> getTripForId;
  private final Function<Trip, TripPattern> getStaticPattern;
  private final BiFunction<Trip, LocalDate, TripPattern> getRealtimePattern;

  private Set<TripPattern> patternsInPreviousUpdate = Set.of();

  public VehiclePositionPatternMatcher(
    String feedId,
    Function<FeedScopedId, Trip> getTripForId,
    Function<Trip, TripPattern> getStaticPattern,
    BiFunction<Trip, LocalDate, TripPattern> getRealtimePattern,
    VehiclePositionRepository repository,
    ZoneId timeZoneId
  ) {
    this.feedId = feedId;
    this.getTripForId = getTripForId;
    this.getStaticPattern = getStaticPattern;
    this.getRealtimePattern = getRealtimePattern;
    this.repository = repository;
    this.timeZoneId = timeZoneId;
  }

  /**
   * Attempts to match each vehicle position to a pattern, then adds each to a pattern
   *
   * @param vehiclePositions List of vehicle positions to match to patterns
   */
  public UpdateResult applyVehiclePositionUpdates(List<VehiclePosition> vehiclePositions) {
    var matchResults = vehiclePositions
      .stream()
      .map(vehiclePosition -> toRealtimeVehiclePosition(feedId, vehiclePosition))
      .toList();

    // we take the list of positions and out of them create a Map<TripPattern, List<VehiclePosition>>
    // that map makes it very easy to update the positions in the service
    // it also enables the bookkeeping about which pattern previously had positions but no longer do
    // these need to be removed from the service as we assume that the vehicle has stopped
    var positions = matchResults
      .stream()
      .filter(Result::isSuccess)
      .map(Result::successValue)
      .collect(Collectors.groupingBy(PatternAndVehiclePosition::pattern))
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(
          Entry::getKey,
          e ->
            e
              .getValue()
              .stream()
              .map(PatternAndVehiclePosition::position)
              .collect(Collectors.toList())
        )
      );

    positions.forEach(repository::setVehiclePositions);
    Set<TripPattern> patternsInCurrentUpdate = positions.keySet();

    // if there was a position in the previous update but not in the current one, we assume
    // that the pattern has no more vehicle positions.
    var toDelete = Sets.difference(patternsInPreviousUpdate, patternsInCurrentUpdate);
    toDelete.forEach(repository::clearVehiclePositions);
    patternsInPreviousUpdate = patternsInCurrentUpdate;

    if (!vehiclePositions.isEmpty() && patternsInCurrentUpdate.isEmpty()) {
      LOG.error(
        "Could not match any vehicle positions for feedId '{}'. Are you sure that the updater is using the correct feedId?",
        feedId
      );
    }

    // need to convert the sucess to the correct type.
    var results = matchResults
      .stream()
      .map(e -> e.mapSuccess(ignored -> UpdateSuccess.noWarnings()))
      .toList();
    // needs to be put into a new list so the types are correct
    var updateResult = UpdateResult.ofResults(new ArrayList<>(results));
    ResultLogger.logUpdateResult(
      feedId,
      "vehicle-positions",
      vehiclePositions.size(),
      updateResult
    );

    return updateResult;
  }

  private LocalDate inferServiceDate(Trip trip) {
    var staticTripTimes = getStaticPattern.apply(trip).getScheduledTimetable().getTripTimes(trip);
    return inferServiceDate(staticTripTimes, timeZoneId, Instant.now());
  }

  /**
   * When a vehicle position doesn't state the service date of its trip then we need to infer it.
   * <p>
   * {@see https://github.com/opentripplanner/OpenTripPlanner/issues/4058}
   */
  protected static LocalDate inferServiceDate(
    TripTimes staticTripTimes,
    ZoneId zoneId,
    Instant now
  ) {
    var start = staticTripTimes.getScheduledDepartureTime(0);
    var end = staticTripTimes.getScheduledDepartureTime(staticTripTimes.getNumStops() - 1);

    var today = now.atZone(zoneId).toLocalDate();
    var yesterday = today.minusDays(1);
    var tomorrow = today.plusDays(1);

    // we compute the temporal "distance" to either the start or the end of the trip on either
    // yesterday, today or tomorrow. whichever one has the lowest "distance" to now is guessed to be
    // the service day of the undated vehicle position
    // if this is concerning to you, you should put a start_date in your feed.
    return Stream
      .of(yesterday, today, tomorrow)
      .flatMap(day -> {
        var startTime = ServiceDateUtils.toZonedDateTime(day, zoneId, start).toInstant();
        var endTime = ServiceDateUtils.toZonedDateTime(day, zoneId, end).toInstant();

        return Stream
          .of(Duration.between(startTime, now), Duration.between(endTime, now))
          .map(Duration::abs) // temporal "distances" can be positive and negative
          .map(duration -> new TemporalDistance(day, duration.toSeconds()));
      })
      .min(Comparator.comparingLong(TemporalDistance::distance))
      .map(TemporalDistance::date)
      .orElse(today);
  }

  /**
   * Converts GtfsRealtime vehicle position to the OTP RealtimeVehiclePosition which can be used by
   * the API.
   */
  private static RealtimeVehiclePosition mapVehiclePosition(
    VehiclePosition vehiclePosition,
    List<StopLocation> stopsOnVehicleTrip,
    Trip trip
  ) {
    var newPosition = RealtimeVehiclePosition.builder();

    if (vehiclePosition.hasPosition()) {
      var position = vehiclePosition.getPosition();
      newPosition.setCoordinates(
        new WgsCoordinate(position.getLatitude(), position.getLongitude())
      );

      if (position.hasSpeed()) {
        newPosition.setSpeed(position.getSpeed());
      }
      if (position.hasBearing()) {
        newPosition.setHeading(position.getBearing());
      }
    }

    if (vehiclePosition.hasVehicle()) {
      var vehicle = vehiclePosition.getVehicle();
      var id = new FeedScopedId(trip.getId().getFeedId(), vehicle.getId());
      newPosition
        .setVehicleId(id)
        .setLabel(Optional.ofNullable(vehicle.getLabel()).orElse(vehicle.getLicensePlate()));
    }

    if (vehiclePosition.hasTimestamp()) {
      newPosition.setTime(Instant.ofEpochSecond(vehiclePosition.getTimestamp()));
    }

    if (vehiclePosition.hasCurrentStatus()) {
      newPosition.setStopStatus(toModel(vehiclePosition.getCurrentStatus()));
    }

    // we prefer the to get the current stop from the stop_id
    if (vehiclePosition.hasStopId()) {
      var matchedStops = stopsOnVehicleTrip
        .stream()
        .filter(stop -> stop.getId().getId().equals(vehiclePosition.getStopId()))
        .toList();
      if (matchedStops.size() == 1) {
        newPosition.setStop(matchedStops.get(0));
      } else {
        LOG.warn(
          "Stop ID {} is not in trip {}. Not setting stopRelationship.",
          vehiclePosition.getStopId(),
          trip.getId()
        );
      }
    }
    // but if stop_id isn't there we try current_stop_sequence
    else if (vehiclePosition.hasCurrentStopSequence()) {
      var stop = stopsOnVehicleTrip.get(vehiclePosition.getCurrentStopSequence());
      newPosition.setStop(stop);
    }

    newPosition.setTrip(trip);

    return newPosition.build();
  }

  private record TemporalDistance(LocalDate date, long distance) {}

  private static StopStatus toModel(VehicleStopStatus currentStatus) {
    return switch (currentStatus) {
      case IN_TRANSIT_TO -> StopStatus.IN_TRANSIT_TO;
      case INCOMING_AT -> StopStatus.INCOMING_AT;
      case STOPPED_AT -> StopStatus.STOPPED_AT;
    };
  }

  private static String toString(VehiclePosition vehiclePosition) {
    try {
      return JsonFormat.printer().omittingInsignificantWhitespace().print(vehiclePosition);
    } catch (InvalidProtocolBufferException ignored) {
      return vehiclePosition.toString();
    }
  }

  private Result<PatternAndVehiclePosition, UpdateError> toRealtimeVehiclePosition(
    String feedId,
    VehiclePosition vehiclePosition
  ) {
    if (!vehiclePosition.hasTrip()) {
      LOG.debug(
        "Realtime vehicle positions {} has no trip ID. Ignoring.",
        toString(vehiclePosition)
      );
      return Result.failure(UpdateError.noTripId(INVALID_INPUT_STRUCTURE));
    }

    var tripId = vehiclePosition.getTrip().getTripId();

    if (!StringUtils.hasValue(tripId)) {
      return Result.failure(UpdateError.noTripId(UpdateError.UpdateErrorType.NO_TRIP_ID));
    }

    var scopedTripId = new FeedScopedId(feedId, tripId);
    var trip = getTripForId.apply(scopedTripId);
    if (trip == null) {
      LOG.debug(
        "Unable to find trip ID in feed '{}' for vehicle position with trip ID {}",
        feedId,
        tripId
      );
      return UpdateError.result(scopedTripId, TRIP_NOT_FOUND);
    }

    var serviceDate = Optional
      .of(vehiclePosition.getTrip().getStartDate())
      .map(Strings::emptyToNull)
      .flatMap(ServiceDateUtils::parseStringToOptional)
      .orElseGet(() -> inferServiceDate(trip));

    var pattern = getRealtimePattern.apply(trip, serviceDate);
    if (pattern == null) {
      LOG.debug("Unable to match OTP pattern ID for vehicle position with trip ID {}", tripId);
      return UpdateError.result(scopedTripId, NO_SERVICE_ON_DATE);
    }

    // Add position to pattern
    var newPosition = mapVehiclePosition(vehiclePosition, pattern.getStops(), trip);

    return Result.success(new PatternAndVehiclePosition(pattern, newPosition));
  }

  record PatternAndVehiclePosition(TripPattern pattern, RealtimeVehiclePosition position) {}
}
