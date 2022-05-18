package org.opentripplanner.model.vehicle_position;

import java.time.Instant;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.transit.model.basic.FeedScopedId;

/**
 * Internal model of a realtime vehicle position.
 */
public record RealtimeVehiclePosition(
  FeedScopedId vehicleId,
  String label,
  WgsCoordinate coordinates,
  /**
   * Speed in meters per second
   */
  Double speed,
  /**
   * Bearing, in degrees, clockwise from North, i.e., 0 is North and 90 is East. This can be the
   * compass bearing, or the direction towards the next stop or intermediate location.
   */
  Double heading,

  /**
   * When the realtime position was recorded
   */
  Instant time,

  /**
   * Status of the vehicle, ie. if approaching the next stop or if it is there already.
   */
  StopRelationship stop,
  Trip trip
) {
  public static RealtimeVehiclePositionBuilder builder() {
    return new RealtimeVehiclePositionBuilder();
  }

  public enum StopStatus {
    INCOMING_AT,
    STOPPED_AT,
    IN_TRANSIT_TO,
  }

  public record StopRelationship(StopLocation stop, StopStatus status) {}
}
