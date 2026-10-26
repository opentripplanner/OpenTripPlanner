package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.util.Objects;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The preferences of the wheelchair transfers: speed and reluctance for a traveler in a
 * wheelchair. The values are normalized(rounded) so the class can used as a cache key.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class WheelchairPreferences
  extends AbstractUserPreferences<WheelchairPreferences>
  implements WithSpeed
{

  public static final WheelchairPreferences DEFAULT = new WheelchairPreferences();

  private final Speed speed;

  private WheelchairPreferences() {
    super(Reluctance.of(2.0));
    this.speed = Speed.ofMetersPerSecond(1.35);
  }

  private WheelchairPreferences(Builder builder) {
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
    return TransferProfileType.WHEELCHAIR;
  }

  @Override
  public Speed speed() {
    return speed;
  }

  @Override
  WheelchairPreferences defaultInstance() {
    return DEFAULT;
  }

  @Override
  public float normalizedDistance(AbstractUserPreferences other) {
    float distance = super.normalizedDistance(other);
    if (other instanceof WheelchairPreferences that) {
      distance += speed.normalizedDistance(that.speed);
    }
    return distance;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WheelchairPreferences that)) {
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
      .appendToString(ToStringBuilder.of(WheelchairPreferences.class))
      .addObj("speed", speed, DEFAULT.speed)
      .toString();
  }

  public static class Builder
    extends AbstractUserPreferences.Builder<WheelchairPreferences, Builder>
  {

    private Speed speed;

    public Builder(WheelchairPreferences original) {
      super(original);
      this.speed = original.speed;
    }

    public Builder withSpeed(Speed speed) {
      this.speed = speed;
      return this;
    }

    @Override
    WheelchairPreferences doBuild() {
      return new WheelchairPreferences(this);
    }
  }
}
