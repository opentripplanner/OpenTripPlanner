package org.opentripplanner.routing.request;

public enum StreetMode {
  /**
   * Walk only
   */
  WALK(true, true),
  /**
   * Bike only
   *
   * Direct mode only. Bike on transit is not yet supported.
   */
  BIKE(false, false),
  /**
   * Bike to a bike parking area, then walk the rest of the way.
   *
   * Direct mode and access mode only.
   */
  // TODO OTP2 Only implemented for direct search
  BIKE_TO_PARK(true, false),
  /**
   * Walk to a bike rental point, bike to a bike rental drop-off point, and walk the rest of the
   * way. This can include bike rental at fixed locations or free-floating services.
   */
  // TODO OTP2 Only implemented for direct search
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
  // TODO OTP2 Only implemented for direct search
  CAR_TO_PARK(true, false),
  /**
   * Walk to a pickup point along the road, drive to a drop-off point along the road,
   * and walk the rest of the way. This can include various taxi-services or kiss & ride.
   */
  // TODO OTP2 Only implemented for direct search
  TAXI(true, true),
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
