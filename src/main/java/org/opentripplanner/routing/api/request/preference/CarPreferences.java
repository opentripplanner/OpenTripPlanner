package org.opentripplanner.routing.api.request.preference;

// TODO VIA (Thomas): Javadoc
import java.io.Serializable;

public class CarPreferences implements Cloneable, Serializable {

  private double speed = 40.0;
  private double reluctance = 2.0;
  private int parkTime = 60;
  private int parkCost = 120;
  private int dropoffTime = 120;
  private int pickupTime = 60;
  private int pickupCost = 120;
  private double decelerationSpeed = 2.9;
  private double accelerationSpeed = 2.9;

  public CarPreferences clone() {
    try {
      var clone = (CarPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  /**
   * Max car speed along streets, in meters per second.
   * <p>
   * Default: 40 m/s, 144 km/h, above the maximum (finite) driving speed limit worldwide.
   */
  public double speed() {
    return speed;
  }

  public void setSpeed(double speed) {
    this.speed = speed;
  }

  public double reluctance() {
    return reluctance;
  }

  public void setReluctance(double reluctance) {
    this.reluctance = reluctance;
  }

  /** Time to park a car. */
  public int parkTime() {
    return parkTime;
  }

  public void setParkTime(int parkTime) {
    this.parkTime = parkTime;
  }

  /** Cost of parking a car. */
  public int parkCost() {
    return parkCost;
  }

  public void setParkCost(int parkCost) {
    this.parkCost = parkCost;
  }

  /**
   * Time to park a car in a park and ride, w/o taking into account driving and walking cost (time
   * to park, switch off, pick your stuff, lock the car, etc...)
   */
  public int dropoffTime() {
    return dropoffTime;
  }

  public void setDropoffTime(int dropoffTime) {
    this.dropoffTime = dropoffTime;
  }

  /** Time of getting in/out of a carPickup (taxi) */
  public int pickupTime() {
    return pickupTime;
  }

  public void setPickupTime(int pickupTime) {
    this.pickupTime = pickupTime;
  }

  /** Cost of getting in/out of a carPickup (taxi) */
  public int pickupCost() {
    return pickupCost;
  }

  public void setPickupCost(int pickupCost) {
    this.pickupCost = pickupCost;
  }

  /**
   * The deceleration speed of an automobile, in meters per second per second.
   * The default is 2.9 m/s/s: 65 mph - 0 mph in 10 seconds
   */
  public double decelerationSpeed() {
    return decelerationSpeed;
  }

  public void setDecelerationSpeed(double decelerationSpeed) {
    this.decelerationSpeed = decelerationSpeed;
  }

  /**
   * The acceleration speed of an automobile, in meters per second per second.
   * Default is 2.9 m/s^2 (0 mph to 65 mph in 10 seconds)
   */
  public double accelerationSpeed() {
    return accelerationSpeed;
  }

  public void setAccelerationSpeed(double accelerationSpeed) {
    this.accelerationSpeed = accelerationSpeed;
  }
}
