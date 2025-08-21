package org.opentripplanner.updater.trip.gtfs.model;

import com.google.transit.realtime.GtfsRealtime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * A real-time update for trip, which may contain updated stop times and trip properties.
 */
public final class TripUpdate {

  private final com.google.transit.realtime.GtfsRealtime.TripUpdate tripUpdate;

  public TripUpdate(com.google.transit.realtime.GtfsRealtime.TripUpdate tripUpdate) {
    this.tripUpdate = tripUpdate;
  }

  public TripDescriptor tripDescriptor() {
    // this field is required, so no check is done
    return new TripDescriptor(tripUpdate.getTrip());
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
      .flatMap(p ->
        p.hasTripHeadsign() ? Optional.of(I18NString.of(p.getTripHeadsign())) : Optional.empty()
      );
  }

  public Optional<String> tripShortName() {
    return tripProperties()
      .flatMap(p -> p.hasTripShortName() ? Optional.of(p.getTripShortName()) : Optional.empty());
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
