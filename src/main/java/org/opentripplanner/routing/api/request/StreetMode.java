package org.opentripplanner.routing.api.request;

public enum StreetMode {
  /**
   * No street mode is set. This option is used if we do not want street routing at all in this part
   * of the search.
   */
  NOT_SET(true, true, true, false, false, false),
  /**
   * Walk only
   */
  WALK(true, true, true, true, false, false),
  /**
   * Bike only
   */
  BIKE(true, true, true, true, true, false),
  /**
   * Bike to a bike parking area, then walk the rest of the way.
   *
   * Direct mode and access mode only.
   */
  BIKE_TO_PARK(true, false, false, true, true, false),
  /**
   * Walk to a bike rental point, bike to a bike rental drop-off point, and walk the rest of the
   * way. This can include bike rental at fixed locations or free-floating services.
   */
  BIKE_RENTAL(true, true, true, true, true,false),
  /**
   * Walk to a scooter rental point, ride a scooter to a scooter rental drop-off point, and walk the
   * rest of the way. This can include scooter rental at fixed locations or free-floating services.
   */
  SCOOTER_RENTAL(true, true, true, true, true,false),
  /**
   * Car only
   *
   * Direct mode only.
   */
  CAR(true, false, false, false, false, true),
  /**
   * Start in the car, drive to a parking area, and walk the rest of the way.
   *
   * Direct mode and access mode only.
   */
  CAR_TO_PARK(true, false, false, true, false, true),
  /**
   * Walk to a pickup point along the road, drive to a drop-off point along the road,
   * and walk the rest of the way. This can include various taxi-services or kiss & ride.
   */
  CAR_PICKUP(true, false, true, true, false, true),
  /**
   * Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way.
   * This can include car rental at fixed locations or free-floating services.
   */
  CAR_RENTAL(true, true, true, true, false, true),

  /**
   * Encompasses all types of on-demand and flexible transportation.
   */
  FLEXIBLE(true, false, true, true, false, true);

  boolean access;

  boolean transfer;

  boolean egress;

  boolean includesWalking;

  boolean includesBiking;

  boolean includesDriving;

  StreetMode(
      boolean access,
      boolean transfer,
      boolean egress,
      boolean includesWalking,
      boolean includesBiking,
      boolean includesDriving
  ) {
    this.access = access;
    this.transfer = transfer;
    this.egress = egress;
    this.includesWalking = includesWalking;
    this.includesBiking = includesBiking;
    this.includesDriving = includesDriving;
  }

  public boolean includesWalking() {
    return includesWalking;
  }

  public boolean includesBiking() {
    return includesBiking;
  }

  public boolean includesDriving() {
    return includesDriving;
  }
}
