package org.opentripplanner.updater.trip.gtfs.model;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.trip.gtfs.model.GtfsRealtimeMapper.mapWheelchairAccessible;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateSuccess;

/**
 * A real-time update for trip, which may contain updated stop times and trip properties.
 * Instances of this class are validated and ready for further processing.
 */
public final class TripUpdate {

  private final String feedId;
  private final com.google.transit.realtime.GtfsRealtime.TripUpdate tripUpdate;
  private final TripDescriptor tripDescriptor;
  private final Supplier<LocalDate> localDateNow;
  private LocalDate serviceDate;

  public TripUpdate(
    String feedId,
    GtfsRealtime.TripUpdate tripUpdate,
    Supplier<LocalDate> localDateNow
  ) {
    this.feedId = feedId;
    this.tripUpdate = tripUpdate;
    this.tripDescriptor = new TripDescriptor(tripUpdate.getTrip());
    this.localDateNow = localDateNow;
  }

  public TripDescriptor descriptor() {
    return tripDescriptor;
  }

  public List<StopTimeUpdate> stopTimeUpdates() {
    return tripUpdate
      .getStopTimeUpdateList()
      .stream()
      .map(StopTimeUpdate::new)
      .collect(Collectors.toList());
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

  public LocalDate serviceDate() {
    if (serviceDate != null) {
      return serviceDate;
    }
    try {
      // TODO: figure out the correct service date. For the special case that a trip
      // starts for example at 40:00, yesterday would probably be a better guess.
      serviceDate = tripDescriptor.startDate().orElse(localDateNow.get());
      return serviceDate;
    } catch (ParseException e) {
      throw new RuntimeException(
        "TripDescription does not have a valid startDate: call validate() first."
      );
    }
  }

  public ScheduleRelationship scheduleRelationship() {
    return tripDescriptor.scheduleRelationship();
  }

  public FeedScopedId tripId() {
    return tripDescriptor
      .tripId()
      .map(id -> new FeedScopedId(feedId, id))
      // this should never happen because an empty trip id will lead to an exception in the
      // constructor.
      .orElseThrow(() ->
        new IllegalStateException(
          "Trip ID is missing from trip update. This indicates a programming error."
        )
      );
  }

  public Result<UpdateSuccess, UpdateError> validate() throws DataValidationException {
    if (tripDescriptor.tripId().isEmpty()) {
      return Result.failure(UpdateError.noTripId(INVALID_INPUT_STRUCTURE));
    }

    try {
      tripDescriptor.startDate();
    } catch (ParseException e) {
      return Result.failure(new UpdateError(tripId(), INVALID_INPUT_STRUCTURE));
    }

    var lastStopSequence = -1;
    for (StopTimeUpdate update : stopTimeUpdates()) {
      // validate stop sequence
      OptionalInt stopSequence = update.stopSequence();
      if (stopSequence.isPresent()) {
        var seq = stopSequence.getAsInt();
        if (seq < 0) {
          return UpdateError.result(tripId(), INVALID_STOP_SEQUENCE);
        }
        if (seq <= lastStopSequence) {
          return UpdateError.result(tripId(), INVALID_STOP_SEQUENCE);
        }
        lastStopSequence = seq;
      }
    }
    return Result.success(UpdateSuccess.noWarnings());
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
}
