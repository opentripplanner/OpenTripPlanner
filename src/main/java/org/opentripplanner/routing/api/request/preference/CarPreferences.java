package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.framework.Units;

/**
 * The car preferences contain all speed, reluctance, cost and factor preferences for driving
 * related to street routing. The values are normalized(rounded) so the class can used as a cache
 * key.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class CarPreferences implements Serializable {

  public static final CarPreferences DEFAULT = new CarPreferences();

  private final double speed;
  private final double reluctance;
  private final int parkTime;
  private final Cost parkCost;
  private final int pickupTime;
  private final Cost pickupCost;
  private final int dropoffTime;
  private final double accelerationSpeed;
  private final double decelerationSpeed;

  /** Create a new instance with default values. */
  private CarPreferences() {
    this.speed = 40.0;
    this.reluctance = 2.0;
    this.parkTime = 60;
    this.parkCost = Cost.costOfMinutes(2);
    this.pickupTime = 60;
    this.pickupCost = Cost.costOfMinutes(2);
    this.dropoffTime = 120;
    this.accelerationSpeed = 2.9;
    this.decelerationSpeed = 2.9;
  }

  private CarPreferences(Builder builder) {
    this.speed = Units.speed(builder.speed);
    this.reluctance = Units.reluctance(builder.reluctance);
    this.parkTime = Units.duration(builder.parkTime);
    this.parkCost = builder.parkCost;
    this.pickupTime = Units.duration(builder.pickupTime);
    this.pickupCost = builder.pickupCost;
    this.dropoffTime = Units.duration(builder.dropoffTime);
    this.accelerationSpeed = Units.acceleration(builder.accelerationSpeed);
    this.decelerationSpeed = Units.acceleration(builder.decelerationSpeed);
  }

  public static CarPreferences.Builder of() {
    return DEFAULT.copyOf();
  }

  public CarPreferences.Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Max car speed along streets, in meters per second.
   * <p>
   * Default: 40 m/s, 144 km/h, above the maximum (finite) driving speed limit worldwide.
   */
  public double speed() {
    return speed;
  }

  public double reluctance() {
    return reluctance;
  }

  /** Time to park a car. */
  public int parkTime() {
    return parkTime;
  }

  /** Cost of parking a car. */
  public int parkCost() {
    return parkCost.toSeconds();
  }

  /** Time of getting in/out of a carPickup (taxi) */
  public int pickupTime() {
    return pickupTime;
  }

  /** Cost of getting in/out of a carPickup (taxi) */
  public int pickupCost() {
    return pickupCost.toSeconds();
  }

  /**
   * Time to park a car in a park and ride, w/o taking into account driving and walking cost (time
   * to park, switch off, pick your stuff, lock the car, etc...)
   */
  public int dropoffTime() {
    return dropoffTime;
  }

  /**
   * The acceleration speed of an automobile, in meters per second per second.
   * Default is 2.9 m/s^2 (0 mph to 65 mph in 10 seconds)
   */
  public double accelerationSpeed() {
    return accelerationSpeed;
  }

  /**
   * The deceleration speed of an automobile, in meters per second per second.
   * The default is 2.9 m/s/s: 65 mph - 0 mph in 10 seconds
   */
  public double decelerationSpeed() {
    return decelerationSpeed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CarPreferences that = (CarPreferences) o;
    return (
      DoubleUtils.doubleEquals(that.speed, speed) &&
      DoubleUtils.doubleEquals(that.reluctance, reluctance) &&
      parkTime == that.parkTime &&
      parkCost.equals(that.parkCost) &&
      pickupTime == that.pickupTime &&
      pickupCost.equals(that.pickupCost) &&
      dropoffTime == that.dropoffTime &&
      DoubleUtils.doubleEquals(that.accelerationSpeed, accelerationSpeed) &&
      DoubleUtils.doubleEquals(that.decelerationSpeed, decelerationSpeed)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      speed,
      reluctance,
      parkTime,
      parkCost,
      pickupTime,
      pickupCost,
      dropoffTime,
      accelerationSpeed,
      decelerationSpeed
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(CarPreferences.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addNum("parkTime", parkTime, DEFAULT.parkTime)
      .addObj("parkCost", parkCost, DEFAULT.parkCost)
      .addNum("pickupTime", pickupTime, DEFAULT.pickupTime)
      .addObj("pickupCost", pickupCost, DEFAULT.pickupCost)
      .addNum("dropoffTime", dropoffTime, DEFAULT.dropoffTime)
      .addNum("accelerationSpeed", accelerationSpeed, DEFAULT.accelerationSpeed)
      .addNum("decelerationSpeed", decelerationSpeed, DEFAULT.decelerationSpeed)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final CarPreferences original;
    private double speed;
    private double reluctance;
    private int parkTime;
    private Cost parkCost;
    private int pickupTime;
    private Cost pickupCost;
    private int dropoffTime;
    private double accelerationSpeed;
    private double decelerationSpeed;

    public Builder(CarPreferences original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.parkTime = original.parkTime;
      this.parkCost = original.parkCost;
      this.pickupTime = original.pickupTime;
      this.pickupCost = original.pickupCost;
      this.dropoffTime = original.dropoffTime;
      this.accelerationSpeed = original.accelerationSpeed;
      this.decelerationSpeed = original.decelerationSpeed;
    }

    public CarPreferences original() {
      return original;
    }

    public Builder withSpeed(double speed) {
      this.speed = speed;
      return this;
    }

    public Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public Builder withParkTime(int parkTime) {
      this.parkTime = parkTime;
      return this;
    }

    public Builder withParkCost(int parkCost) {
      this.parkCost = Cost.costOfSeconds(parkCost);
      return this;
    }

    public Builder withPickupTime(int pickupTime) {
      this.pickupTime = pickupTime;
      return this;
    }

    public Builder withPickupCost(int pickupCost) {
      this.pickupCost = Cost.costOfSeconds(pickupCost);
      return this;
    }

    public Builder withDropoffTime(int dropoffTime) {
      this.dropoffTime = dropoffTime;
      return this;
    }

    public Builder withAccelerationSpeed(double accelerationSpeed) {
      this.accelerationSpeed = accelerationSpeed;
      return this;
    }

    public Builder withDecelerationSpeed(double decelerationSpeed) {
      this.decelerationSpeed = decelerationSpeed;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public CarPreferences build() {
      var value = new CarPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
