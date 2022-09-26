package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.function.Consumer;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * The bike preferences contain all speed, reluctance, cost and factor preferences for biking
 * related to street and transit routing. The values are normalized(rounded) so the class can used
 * as a cache key.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD SAFE.
 */
public class BikePreferences implements Serializable {

  public static final BikePreferences DEFAULT = new BikePreferences();

  private final double speed;

  private final double reluctance;
  private final int boardCost;
  private final double walkingSpeed;
  private final double walkingReluctance;
  private final int switchTime;
  private final int switchCost;
  private final int parkTime;
  private final int parkCost;
  private final BicycleOptimizeType optimizeType;
  private final TimeSlopeSafetyTriangle optimizeTriangle;

  public BikePreferences() {
    this.speed = 5;
    this.reluctance = 2.0;
    this.boardCost = 60 * 10;
    this.walkingSpeed = 1.33;
    this.walkingReluctance = 5.0;
    this.switchTime = 0;
    this.switchCost = 0;
    this.parkTime = 60;
    /** Cost of parking a bike. */
    this.parkCost = 120;
    this.optimizeType = BicycleOptimizeType.SAFE;
    this.optimizeTriangle = TimeSlopeSafetyTriangle.DEFAULT;
  }

  private BikePreferences(Builder builder) {
    this.speed = builder.speed;
    this.reluctance = builder.reluctance;
    this.boardCost = builder.boardCost;
    this.walkingSpeed = builder.walkingSpeed;
    this.walkingReluctance = builder.walkingReluctance;
    this.switchTime = builder.switchTime;
    this.switchCost = builder.switchCost;
    this.parkTime = builder.parkTime;
    this.parkCost = builder.parkCost;
    this.optimizeType = builder.optimizeType;
    this.optimizeTriangle = builder.optimizeTriangle;
  }

  public static BikePreferences.Builder of() {
    return new Builder(DEFAULT);
  }

  public BikePreferences.Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Default: 5 m/s, ~11 mph, a random bicycling speed
   */
  public double speed() {
    return speed;
  }

  public double reluctance() {
    return reluctance;
  }

  /**
   * Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot. This
   * is in addition to the cost of the transfer(biking) and waiting-time. It is also in addition to
   * the {@link TransferPreferences#cost()}.
   */
  public int boardCost() {
    return boardCost;
  }

  /**
   * The walking speed when walking a bike. Default: 1.33 m/s ~ Same as walkSpeed
   */
  public double walkingSpeed() {
    return walkingSpeed;
  }

  /**
   * A multiplier for how bad walking is, compared to being in transit for equal
   * lengths of time. Empirically, values between 2 and 4 seem to correspond
   * well to the concept of not wanting to walk too much without asking for
   * totally ridiculous itineraries, but this observation should in no way be
   * taken as scientific or definitive. Your mileage may vary. See
   * https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on
   * performance with high values. Default value: 2.0
   */
  public double walkingReluctance() {
    return walkingReluctance;
  }

  /** Time to get on and off your own bike */
  public int switchTime() {
    return switchTime;
  }

  /** Cost of getting on and off your own bike */
  public int switchCost() {
    return switchCost;
  }

  /** Time to park a bike */
  public int parkTime() {
    return parkTime;
  }

  /** Cost of parking a bike. */
  public int parkCost() {
    return parkCost;
  }

  /**
   * The set of characteristics that the user wants to optimize for -- defaults to SAFE.
   */
  public BicycleOptimizeType optimizeType() {
    return optimizeType;
  }

  public TimeSlopeSafetyTriangle optimizeTriangle() {
    return optimizeTriangle;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(BikePreferences.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addNum("boardCost", boardCost, DEFAULT.boardCost)
      .addNum("walkingSpeed", walkingSpeed, DEFAULT.walkingSpeed)
      .addNum("walkingReluctance", walkingReluctance, DEFAULT.walkingReluctance)
      .addDurationSec("switchTime", switchTime, DEFAULT.switchTime)
      .addNum("switchCost", switchCost, DEFAULT.switchCost)
      .addDurationSec("parkTime", parkTime, DEFAULT.parkTime)
      .addNum("parkCost", parkCost, DEFAULT.parkCost)
      .addEnum("optimizeType", optimizeType, DEFAULT.optimizeType)
      .addObj("optimizeTriangle", optimizeTriangle, DEFAULT.optimizeTriangle)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final BikePreferences original;
    private double speed;
    private double reluctance;
    private int boardCost;
    private double walkingSpeed;
    private double walkingReluctance;
    private int switchTime;
    private int switchCost;
    private int parkTime;
    private int parkCost;
    private BicycleOptimizeType optimizeType;
    private TimeSlopeSafetyTriangle optimizeTriangle;

    public Builder(BikePreferences original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.boardCost = original.boardCost;
      this.walkingSpeed = original.walkingSpeed;
      this.walkingReluctance = original.walkingReluctance;
      this.switchTime = original.switchTime;
      this.switchCost = original.switchCost;
      this.parkTime = original.parkTime;
      this.parkCost = original.parkCost;
      this.optimizeType = original.optimizeType;
      this.optimizeTriangle = original.optimizeTriangle;
    }

    public double speed() {
      return speed;
    }

    public Builder setSpeed(double speed) {
      this.speed = speed;
      return this;
    }

    public double reluctance() {
      return reluctance;
    }

    public Builder setReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public int boardCost() {
      return boardCost;
    }

    public Builder setBoardCost(int boardCost) {
      this.boardCost = boardCost;
      return this;
    }

    public double walkingSpeed() {
      return walkingSpeed;
    }

    public Builder setWalkingSpeed(double walkingSpeed) {
      this.walkingSpeed = walkingSpeed;
      return this;
    }

    public double walkingReluctance() {
      return walkingReluctance;
    }

    public Builder setWalkingReluctance(double walkingReluctance) {
      this.walkingReluctance = walkingReluctance;
      return this;
    }

    public int switchTime() {
      return switchTime;
    }

    public Builder setSwitchTime(int switchTime) {
      this.switchTime = switchTime;
      return this;
    }

    public int switchCost() {
      return switchCost;
    }

    public Builder setSwitchCost(int switchCost) {
      this.switchCost = switchCost;
      return this;
    }

    public int parkTime() {
      return parkTime;
    }

    public Builder setParkTime(int parkTime) {
      this.parkTime = parkTime;
      return this;
    }

    public int parkCost() {
      return parkCost;
    }

    public Builder setParkCost(int parkCost) {
      this.parkCost = parkCost;
      return this;
    }

    public BicycleOptimizeType optimizeType() {
      return optimizeType;
    }

    public Builder setOptimizeType(BicycleOptimizeType optimizeType) {
      this.optimizeType = optimizeType;
      return this;
    }

    public TimeSlopeSafetyTriangle optimizeTriangle() {
      return optimizeTriangle;
    }

    public Builder withOptimizeTriangle(Consumer<TimeSlopeSafetyTriangle.Builder> body) {
      var builder = TimeSlopeSafetyTriangle.of();
      body.accept(builder);
      this.optimizeTriangle = builder.buildOrDefault(this.optimizeTriangle);
      return this;
    }

    public BikePreferences build() {
      var value = new BikePreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
