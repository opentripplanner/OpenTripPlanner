package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.util.Objects;
import org.opentripplanner.core.model.basic.NormalizedRatio;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The preferences of the wheelchair transfers: speed, reluctance and safety factor for a traveler
 * in a wheelchair. The values are normalized(rounded) so the class can used as a cache key.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class WheelchairPreferences
  extends AbstractUserPreferences<WheelchairPreferences>
  implements WithSpeed, WithSafetyFactor
{

  /** A change of the speed from 1.3 to 1.4 m/s has the reference distance. */
  private static final double SPEED_WEIGHT = DistanceWeight.of(
    Speed.ofMetersPerSecond(1.3),
    Speed.ofMetersPerSecond(1.4)
  );

  public static final WheelchairPreferences DEFAULT = new WheelchairPreferences();

  private final Speed speed;
  private final NormalizedRatio safetyFactor;

  private WheelchairPreferences() {
    super(Reluctance.of(2.0));
    this.speed = Speed.ofMetersPerSecond(1.35);
    this.safetyFactor = Ratio.of(1.0).normalize();
  }

  private WheelchairPreferences(Builder builder) {
    super(builder);
    this.speed = Objects.requireNonNull(builder.speed);
    this.safetyFactor = Objects.requireNonNull(builder.safetyFactor);
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public TransferProfileType type() {
    return TransferProfileType.WHEELCHAIR;
  }

  @Override
  public Speed speed() {
    return speed;
  }

  @Override
  public NormalizedRatio safetyFactor() {
    return safetyFactor;
  }

  @Override
  WheelchairPreferences defaultInstance() {
    return DEFAULT;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The distance is the sum of the reluctance, the speed and the safety factor. The reluctance and
   * the speed are weighted so a reference change counts {@link DistanceWeight#REFERENCE_DISTANCE}:
   * 2.0 to 2.5 for the reluctance and 1.3 to 1.4 m/s for the speed. The safety factor has the
   * same weight and asymmetry as for {@link WalkPreferences}, the wheelchair search uses the walk
   * safety of the streets.
   * <p>
   * The reluctance hardly matters (it only changes paths where there are elevators). The speed
   * matters more in the data, but the numbers are inflated: the street search can return a path
   * over stairs when the time limit prunes the better route. The weights should be reviewed when
   * that is fixed.
   */
  @Override
  public double normalizedDistance(AbstractUserPreferences other) {
    double distance = super.normalizedDistance(other);
    if (other instanceof WheelchairPreferences that) {
      distance += SPEED_WEIGHT * speed.normalizedDistance(that.speed);
      distance += WalkPreferences.safetyDistance(safetyFactor, that.safetyFactor);
    }
    return distance;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WheelchairPreferences that)) {
      return false;
    }
    return super.equals(obj) && speed.equals(that.speed) && safetyFactor.equals(that.safetyFactor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), speed, safetyFactor);
  }

  @Override
  public String toString() {
    return super
      .appendToString(ToStringBuilder.of(WheelchairPreferences.class))
      .addObj("speed", speed, DEFAULT.speed)
      .addObj("safetyFactor", safetyFactor, DEFAULT.safetyFactor)
      .toString();
  }

  public static class Builder
    extends AbstractUserPreferences.Builder<WheelchairPreferences, Builder>
  {

    private Speed speed;
    private NormalizedRatio safetyFactor;

    public Builder(WheelchairPreferences original) {
      super(original);
      this.speed = original.speed;
      this.safetyFactor = original.safetyFactor;
    }

    public Builder withSpeed(Speed speed) {
      this.speed = speed;
      return this;
    }

    public Builder withSafetyFactor(NormalizedRatio safetyFactor) {
      this.safetyFactor = safetyFactor;
      return this;
    }

    @Override
    WheelchairPreferences doBuild() {
      return new WheelchairPreferences(this);
    }
  }
}
