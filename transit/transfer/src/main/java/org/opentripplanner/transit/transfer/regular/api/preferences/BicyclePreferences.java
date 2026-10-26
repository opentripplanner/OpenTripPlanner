package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.util.Objects;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The preferences of the bicycle transfers: speed and reluctance for a traveler that rides
 * a bicycle. The values are normalized(rounded) so the class can used as a
 * cache key.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class BicyclePreferences
  extends AbstractUserPreferences<BicyclePreferences>
  implements WithSpeed
{

  public static final BicyclePreferences DEFAULT = new BicyclePreferences();

  private final Speed speed;

  private BicyclePreferences() {
    super(Reluctance.of(2.0));
    this.speed = Speed.ofMetersPerSecond(5.0);
  }

  private BicyclePreferences(Builder builder) {
    super(builder);
    this.speed = Objects.requireNonNull(builder.speed);
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

  @Override
  BicyclePreferences defaultInstance() {
    return DEFAULT;
  }

  @Override
  public float normalizedDistance(AbstractUserPreferences other) {
    float distance = super.normalizedDistance(other);
    if (other instanceof BicyclePreferences that) {
      distance += speed.normalizedDistance(that.speed);
    }
    return distance;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BicyclePreferences that)) {
      return false;
    }
    return super.equals(obj) && speed.equals(that.speed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), speed);
  }

  @Override
  public String toString() {
    return super
      .appendToString(ToStringBuilder.of(BicyclePreferences.class))
      .addObj("speed", speed, DEFAULT.speed)
      .toString();
  }

  public static class Builder extends AbstractUserPreferences.Builder<BicyclePreferences, Builder> {

    private Speed speed;

    public Builder(BicyclePreferences original) {
      super(original);
      this.speed = original.speed;
    }

    public Builder withSpeed(Speed speed) {
      this.speed = speed;
      return this;
    }

    @Override
    BicyclePreferences doBuild() {
      return new BicyclePreferences(this);
    }
  }
}
