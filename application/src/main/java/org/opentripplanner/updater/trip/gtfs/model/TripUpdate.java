package org.opentripplanner.updater.trip.gtfs.model;

import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.updater.spi.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateErrorType.TOO_MANY_STOPS;
import static org.opentripplanner.updater.trip.gtfs.model.GtfsRealtimeMapper.mapWheelchairAccessible;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Supplier;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * A real-time update for trip, which may contain updated stop times and trip properties. Instances
 * of this class are validated and ready for further processing.
 */
public final class TripUpdate {

  /**
   * The maximum number of stops in a {@code REPLACEMENT} trip update. A replacement is matched
   * against the scheduled trip it replaces, which takes time and memory proportional to the number
   * of stops in both trips. Real trips should be far below this limit, so a larger update is
   * rejected to protect the updater from malformed feeds.
   */
  public static final int MAX_REPLACEMENT_STOPS = 500;

  /**
   * Maximum time in seconds since the start of the service day for arrivals and departures of new
   * trips.
   */
  private static final long MAX_ARRIVAL_DEPARTURE_TIME = 48 * 60 * 60;

  private final String feedId;
  private final GtfsRealtime.TripUpdate tripUpdate;
  private final TripDescriptor tripDescriptor;
  private final List<StopTimeUpdate> stopTimeUpdates;
  private final Supplier<LocalDate> localDateNow;
  private final ZoneId timeZone;
  private LocalDate startDate;

  public TripUpdate(
    String feedId,
    GtfsRealtime.TripUpdate tripUpdate,
    Supplier<LocalDate> localDateNow,
    ZoneId timeZone
  ) {
    this.feedId = feedId;
    this.tripUpdate = tripUpdate;
    this.tripDescriptor = new TripDescriptor(tripUpdate.getTrip());
    this.stopTimeUpdates = tripUpdate
      .getStopTimeUpdateList()
      .stream()
      .map(StopTimeUpdate::new)
      .toList();
    this.localDateNow = localDateNow;
    this.timeZone = timeZone;
  }

  public TripDescriptor descriptor() {
    return tripDescriptor;
  }

  public List<StopTimeUpdate> stopTimeUpdates() {
    return stopTimeUpdates;
  }

  public Optional<I18NString> tripHeadsign() {
    return tripProperties()
      .filter(p -> p.hasTripHeadsign())
      .map(p -> I18NString.of(p.getTripHeadsign()));
  }

  public Optional<String> tripShortName() {
    return tripProperties()
      .filter(p -> p.hasTripShortName())
      .map(p -> p.getTripShortName());
  }

  public Optional<Accessibility> wheelchairAccessibility() {
    return vehicle()
      .filter(d -> d.hasWheelchairAccessible())
      .flatMap(vehicleDescriptor ->
        mapWheelchairAccessible(vehicleDescriptor.getWheelchairAccessible())
      );
  }

  public LocalDate startDate() {
    if (startDate != null) {
      return startDate;
    }
    // TODO: figure out the correct service date. For the special case that a trip
    // starts for example at 40:00, yesterday would probably be a better guess.
    startDate = tripDescriptor.startDate().orElse(localDateNow.get());
    return startDate;
  }

  public ScheduleRelationship scheduleRelationship() {
    return tripDescriptor.scheduleRelationship();
  }

  public FeedScopedId tripId() {
    return (
      tripDescriptor
        .tripId()
        .map(id -> new FeedScopedId(feedId, id))
        // this should never happen because an empty trip id will lead to an exception in the
        // constructor.
        .orElseThrow(() ->
          new IllegalStateException(
            "Trip ID is missing from trip update. This indicates a programming error."
          )
        )
    );
  }

  /**
   * Validate the structure and content of the update. All checks that depend only on the message
   * itself (not on the state of the timetable or the static data) belong here.
   */
  public void validate() throws DataValidationException, UpdateException {
    if (tripDescriptor.tripId().isEmpty()) {
      throw UpdateException.noTripId(INVALID_INPUT_STRUCTURE);
    }

    // exercise the getter, would throw an UpdateException if start date is malformed
    tripDescriptor.startDate();

    validateStopSequences();

    switch (scheduleRelationship()) {
      case SCHEDULED -> validateScheduled();
      case REPLACEMENT -> validateReplacement();
      case NEW, ADDED -> validateNewTrip();
      case DUPLICATED -> validateDuplicated();
      default -> {
      }
    }
  }

  private void validateStopSequences() {
    var lastStopSequence = -1;
    for (int i = 0; i < stopTimeUpdates.size(); i++) {
      OptionalInt stopSequence = stopTimeUpdates.get(i).stopSequence();
      if (stopSequence.isPresent()) {
        var seq = stopSequence.getAsInt();
        if (seq < 0 || seq <= lastStopSequence) {
          throw UpdateException.of(tripId(), INVALID_STOP_SEQUENCE, i);
        }
        lastStopSequence = seq;
      }
    }
  }

  private void validateScheduled() {
    if (stopTimeUpdates.isEmpty()) {
      throw UpdateException.of(tripId(), NO_UPDATES);
    }
    for (int i = 0; i < stopTimeUpdates.size(); i++) {
      var update = stopTimeUpdates.get(i);
      if (update.isSkipped() || update.isNoData()) {
        continue;
      }
      // a provided arrival or departure must contain a time or a delay
      if (!update.isArrivalValid()) {
        throw UpdateException.of(tripId(), INVALID_ARRIVAL_TIME, i);
      }
      if (!update.isDepartureValid()) {
        throw UpdateException.of(tripId(), INVALID_DEPARTURE_TIME, i);
      }
    }
  }

  private void validateReplacement() {
    if (stopTimeUpdates.size() > MAX_REPLACEMENT_STOPS) {
      throw UpdateException.of(tripId(), TOO_MANY_STOPS);
    }
    validateNewTrip();
  }

  private void validateNewTrip() {
    if (stopTimeUpdates.size() < 2) {
      throw UpdateException.of(tripId(), TOO_FEW_STOPS);
    }
    final long startOfService = ServiceDateUtils.asStartOfService(
      startDate(),
      timeZone
    ).toEpochSecond();
    for (int i = 0; i < stopTimeUpdates.size(); i++) {
      var update = stopTimeUpdates.get(i);
      if (update.isNoData()) {
        // a NO_DATA stop of a new trip must only contain scheduled times
        if (!update.hasOnlyScheduledArrivalTime()) {
          throw UpdateException.of(tripId(), INVALID_ARRIVAL_TIME, i);
        }
        if (!update.hasOnlyScheduledDepartureTime()) {
          throw UpdateException.of(tripId(), INVALID_DEPARTURE_TIME, i);
        }
      }
      if (isOutOfRange(update.scheduledArrivalTimeWithRealTimeFallback(), startOfService)) {
        throw UpdateException.of(tripId(), INVALID_ARRIVAL_TIME, i);
      }
      if (isOutOfRange(update.scheduledDepartureTimeWithRealTimeFallback(), startOfService)) {
        throw UpdateException.of(tripId(), INVALID_DEPARTURE_TIME, i);
      }
    }
  }

  private static boolean isOutOfRange(OptionalLong epochSeconds, long startOfService) {
    if (epochSeconds.isEmpty()) {
      return false;
    }
    var time = epochSeconds.getAsLong() - startOfService;
    return time < 0 || time > MAX_ARRIVAL_DEPARTURE_TIME;
  }

  private void validateDuplicated() {
    if (tripDescriptor.startDate().isEmpty() || tripDescriptor.startTime().isEmpty()) {
      throw UpdateException.of(tripId(), INVALID_INPUT_STRUCTURE);
    }
  }

  public Optional<LocalTime> startTime() {
    return tripDescriptor.startTime();
  }

  public Optional<FeedScopedId> routeId() {
    return tripDescriptor.routeId().map(id -> new FeedScopedId(feedId, id));
  }

  private Optional<GtfsRealtime.TripUpdate.TripProperties> tripProperties() {
    return tripUpdate.hasTripProperties()
      ? Optional.of(tripUpdate.getTripProperties())
      : Optional.empty();
  }

  public Optional<GtfsRealtime.VehicleDescriptor> vehicle() {
    return tripUpdate.hasVehicle() ? Optional.of(tripUpdate.getVehicle()) : Optional.empty();
  }

  public Optional<FeedScopedId> vehicleId() {
    return vehicle()
      .filter(v -> StringUtils.hasValue(v.getId()))
      .map(v -> new FeedScopedId(feedId, v.getId()));
  }
}
