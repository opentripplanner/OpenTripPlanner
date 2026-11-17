package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.util.Objects;
import org.opentripplanner.core.model.basic.NormalizedRatio;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The walk preferences contain all speed, reluctance, cost and factor preferences for walking
 * related to street and transit routing. The values are normalized(rounded) so the class
 * can used as a cache key.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class WalkPreferences
  extends AbstractUserPreferences<WalkPreferences>
  implements WithSpeed, WithSafetyFactor
{

  /**
   * The speed and the reluctance count this share of the reference distance, a change of the speed
   * from 1.3 to 1.4 m/s, or of the reluctance from 2.0 to 2.5, is
   * {@code DistanceWeight.REFERENCE_DISTANCE * SPEED_AND_RELUCTANCE_SHARE}.
   */
  static final double SPEED_AND_RELUCTANCE_SHARE = 0.25;

  private static final double SPEED_WEIGHT =
    DistanceWeight.of(Speed.ofMetersPerSecond(1.3), Speed.ofMetersPerSecond(1.4)) *
    SPEED_AND_RELUCTANCE_SHARE;

  /** A change of the safety factor from 0.5 to 0.6 has the reference distance. */
  private static final double SAFETY_FACTOR_WEIGHT = DistanceWeight.of(
    Ratio.of(0.5).normalize(),
    Ratio.of(0.6).normalize()
  );

  /** The extra weight of the safety factor when the user wants more safety than the profile. */
  private static final double MORE_SAFETY_WEIGHT_FACTOR = 2.0;

  public static final WalkPreferences DEFAULT = new WalkPreferences();

  private final Speed speed;
  private final NormalizedRatio safetyFactor;

  private WalkPreferences() {
    super(Reluctance.of(2.0));
    this.speed = Speed.ofMetersPerSecond(1.35);
    this.safetyFactor = Ratio.of(1.0).normalize();
  }

  private WalkPreferences(Builder builder) {
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
    return TransferProfileType.WALK;
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
  WalkPreferences defaultInstance() {
    return DEFAULT;
  }

  @Override
  protected double reluctanceWeight() {
    return super.reluctanceWeight() * SPEED_AND_RELUCTANCE_SHARE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The distance is from the preferences of the user (this) to the preferences of a profile
   * ({@code other}). It is the sum of the reluctance, the speed and the safety factor.
   * <p>
   * The safety factor is the field that matters. A profile with another safety factor changes the
   * path of 35-87% of the transfers and costs 1-6.5% extra on average. Speed and reluctance only
   * change the path where an elevator, an escalator or a pathway is involved, and cost 0.01-0.5%
   * extra over all transfers. So a step of the safety factor, from 0.5 to 0.6, counts
   * {@link DistanceWeight#REFERENCE_DISTANCE}, and a step of the speed, from 1.3 to 1.4 m/s, or of
   * the reluctance, from 2.0 to 2.5, counts {@link #SPEED_AND_RELUCTANCE_SHARE} of that. With equal
   * weights a user with a slow speed and a low safety factor got a profile with the wrong safety
   * factor and the right speed, 3% extra against 0.5%.
   * <p>
   * The safety factor is <b>asymmetric</b>: its distance is multiplied by
   * {@link #MORE_SAFETY_WEIGHT_FACTOR} if the user wants more safety than the profile has. Using
   * a profile with safety factor 0 for a user with 1.0 costs 11-13% extra, the reverse costs
   * 2.6-3.8%. The factor is a rough default, how much it matters depends on the safety values of
   * the street data (the OSM tag mapping). The effect is also not linear: it is steep near 0 and
   * flat near 1 (0.5 to 1.0 costs 0.3-0.5%, 0 to 0.5 costs 5-6%). A logarithmic scale
   * {@code ln(1 + 9 * f)} fits much better than the linear one used here, but the linear one is
   * simple, and good enough to prefer the right profile.
   */
  @Override
  public double normalizedDistance(AbstractUserPreferences other) {
    double distance = super.normalizedDistance(other);
    if (other instanceof WalkPreferences that) {
      distance += SPEED_WEIGHT * speed.normalizedDistance(that.speed);
      double safetyDistance =
        SAFETY_FACTOR_WEIGHT * safetyFactor.normalizedDistance(that.safetyFactor);
      if (safetyFactor.asDouble() > that.safetyFactor.asDouble()) {
        safetyDistance *= MORE_SAFETY_WEIGHT_FACTOR;
      }
      distance += safetyDistance;
    }
    return distance;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WalkPreferences that)) {
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
      .appendToString(ToStringBuilder.of(WalkPreferences.class))
      .addObj("speed", speed, DEFAULT.speed)
      .addObj("safetyFactor", safetyFactor, DEFAULT.safetyFactor)
      .toString();
  }

  public static class Builder extends AbstractUserPreferences.Builder<WalkPreferences, Builder> {

    private Speed speed;
    private NormalizedRatio safetyFactor;

    public Builder(WalkPreferences original) {
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
    WalkPreferences doBuild() {
      return new WalkPreferences(this);
    }
  }
}
