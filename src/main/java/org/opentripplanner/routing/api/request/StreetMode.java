package org.opentripplanner.routing.api.request;

public enum StreetMode {
  /**
   * Walk only
   */
  WALK(true, true),
  /**
   * Bike only
   *
   * This can be used as access/egress, but transfers will still be walk only.
   * // TODO OTP2 Implement bicycle transfers
   */
  BIKE(true, true),
  /**
   * Bike to a bike parking area, then walk the rest of the way.
   *
   * Direct mode and access mode only.
   */
  BIKE_TO_PARK(true, false),
  /**
   * Walk to a bike rental point, bike to a bike rental drop-off point, and walk the rest of the
   * way. This can include bike rental at fixed locations or free-floating services.
   */
  BIKE_RENTAL(true, true),
  /**
   * Car only
   *
   * Direct mode only.
   */
  CAR(false, false),
  /**
   * Start in the car, drive to a parking area, and walk the rest of the way.
   *
   * Direct mode and access mode only.
   */
  CAR_TO_PARK(true, false),
  /**
   * Walk to a pickup point along the road, drive to a drop-off point along the road,
   * and walk the rest of the way. This can include various taxi-services or kiss & ride.
   */
  CAR_PICKUP(true, true),
  /**
   * Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way.
   * This can include car rental at fixed locations or free-floating services.
   */
  // TODO OTP2 Not implemented
  CAR_RENTAL(true, true);

  boolean access;

  boolean egress;

  StreetMode(boolean access, boolean egress) {
    this.access = access;
    this.egress = egress;
  }
}
