package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.util.Objects;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The preferences of the bicycle transfers: speed and reluctance for a traveler that rides
 * a bicycle, how the route is optimized, and the preferences for walking with it. The values are normalized(rounded) so the
 * class can used as a cache key.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class BicyclePreferences
  extends AbstractUserPreferences<BicyclePreferences>
  implements WithSpeed
{

  /** A change of the speed from 5.0 to 5.4 m/s has the reference distance. */
  private static final double SPEED_WEIGHT = DistanceWeight.of(
    Speed.ofMetersPerSecond(5.0),
    Speed.ofMetersPerSecond(5.4)
  );

  public static final BicyclePreferences DEFAULT = new BicyclePreferences();

  private final Speed speed;
  private final TimeSlopeSafetyTrianglePreference optimize;
  private final BicycleWalk walk;

  private BicyclePreferences() {
    super(Reluctance.of(2.0));
    this.speed = Speed.ofMetersPerSecond(5.0);
    this.optimize = TimeSlopeSafetyTrianglePreference.of(0.0, 0.0, 1.0);
    this.walk = BicycleWalk.of(Speed.ofMetersPerSecond(1.35), Reluctance.of(5.0));
  }

  private BicyclePreferences(Builder builder) {
    super(builder);
    this.speed = Objects.requireNonNull(builder.speed);
    this.optimize = Objects.requireNonNull(builder.optimize);
    this.walk = Objects.requireNonNull(builder.walk);
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public TransferProfileType type() {
    return TransferProfileType.BICYCLE;
  }

  @Override
  public Speed speed() {
    return speed;
  }

  /** How the route is optimized: the importance of the time, the slope and the safety. */
  public TimeSlopeSafetyTrianglePreference optimize() {
    return optimize;
  }

  /** The preferences for walking with the bicycle. */
  public BicycleWalk walk() {
    return walk;
  }

  @Override
  BicyclePreferences defaultInstance() {
    return DEFAULT;
  }

  @Override
  public double normalizedDistance(AbstractUserPreferences other) {
    double distance = super.normalizedDistance(other);
    if (other instanceof BicyclePreferences that) {
      distance += SPEED_WEIGHT * speed.normalizedDistance(that.speed);
      // The distance of the triangle is the sum of three parts, the average counts as one field
      distance += optimize.normalizedDistance(that.optimize) / 3.0;
      distance += walk.normalizedDistance(that.walk);
    }
    return distance;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BicyclePreferences that)) {
      return false;
    }
    return (
      super.equals(obj) &&
      speed.equals(that.speed) &&
      optimize.equals(that.optimize) &&
      walk.equals(that.walk)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), speed, optimize, walk);
  }

  @Override
  public String toString() {
    return super
      .appendToString(ToStringBuilder.of(BicyclePreferences.class))
      .addObj("speed", speed, DEFAULT.speed)
      .addObj("optimize", optimize, DEFAULT.optimize)
      .addObj("walk", walk, DEFAULT.walk)
      .toString();
  }

  public static class Builder extends AbstractUserPreferences.Builder<BicyclePreferences, Builder> {

    private Speed speed;
    private TimeSlopeSafetyTrianglePreference optimize;
    private BicycleWalk walk;

    public Builder(BicyclePreferences original) {
      super(original);
      this.speed = original.speed;
      this.optimize = original.optimize;
      this.walk = original.walk;
    }

    public Builder withSpeed(Speed speed) {
      this.speed = speed;
      return this;
    }

    public Builder withOptimize(TimeSlopeSafetyTrianglePreference optimize) {
      this.optimize = optimize;
      return this;
    }

    public Builder withWalk(BicycleWalk walk) {
      this.walk = walk;
      return this;
    }

    @Override
    BicyclePreferences doBuild() {
      return new BicyclePreferences(this);
    }
  }
}
