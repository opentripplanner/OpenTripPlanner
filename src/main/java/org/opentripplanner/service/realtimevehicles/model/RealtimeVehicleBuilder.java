package org.opentripplanner.service.realtimevehicles.model;

import java.time.Instant;
import java.util.Optional;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle.StopRelationship;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle.StopStatus;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;

public class RealtimeVehicleBuilder {

  private FeedScopedId vehicleId;
  private String label;
  private WgsCoordinate coordinates;
  private Double speed = null;
  private Double heading = null;
  private Instant time;
  private StopStatus stopStatus = StopStatus.IN_TRANSIT_TO;
  private StopLocation stop;
  private Trip trip;
  private OccupancyStatus occupancyStatus;

  public RealtimeVehicleBuilder withVehicleId(FeedScopedId vehicleId) {
    this.vehicleId = vehicleId;
    return this;
  }

  public RealtimeVehicleBuilder withLabel(String label) {
    this.label = label;
    return this;
  }

  public RealtimeVehicleBuilder withCoordinates(WgsCoordinate c) {
    this.coordinates = c;
    return this;
  }

  public RealtimeVehicleBuilder withSpeed(double speed) {
    this.speed = speed;
    return this;
  }

  public RealtimeVehicleBuilder withHeading(double heading) {
    this.heading = heading;
    return this;
  }

  public RealtimeVehicleBuilder withTime(Instant time) {
    this.time = time;
    return this;
  }

  public RealtimeVehicleBuilder withStopStatus(StopStatus stopStatus) {
    this.stopStatus = stopStatus;
    return this;
  }

  public RealtimeVehicleBuilder withStop(StopLocation stop) {
    this.stop = stop;
    return this;
  }

  public RealtimeVehicleBuilder withTrip(Trip trip) {
    this.trip = trip;
    return this;
  }

  public RealtimeVehicleBuilder withOccupancyStatus(OccupancyStatus occupancyStatus) {
    this.occupancyStatus = occupancyStatus;
    return this;
  }

  public RealtimeVehicle build() {
    var stop = Optional
      .ofNullable(this.stop)
      .map(s -> new StopRelationship(s, stopStatus))
      .orElse(null);
    return new RealtimeVehicle(
      vehicleId,
      label,
      coordinates,
      speed,
      heading,
      time,
      stop,
      trip,
      occupancyStatus
    );
  }
}
