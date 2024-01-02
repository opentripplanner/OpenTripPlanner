package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.framework.lang.DoubleUtils.doubleEquals;
import static org.opentripplanner.framework.lang.ObjectUtils.ifNotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.core.BicycleOptimizeType;

/**
 * The bike preferences contain all speed, reluctance, cost and factor preferences for biking
 * related to street and transit routing. The values are normalized(rounded) so the class can used
 * as a cache key.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class BikePreferences implements Serializable {

  public static final BikePreferences DEFAULT = new BikePreferences();

  private final double speed;
  private final double reluctance;
  private final Cost boardCost;
  private final double walkingSpeed;
  private final double walkingReluctance;
  private final int switchTime;
  private final Cost switchCost;
  private final VehicleParkingPreferences parking;
  private final double stairsReluctance;
  private final BicycleOptimizeType optimizeType;
  private final TimeSlopeSafetyTriangle optimizeTriangle;

  private BikePreferences() {
    this.speed = 5;
    this.reluctance = 2.0;
    this.boardCost = Cost.costOfMinutes(10);
    this.walkingSpeed = 1.33;
    this.walkingReluctance = 5.0;
    this.switchTime = 0;
    this.switchCost = Cost.ZERO;
    this.parking = VehicleParkingPreferences.DEFAULT;
    this.optimizeType = BicycleOptimizeType.SAFE;
    this.optimizeTriangle = TimeSlopeSafetyTriangle.DEFAULT;
    // very high reluctance to carry the bike up/down a flight of stairs
    this.stairsReluctance = 10;
  }

  private BikePreferences(Builder builder) {
    this.speed = Units.speed(builder.speed);
    this.reluctance = Units.reluctance(builder.reluctance);
    this.boardCost = builder.boardCost;
    this.walkingSpeed = Units.speed(builder.walkingSpeed);
    this.walkingReluctance = Units.reluctance(builder.walkingReluctance);
    this.switchTime = Units.duration(builder.switchTime);
    this.switchCost = builder.switchCost;
    this.parking = builder.parking;
    this.optimizeType = Objects.requireNonNull(builder.optimizeType);
    this.optimizeTriangle = Objects.requireNonNull(builder.optimizeTriangle);
    this.stairsReluctance = Units.reluctance(builder.stairsReluctance);
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
    return boardCost.toSeconds();
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
    return switchCost.toSeconds();
  }

  /** Parking preferences that can be different per request */
  public VehicleParkingPreferences parking() {
    return parking;
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

  public double stairsReluctance() {
    return stairsReluctance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BikePreferences that = (BikePreferences) o;
    return (
      doubleEquals(that.speed, speed) &&
      doubleEquals(that.reluctance, reluctance) &&
      boardCost.equals(that.boardCost) &&
      doubleEquals(that.walkingSpeed, walkingSpeed) &&
      doubleEquals(that.walkingReluctance, walkingReluctance) &&
      switchTime == that.switchTime &&
      switchCost.equals(that.switchCost) &&
      parking.equals(that.parking) &&
      optimizeType == that.optimizeType &&
      optimizeTriangle.equals(that.optimizeTriangle) &&
      doubleEquals(stairsReluctance, that.stairsReluctance)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      speed,
      reluctance,
      boardCost,
      walkingSpeed,
      walkingReluctance,
      switchTime,
      switchCost,
      parking,
      optimizeType,
      optimizeTriangle,
      stairsReluctance
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(BikePreferences.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addObj("boardCost", boardCost, DEFAULT.boardCost)
      .addNum("walkingSpeed", walkingSpeed, DEFAULT.walkingSpeed)
      .addNum("walkingReluctance", walkingReluctance, DEFAULT.walkingReluctance)
      .addDurationSec("switchTime", switchTime, DEFAULT.switchTime)
      .addObj("switchCost", switchCost, DEFAULT.switchCost)
      .addObj("parking", parking, DEFAULT.parking)
      .addEnum("optimizeType", optimizeType, DEFAULT.optimizeType)
      .addObj("optimizeTriangle", optimizeTriangle, DEFAULT.optimizeTriangle)
      .addNum("stairsReluctance", stairsReluctance, DEFAULT.stairsReluctance)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final BikePreferences original;
    private double speed;
    private double reluctance;
    private Cost boardCost;
    private double walkingSpeed;
    private double walkingReluctance;
    private int switchTime;
    private Cost switchCost;
    private VehicleParkingPreferences parking;
    private BicycleOptimizeType optimizeType;
    private TimeSlopeSafetyTriangle optimizeTriangle;

    public double stairsReluctance;

    public Builder(BikePreferences original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.boardCost = original.boardCost;
      this.walkingSpeed = original.walkingSpeed;
      this.walkingReluctance = original.walkingReluctance;
      this.switchTime = original.switchTime;
      this.switchCost = original.switchCost;
      this.parking = original.parking;
      this.optimizeType = original.optimizeType;
      this.optimizeTriangle = original.optimizeTriangle;
      this.stairsReluctance = original.stairsReluctance;
    }

    public BikePreferences original() {
      return original;
    }

    public double speed() {
      return speed;
    }

    public Builder withSpeed(double speed) {
      this.speed = speed;
      return this;
    }

    public double reluctance() {
      return reluctance;
    }

    public Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public Cost boardCost() {
      return boardCost;
    }

    public Builder withBoardCost(int boardCost) {
      this.boardCost = Cost.costOfSeconds(boardCost);
      return this;
    }

    public double walkingSpeed() {
      return walkingSpeed;
    }

    public Builder withWalkingSpeed(double walkingSpeed) {
      this.walkingSpeed = walkingSpeed;
      return this;
    }

    public double walkingReluctance() {
      return walkingReluctance;
    }

    public Builder withWalkingReluctance(double walkingReluctance) {
      this.walkingReluctance = walkingReluctance;
      return this;
    }

    public int switchTime() {
      return switchTime;
    }

    public Builder withSwitchTime(int switchTime) {
      this.switchTime = switchTime;
      return this;
    }

    public Cost switchCost() {
      return switchCost;
    }

    public Builder withSwitchCost(int switchCost) {
      this.switchCost = Cost.costOfSeconds(switchCost);
      return this;
    }

    public Builder withParking(Consumer<VehicleParkingPreferences.Builder> body) {
      this.parking = ifNotNull(this.parking, original.parking).copyOf().apply(body).build();
      return this;
    }

    public BicycleOptimizeType optimizeType() {
      return optimizeType;
    }

    public Builder withOptimizeType(BicycleOptimizeType optimizeType) {
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

    public Builder withStairsReluctance(double value) {
      this.stairsReluctance = value;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public BikePreferences build() {
      var value = new BikePreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
