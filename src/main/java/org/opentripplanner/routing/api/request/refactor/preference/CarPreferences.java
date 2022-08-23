package org.opentripplanner.routing.api.request.refactor.preference;

public class CarPreferences {

  /**
   * Max car speed along streets, in meters per second.
   * <p>
   * Default: 40 m/s, 144 km/h, above the maximum (finite) driving speed limit worldwide.
   */
  private double speed = 40.0;
  private double reluctance = 2.0;
  /** Time to park a car */
  private int parkTime = 60;
  /** Cost of parking a car. */
  private int parkCost = 120;
  /**
   * Time to park a car in a park and ride, w/o taking into account driving and walking cost (time
   * to park, switch off, pick your stuff, lock the car, etc...)
   */
  private int dropoffTime = 120;
  /** Time of getting in/out of a carPickup (taxi) */
  private int pickupTime = 60;
  /** Cost of getting in/out of a carPickup (taxi) */
  private int pickupCost = 120;
  /**
   * The deceleration speed of an automobile, in meters per second per second.
   */
  // 2.9 m/s/s: 65 mph - 0 mph in 10 seconds
  private double decelerationSpeed = 2.9;
  /**
   * The acceleration speed of an automobile, in meters per second per second.
   */
  // 2.9 m/s/s: 0 mph to 65 mph in 10 seconds
  private double accelerationSpeed = 2.9;

  // TODO: 2022-08-18 should it be here?
  private boolean parkAndRide = false;
  // TODO: 2022-08-18 should it be here?
  private boolean allowPickup = false;

  public void setSpeed(double speed) {
    this.speed = speed;
  }

  public double speed() {
    return speed;
  }

  public void setReluctance(double reluctance) {
    this.reluctance = reluctance;
  }

  public double reluctance() {
    return reluctance;
  }

  public void setParkTime(int parkTime) {
    this.parkTime = parkTime;
  }

  public int parkTime() {
    return parkTime;
  }

  public void setParkCost(int parkCost) {
    this.parkCost = parkCost;
  }

  public int parkCost() {
    return parkCost;
  }

  public void setDropoffTime(int dropoffTime) {
    this.dropoffTime = dropoffTime;
  }

  public int dropoffTime() {
    return dropoffTime;
  }

  public void setPickupTime(int pickupTime) {
    this.pickupTime = pickupTime;
  }

  public int pickupTime() {
    return pickupTime;
  }

  public void setPickupCost(int pickupCost) {
    this.pickupCost = pickupCost;
  }

  public int pickupCost() {
    return pickupCost;
  }

  public void setDecelerationSpeed(double decelerationSpeed) {
    this.decelerationSpeed = decelerationSpeed;
  }

  public double decelerationSpeed() {
    return decelerationSpeed;
  }

  public void setAccelerationSpeed(double accelerationSpeed) {
    this.accelerationSpeed = accelerationSpeed;
  }

  public double accelerationSpeed() {
    return accelerationSpeed;
  }

  public void setParkAndRide(boolean parkAndRide) {
    this.parkAndRide = parkAndRide;
  }

  public boolean parkAndRide() {
    return parkAndRide;
  }

  public void setAllowPickup(boolean allowPickup) {
    this.allowPickup = allowPickup;
  }

  public boolean allowPickup() {
    return allowPickup;
  }
}
