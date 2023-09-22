package org.opentripplanner.service.realtimevehicles.model;

import java.time.Instant;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Internal model of a realtime vehicle.
 */
public record RealtimeVehicle(
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
   * When the realtime vehicle was recorded
   */
  Instant time,

  /**
   * Status of the vehicle, ie. if approaching the next stop or if it is there already.
   */
  StopRelationship stop,
  Trip trip,

  /**
   * How full the vehicle is and is it still accepting passengers.
   */
  OccupancyStatus occupancyStatus
) {
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
