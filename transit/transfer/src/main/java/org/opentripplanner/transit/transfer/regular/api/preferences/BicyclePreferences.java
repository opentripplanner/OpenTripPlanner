package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The preferences of the bicycle transfers: speed, reluctance and stairs for a traveler that rides
 * a bicycle, and pushes it on stairs. The values are normalized(rounded) so the class can used as a
 * cache key.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class BicyclePreferences
  extends AbstractUserPreferences<BicyclePreferences>
  implements WithSpeed, WithStairs
{

  public static final BicyclePreferences DEFAULT = new BicyclePreferences();

  private final Speed speed;
  private final StreetSegmentTypeUsage stairs;

  private BicyclePreferences() {
    super(Reluctance.of(2.0));
    this.speed = Speed.ofMetersPerSecond(5.0);
    this.stairs = StreetSegmentTypeUsage.ALLOWED;
  }

  private BicyclePreferences(Builder builder) {
    super(builder);
    this.speed = Objects.requireNonNull(builder.speed);
    this.stairs = Objects.requireNonNull(builder.stairs);
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * The preferences a profile starts with: the defaults, with the values the {@code parent} has in
   * common, see {@link AbstractUserPreferences.Builder#inheritFrom}. The parent itself if it is
   * bicycle preferences, the defaults if there is no parent.
   */
  public static BicyclePreferences inheritedFrom(@Nullable AbstractUserPreferences<?> parent) {
    if (parent == null) {
      return DEFAULT;
    }
    if (parent instanceof BicyclePreferences bicycle) {
      return bicycle;
    }
    return of().inheritFrom(parent).build();
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
  public StreetSegmentTypeUsage stairs() {
    return stairs;
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
      distance += stairs.normalizedDistance(that.stairs);
    }
    return distance;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BicyclePreferences that)) {
      return false;
    }
    return super.equals(obj) && speed.equals(that.speed) && stairs == that.stairs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), speed, stairs);
  }

  @Override
  public String toString() {
    return super
      .appendToString(ToStringBuilder.of(BicyclePreferences.class))
      .addObj("speed", speed, DEFAULT.speed)
      .addObj("stairs", stairs, DEFAULT.stairs)
      .toString();
  }

  public static class Builder extends AbstractUserPreferences.Builder<BicyclePreferences, Builder> {

    private Speed speed;
    private StreetSegmentTypeUsage stairs;

    public Builder(BicyclePreferences original) {
      super(original);
      this.speed = original.speed;
      this.stairs = original.stairs;
    }

    public Builder withSpeed(Speed speed) {
      this.speed = speed;
      return this;
    }

    public Builder withStairs(StreetSegmentTypeUsage stairs) {
      this.stairs = stairs;
      return this;
    }

    @Override
    public Builder inheritFrom(AbstractUserPreferences<?> parent) {
      super.inheritFrom(parent);
      if (parent instanceof WithSpeed withSpeed) {
        this.speed = withSpeed.speed();
      }
      if (parent instanceof WithStairs withStairs) {
        this.stairs = withStairs.stairs();
      }
      return this;
    }

    @Override
    BicyclePreferences doBuild() {
      return new BicyclePreferences(this);
    }
  }
}
