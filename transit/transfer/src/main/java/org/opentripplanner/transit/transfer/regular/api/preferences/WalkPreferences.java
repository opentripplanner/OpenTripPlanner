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
  public double normalizedDistance(AbstractUserPreferences other) {
    double distance = super.normalizedDistance(other);
    if (other instanceof WalkPreferences that) {
      distance += speed.normalizedDistance(that.speed);
      distance += safetyFactor.normalizedDistance(that.safetyFactor);
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
