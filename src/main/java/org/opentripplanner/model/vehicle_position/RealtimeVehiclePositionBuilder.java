package org.opentripplanner.model.vehicle_position;

import java.time.Instant;
import java.util.Optional;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition.StopRelationship;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition.StopStatus;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public class RealtimeVehiclePositionBuilder {

  private FeedScopedId vehicleId;
  private String label;
  private WgsCoordinate coordinates;
  private Double speed = null;
  private Double heading = null;
  private Instant time;
  private StopStatus stopStatus = StopStatus.IN_TRANSIT_TO;
  private StopLocation stop;
  private Trip trip;

  public RealtimeVehiclePositionBuilder setVehicleId(FeedScopedId vehicleId) {
    this.vehicleId = vehicleId;
    return this;
  }

  public RealtimeVehiclePositionBuilder setLabel(String label) {
    this.label = label;
    return this;
  }

  public RealtimeVehiclePositionBuilder setCoordinates(WgsCoordinate c) {
    this.coordinates = c;
    return this;
  }

  public RealtimeVehiclePositionBuilder setSpeed(double speed) {
    this.speed = speed;
    return this;
  }

  public RealtimeVehiclePositionBuilder setHeading(double heading) {
    this.heading = heading;
    return this;
  }

  public RealtimeVehiclePositionBuilder setTime(Instant time) {
    this.time = time;
    return this;
  }

  public RealtimeVehiclePositionBuilder setStopStatus(StopStatus stopStatus) {
    this.stopStatus = stopStatus;
    return this;
  }

  public RealtimeVehiclePositionBuilder setStop(StopLocation stop) {
    this.stop = stop;
    return this;
  }

  public RealtimeVehiclePositionBuilder setTrip(Trip trip) {
    this.trip = trip;
    return this;
  }

  public RealtimeVehiclePosition build() {
    var stop = Optional
      .ofNullable(this.stop)
      .map(s -> new StopRelationship(s, stopStatus))
      .orElse(null);
    return new RealtimeVehiclePosition(
      vehicleId,
      label,
      coordinates,
      speed,
      heading,
      time,
      stop,
      trip
    );
  }
}
