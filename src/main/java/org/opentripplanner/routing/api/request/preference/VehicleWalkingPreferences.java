package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * Preferences for walking a vehicle.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public class VehicleWalkingPreferences implements Serializable {

  public static final VehicleWalkingPreferences DEFAULT = new VehicleWalkingPreferences();

  private final double speed;
  private final double reluctance;
  private final Duration hopTime;
  private final Cost hopCost;
  private final double stairsReluctance;

  private VehicleWalkingPreferences() {
    this.speed = 1.33;
    this.reluctance = 5.0;
    this.hopTime = Duration.ZERO;
    this.hopCost = Cost.ZERO;
    // very high reluctance to carry the bike up/down a flight of stairs
    this.stairsReluctance = 10;
  }

  /**
   * Sets the vehicle walking preferences, does some input value validation and rounds
   * reluctances and speed to not have too many decimals.
   */
  private VehicleWalkingPreferences(Builder builder) {
    this.speed = Units.speed(builder.speed);
    this.reluctance = Units.reluctance(builder.reluctance);
    this.hopTime = Duration.ofSeconds(Units.duration(builder.hopTime));
    this.hopCost = Cost.costOfSeconds(builder.hopCost);
    this.stairsReluctance = Units.reluctance(builder.stairsReluctance);
  }

  public static VehicleWalkingPreferences.Builder of() {
    return new VehicleWalkingPreferences.Builder(DEFAULT);
  }

  public VehicleWalkingPreferences.Builder copyOf() {
    return new VehicleWalkingPreferences.Builder(this);
  }

  /**
   * The walking speed when walking a vehicle. Default: 1.33 m/s ~ Same as walkSpeed.
   */
  public double speed() {
    return speed;
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
  public double reluctance() {
    return reluctance;
  }

  /** Time to get on and off your own vehicle. */
  public Duration hopTime() {
    return hopTime;
  }

  /** Cost of getting on and off your own vehicle. */
  public Cost hopCost() {
    return hopCost;
  }

  /** Reluctance of walking carrying a vehicle up a flight of stairs. */
  public double stairsReluctance() {
    return stairsReluctance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleWalkingPreferences that = (VehicleWalkingPreferences) o;
    return (
      speed == that.speed &&
      reluctance == that.reluctance &&
      Objects.equals(hopTime, that.hopTime) &&
      Objects.equals(hopCost, that.hopCost) &&
      stairsReluctance == that.stairsReluctance
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(speed, reluctance, hopTime, hopCost, stairsReluctance);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(VehicleWalkingPreferences.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addObj("hopTime", hopTime, DEFAULT.hopTime)
      .addObj("hopCost", hopCost, DEFAULT.hopCost)
      .addNum("stairsReluctance", stairsReluctance, DEFAULT.stairsReluctance)
      .toString();
  }

  public static class Builder {

    private final VehicleWalkingPreferences original;
    private double speed;
    private double reluctance;
    private int hopTime;
    private int hopCost;
    private double stairsReluctance;

    private Builder(VehicleWalkingPreferences original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.hopTime = (int) original.hopTime.toSeconds();
      this.hopCost = original.hopCost.toSeconds();
      this.stairsReluctance = original.stairsReluctance;
    }

    public VehicleWalkingPreferences.Builder withSpeed(double speed) {
      this.speed = speed;
      return this;
    }

    public VehicleWalkingPreferences.Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public VehicleWalkingPreferences.Builder withHopTime(Duration hopTime) {
      this.hopTime = (int) hopTime.toSeconds();
      return this;
    }

    public VehicleWalkingPreferences.Builder withHopTime(int hopTime) {
      this.hopTime = hopTime;
      return this;
    }

    public VehicleWalkingPreferences.Builder withHopCost(int hopCost) {
      this.hopCost = hopCost;
      return this;
    }

    public VehicleWalkingPreferences.Builder withStairsReluctance(double stairsReluctance) {
      this.stairsReluctance = stairsReluctance;
      return this;
    }

    public VehicleWalkingPreferences original() {
      return original;
    }

    public VehicleWalkingPreferences.Builder apply(
      Consumer<VehicleWalkingPreferences.Builder> body
    ) {
      body.accept(this);
      return this;
    }

    public VehicleWalkingPreferences build() {
      var newObj = new VehicleWalkingPreferences(this);
      return original.equals(newObj) ? original : newObj;
    }
  }
}
