package org.opentripplanner.service.realtimevehicles.model;

import java.time.Instant;
import java.util.Optional;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Internal model of a realtime vehicle.
 */
public class RealtimeVehicle {

  private final FeedScopedId vehicleId;

  private final String label;

  private final WgsCoordinate coordinates;

  /**
   * Speed in meters per second
   */
  private final Double speed;
  /**
   * Bearing, in degrees, clockwise from North, i.e., 0 is North and 90 is East. This can be the
   * compass bearing, or the direction towards the next stop or intermediate location.
   */
  private final Double heading;

  /**
   * When the realtime vehicle was recorded
   */
  private final Instant time;

  /**
   * Status of the vehicle, ie. if approaching the next stop or if it is there already.
   */
  private final StopRelationship stop;

  private final Trip trip;

  /**
   * How full the vehicle is and is it still accepting passengers.
   */
  private final OccupancyStatus occupancyStatus;

  RealtimeVehicle(RealtimeVehicleBuilder builder) {
    var stopRelationship = Optional.ofNullable(builder.stop())
      .map(s -> new StopRelationship(s, builder.stopStatus()))
      .orElse(null);
    this.vehicleId = builder.vehicleId();
    this.label = builder.label();
    this.coordinates = builder.coordinates();
    this.speed = builder.speed();
    this.heading = builder.heading();
    this.time = builder.time();
    this.stop = stopRelationship;
    this.trip = builder.trip();
    this.occupancyStatus = builder.occupancyStatus();
  }

  public Optional<FeedScopedId> vehicleId() {
    return Optional.ofNullable(vehicleId);
  }

  public Optional<String> label() {
    return Optional.ofNullable(label);
  }

  public Optional<WgsCoordinate> coordinates() {
    return Optional.ofNullable(coordinates);
  }

  public Optional<Double> speed() {
    return Optional.ofNullable(speed);
  }

  public Optional<Double> heading() {
    return Optional.ofNullable(heading);
  }

  public Optional<Instant> time() {
    return Optional.ofNullable(time);
  }

  public Optional<StopRelationship> stop() {
    return Optional.ofNullable(stop);
  }

  public Trip trip() {
    return trip;
  }

  public Optional<OccupancyStatus> occupancyStatus() {
    return Optional.ofNullable(occupancyStatus);
  }

  public static RealtimeVehicleBuilder builder() {
    return new RealtimeVehicleBuilder();
  }

  public enum StopStatus {
    INCOMING_AT,
    STOPPED_AT,
    IN_TRANSIT_TO,
  }

  public record StopRelationship(StopLocation stop, StopStatus status) {}
}
