package org.opentripplanner.routing.api.request;

public enum StreetMode {
  /**
   * No street mode is set. This option is used if we do not want street routing at all in this part
   * of the search.
   */
  NOT_SET(true, true, true, false, false, false, false, false, false),
  /**
   * Walk only
   */
  WALK(true, true, true, true, false, false, false, false, false),
  /**
   * Bike only
   */
  BIKE(true, true, true, false, true, false, false, false, false),
  /**
   * Bike to a bike parking area, then walk the rest of the way.
   * <p>
   * Direct mode and access mode only.
   */
  BIKE_TO_PARK(true, false, false, true, true, false, false, true, false),
  /**
   * Walk to a bike rental point, bike to a bike rental drop-off point, and walk the rest of the
   * way. This can include bike rental at fixed locations or free-floating services.
   */
  BIKE_RENTAL(true, true, true, true, true, false, true, false, false),
  /**
   * Walk to a scooter rental point, ride a scooter to a scooter rental drop-off point, and walk the
   * rest of the way. This can include scooter rental at fixed locations or free-floating services.
   */
  SCOOTER_RENTAL(true, true, true, true, true, false, true, false, false),
  /**
   * Car only
   * <p>
   * Direct mode only.
   */
  CAR(true, false, false, false, false, true, false, false, false),
  /**
   * Start in the car, drive to a parking area, and walk the rest of the way.
   * <p>
   * Direct mode and access mode only.
   */
  CAR_TO_PARK(true, false, false, true, false, true, false, true, false),
  /**
   * Walk to a pickup point along the road, drive to a drop-off point along the road, and walk the
   * rest of the way. This can include various taxi-services or kiss & ride.
   */
  CAR_PICKUP(true, false, true, true, false, true, false, false, true),
  /**
   * Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way.
   * This can include car rental at fixed locations or free-floating services.
   */
  CAR_RENTAL(true, true, true, true, false, true, true, false, false),

  /**
   * Using a car hailing app like Uber or Lyft to get to a train station or all the way to the destination.
   */
  CAR_HAILING(true, false, true, false, false, true, false, false, true),

  /**
   * Encompasses all types of on-demand and flexible transportation.
   */
  FLEXIBLE(true, false, true, true, false, false, false, false, false);

  final boolean access;

  final boolean transfer;

  final boolean egress;

  final boolean includesWalking;

  final boolean includesBiking;

  final boolean includesDriving;

  final boolean includesRenting;

  final boolean includesParking;

  final boolean includesPickup;

  StreetMode(
    boolean access,
    boolean transfer,
    boolean egress,
    boolean includesWalking,
    boolean includesBiking,
    boolean includesDriving,
    boolean includesRenting,
    boolean includesParking,
    boolean includesPickup
  ) {
    this.access = access;
    this.transfer = transfer;
    this.egress = egress;
    this.includesWalking = includesWalking;
    this.includesBiking = includesBiking;
    this.includesDriving = includesDriving;
    this.includesRenting = includesRenting;
    this.includesParking = includesParking;
    this.includesPickup = includesPickup;
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

  public boolean includesRenting() {
    return includesRenting;
  }

  public boolean includesParking() {
    return includesParking;
  }

  public boolean includesPickup() {
    return includesPickup;
  }
}
