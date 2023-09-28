package org.opentripplanner.service.realtimevehicles.model;

import java.time.Instant;
import org.opentripplanner.framework.geometry.WgsCoordinate;
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

  public FeedScopedId vehicleId() {
    return vehicleId;
  }

  public RealtimeVehicleBuilder withVehicleId(FeedScopedId vehicleId) {
    this.vehicleId = vehicleId;
    return this;
  }

  public String label() {
    return label;
  }

  public RealtimeVehicleBuilder withLabel(String label) {
    this.label = label;
    return this;
  }

  public WgsCoordinate coordinates() {
    return coordinates;
  }

  public RealtimeVehicleBuilder withCoordinates(WgsCoordinate c) {
    this.coordinates = c;
    return this;
  }

  public Double speed() {
    return speed;
  }

  public RealtimeVehicleBuilder withSpeed(double speed) {
    this.speed = speed;
    return this;
  }

  public Double heading() {
    return heading;
  }

  public RealtimeVehicleBuilder withHeading(double heading) {
    this.heading = heading;
    return this;
  }

  public Instant time() {
    return time;
  }

  public RealtimeVehicleBuilder withTime(Instant time) {
    this.time = time;
    return this;
  }

  public StopStatus stopStatus() {
    return stopStatus;
  }

  public RealtimeVehicleBuilder withStopStatus(StopStatus stopStatus) {
    this.stopStatus = stopStatus;
    return this;
  }

  public StopLocation stop() {
    return stop;
  }

  public RealtimeVehicleBuilder withStop(StopLocation stop) {
    this.stop = stop;
    return this;
  }

  public Trip trip() {
    return trip;
  }

  public RealtimeVehicleBuilder withTrip(Trip trip) {
    this.trip = trip;
    return this;
  }

  public OccupancyStatus occupancyStatus() {
    return occupancyStatus;
  }

  public RealtimeVehicleBuilder withOccupancyStatus(OccupancyStatus occupancyStatus) {
    this.occupancyStatus = occupancyStatus;
    return this;
  }

  public RealtimeVehicle build() {
    return new RealtimeVehicle(this);
  }
}
