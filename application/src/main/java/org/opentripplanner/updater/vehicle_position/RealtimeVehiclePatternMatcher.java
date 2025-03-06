package org.opentripplanner.updater.vehicle_position;

import static org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig.VehiclePositionFeature.OCCUPANCY;
import static org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig.VehiclePositionFeature.POSITION;
import static org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig.VehiclePositionFeature.STOP_POSITION;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

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
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle.StopStatus;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for converting vehicle positions in memory to exportable ones, and associating each
 * position with a pattern.
 */
public class RealtimeVehiclePatternMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(RealtimeVehiclePatternMatcher.class);

  private final String feedId;
  private final RealtimeVehicleRepository repository;
  private final ZoneId timeZoneId;

  private final Function<FeedScopedId, Trip> getTripForId;
  private final Function<Trip, TripPattern> getStaticPattern;
  private final BiFunction<Trip, LocalDate, TripPattern> getRealtimePattern;
  private final GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;
  private final Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures;

  private Set<TripPattern> patternsInPreviousUpdate = Set.of();

  public RealtimeVehiclePatternMatcher(
    String feedId,
    Function<FeedScopedId, Trip> getTripForId,
    Function<Trip, TripPattern> getStaticPattern,
    BiFunction<Trip, LocalDate, TripPattern> getRealtimePattern,
    RealtimeVehicleRepository repository,
    ZoneId timeZoneId,
    GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher,
    Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> vehiclePositionFeatures
  ) {
    this.feedId = feedId;
    this.getTripForId = getTripForId;
    this.getStaticPattern = getStaticPattern;
    this.getRealtimePattern = getRealtimePattern;
    this.repository = repository;
    this.timeZoneId = timeZoneId;
    this.fuzzyTripMatcher = fuzzyTripMatcher;
    this.vehiclePositionFeatures = vehiclePositionFeatures;
  }

  /**
   * Attempts to match each vehicle to a pattern, then adds each to a pattern
   *
   * @param vehiclePositions List of vehicle positions to match to patterns
   */
  public UpdateResult applyRealtimeVehicleUpdates(List<VehiclePosition> vehiclePositions) {
    var matchResults = vehiclePositions
      .stream()
      .map(vehiclePosition -> toRealtimeVehicle(feedId, vehiclePosition))
      .toList();

    // we take the list of vehicles and out of them create a Map<TripPattern, List<RealtimeVehicle>>
    // that map makes it very easy to update the vehicles in the service
    // it also enables the bookkeeping about which pattern previously had vehicles but no longer do
    // these need to be removed from the service as we assume that the vehicle has stopped
    var vehicles = matchResults
      .stream()
      .filter(Result::isSuccess)
      .map(Result::successValue)
      .collect(Collectors.groupingBy(PatternAndRealtimeVehicle::pattern))
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(Entry::getKey, e ->
          e.getValue().stream().map(PatternAndRealtimeVehicle::vehicle).collect(Collectors.toList())
        )
      );

    vehicles.forEach(repository::setRealtimeVehicles);
    Set<TripPattern> patternsInCurrentUpdate = vehicles.keySet();

    // if there was a vehicle in the previous update but not in the current one, we assume
    // that the pattern has no more vehicles.
    var toDelete = Sets.difference(patternsInPreviousUpdate, patternsInCurrentUpdate);
    toDelete.forEach(repository::clearRealtimeVehicles);
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
    ResultLogger.logUpdateResult(feedId, "gtfs-rt-vehicle-positions", updateResult);

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
    return Stream.of(yesterday, today, tomorrow)
      .flatMap(day -> {
        var startTime = ServiceDateUtils.toZonedDateTime(day, zoneId, start).toInstant();
        var endTime = ServiceDateUtils.toZonedDateTime(day, zoneId, end).toInstant();

        return Stream.of(Duration.between(startTime, now), Duration.between(endTime, now))
          .map(Duration::abs) // temporal "distances" can be positive and negative
          .map(duration -> new TemporalDistance(day, duration.toSeconds()));
      })
      .min(Comparator.comparingLong(TemporalDistance::distance))
      .map(TemporalDistance::date)
      .orElse(today);
  }

  /**
   * Converts GtfsRealtime vehicle position to the OTP RealtimeVehicle which can be used by
   * the API.
   *
   * @param stopIndexOfGtfsSequence A function that takes a GTFS stop_sequence and returns the index
   *                                of the stop in the trip.
   */
  private RealtimeVehicle mapRealtimeVehicle(
    VehiclePosition vehiclePosition,
    List<StopLocation> stopsOnVehicleTrip,
    Trip trip,
    Function<Integer, OptionalInt> stopIndexOfGtfsSequence
  ) {
    var newVehicle = RealtimeVehicle.builder();

    if (vehiclePositionFeatures.contains(POSITION) && vehiclePosition.hasPosition()) {
      var position = vehiclePosition.getPosition();
      newVehicle.withCoordinates(
        new WgsCoordinate(position.getLatitude(), position.getLongitude())
      );

      if (position.hasSpeed()) {
        newVehicle.withSpeed(position.getSpeed());
      }
      if (position.hasBearing()) {
        newVehicle.withHeading(position.getBearing());
      }
    }

    if (vehiclePosition.hasVehicle()) {
      var vehicle = vehiclePosition.getVehicle();
      var id = new FeedScopedId(feedId, vehicle.getId());
      newVehicle
        .withVehicleId(id)
        .withLabel(Optional.ofNullable(vehicle.getLabel()).orElse(vehicle.getLicensePlate()));
    }

    if (vehiclePosition.hasTimestamp()) {
      newVehicle.withTime(Instant.ofEpochSecond(vehiclePosition.getTimestamp()));
    }

    if (vehiclePositionFeatures.contains(STOP_POSITION)) {
      if (vehiclePosition.hasCurrentStatus()) {
        newVehicle.withStopStatus(stopStatusToModel(vehiclePosition.getCurrentStatus()));
      }

      // we prefer the to get the current stop from the stop_id
      if (vehiclePosition.hasStopId()) {
        var matchedStops = stopsOnVehicleTrip
          .stream()
          .filter(stop -> stop.getId().getId().equals(vehiclePosition.getStopId()))
          .toList();
        if (matchedStops.size() == 1) {
          newVehicle.withStop(matchedStops.get(0));
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
        stopIndexOfGtfsSequence
          .apply(vehiclePosition.getCurrentStopSequence())
          .ifPresent(stopIndex -> {
            if (validStopIndex(stopIndex, stopsOnVehicleTrip)) {
              var stop = stopsOnVehicleTrip.get(stopIndex);
              newVehicle.withStop(stop);
            }
          });
      }
    }

    newVehicle.withTrip(trip);

    if (vehiclePositionFeatures.contains(OCCUPANCY) && vehiclePosition.hasOccupancyStatus()) {
      newVehicle.withOccupancyStatus(occupancyStatusToModel(vehiclePosition.getOccupancyStatus()));
    }

    return newVehicle.build();
  }

  /**
   * Checks that the stop index can actually be found in the pattern.
   */
  private static boolean validStopIndex(int stopIndex, List<StopLocation> stopsOnVehicleTrip) {
    return stopIndex < stopsOnVehicleTrip.size() - 1;
  }

  private record TemporalDistance(LocalDate date, long distance) {}

  private static StopStatus stopStatusToModel(VehicleStopStatus currentStatus) {
    return switch (currentStatus) {
      case IN_TRANSIT_TO -> StopStatus.IN_TRANSIT_TO;
      case INCOMING_AT -> StopStatus.INCOMING_AT;
      case STOPPED_AT -> StopStatus.STOPPED_AT;
    };
  }

  private static OccupancyStatus occupancyStatusToModel(
    VehiclePosition.OccupancyStatus occupancyStatus
  ) {
    return switch (occupancyStatus) {
      case NO_DATA_AVAILABLE -> OccupancyStatus.NO_DATA_AVAILABLE;
      case EMPTY -> OccupancyStatus.EMPTY;
      case MANY_SEATS_AVAILABLE -> OccupancyStatus.MANY_SEATS_AVAILABLE;
      case FEW_SEATS_AVAILABLE -> OccupancyStatus.FEW_SEATS_AVAILABLE;
      case STANDING_ROOM_ONLY -> OccupancyStatus.STANDING_ROOM_ONLY;
      case CRUSHED_STANDING_ROOM_ONLY -> OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY;
      case FULL -> OccupancyStatus.FULL;
      case NOT_ACCEPTING_PASSENGERS -> OccupancyStatus.NOT_ACCEPTING_PASSENGERS;
      case NOT_BOARDABLE -> OccupancyStatus.NOT_ACCEPTING_PASSENGERS;
    };
  }

  private static String toString(VehiclePosition vehiclePosition) {
    try {
      return JsonFormat.printer().omittingInsignificantWhitespace().print(vehiclePosition);
    } catch (InvalidProtocolBufferException ignored) {
      return vehiclePosition.toString();
    }
  }

  private VehiclePosition fuzzilySetTrip(VehiclePosition vehiclePosition) {
    var trip = fuzzyTripMatcher.match(feedId, vehiclePosition.getTrip());
    return vehiclePosition.toBuilder().setTrip(trip).build();
  }

  private Result<PatternAndRealtimeVehicle, UpdateError> toRealtimeVehicle(
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

    var vehiclePositionWithTripId = fuzzyTripMatcher == null
      ? vehiclePosition
      : fuzzilySetTrip(vehiclePosition);

    var tripId = vehiclePositionWithTripId.getTrip().getTripId();

    if (StringUtils.hasNoValue(tripId)) {
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

    var serviceDate = Optional.of(vehiclePositionWithTripId.getTrip().getStartDate())
      .map(Strings::emptyToNull)
      .flatMap(ServiceDateUtils::parseStringToOptional)
      .orElseGet(() -> inferServiceDate(trip));

    var pattern = getRealtimePattern.apply(trip, serviceDate);
    if (pattern == null) {
      LOG.debug("Unable to match OTP pattern ID for vehicle position with trip ID {}", tripId);
      return UpdateError.result(scopedTripId, NO_SERVICE_ON_DATE);
    }

    // the trip times are only used for mapping the GTFS-RT stop_sequence back to a stop.
    // because new trips without trip times are created for realtime-updated ones, we explicitly
    // look at the static trips for the stop_sequence->stop mapping
    var staticTripTimes = getStaticPattern.apply(trip).getScheduledTimetable().getTripTimes(trip);
    if (staticTripTimes == null) {
      return UpdateError.result(scopedTripId, TRIP_NOT_FOUND_IN_PATTERN);
    }

    // Add position to pattern
    var newVehicle = mapRealtimeVehicle(
      vehiclePositionWithTripId,
      pattern.getStops(),
      trip,
      staticTripTimes::stopIndexOfGtfsSequence
    );

    return Result.success(new PatternAndRealtimeVehicle(pattern, newVehicle));
  }

  record PatternAndRealtimeVehicle(TripPattern pattern, RealtimeVehicle vehicle) {}
}
