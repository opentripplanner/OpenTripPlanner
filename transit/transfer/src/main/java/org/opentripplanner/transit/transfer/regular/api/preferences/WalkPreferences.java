package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;
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
  implements WithStairs
{

  public static final WalkPreferences DEFAULT = new WalkPreferences();

  private final StreetSegmentTypeUsage stairs;

  private WalkPreferences() {
    super(Speed.ofMetersPerSecond(1.35), Reluctance.of(2.0));
    this.stairs = StreetSegmentTypeUsage.ALLOWED;
  }

  private WalkPreferences(Builder builder) {
    super(builder);
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
   * walk preferences, the defaults if there is no parent.
   */
  public static WalkPreferences inheritedFrom(@Nullable AbstractUserPreferences<?> parent) {
    if (parent == null) {
      return DEFAULT;
    }
    if (parent instanceof WalkPreferences walk) {
      return walk;
    }
    return of().inheritFrom(parent).build();
  }

  @Override
  public TransferProfileType type() {
    return TransferProfileType.WALK;
  }

  @Override
  public StreetSegmentTypeUsage stairs() {
    return stairs;
  }

  @Override
  WalkPreferences defaultInstance() {
    return DEFAULT;
  }

  @Override
  public float normalizedDistance(AbstractUserPreferences other) {
    float distance = super.normalizedDistance(other);
    if (other instanceof WalkPreferences that) {
      distance += stairs.normalizedDistance(that.stairs);
    }
    return distance;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WalkPreferences that)) {
      return false;
    }
    return super.equals(obj) && stairs == that.stairs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), stairs);
  }

  @Override
  public String toString() {
    return super
      .appendToString(ToStringBuilder.of(WalkPreferences.class))
      .addObj("stairs", stairs, DEFAULT.stairs)
      .toString();
  }

  public static class Builder extends AbstractUserPreferences.Builder<WalkPreferences, Builder> {

    private StreetSegmentTypeUsage stairs;

    public Builder(WalkPreferences original) {
      super(original);
      this.stairs = original.stairs;
    }

    public Builder withStairs(StreetSegmentTypeUsage stairs) {
      this.stairs = stairs;
      return this;
    }

    @Override
    public Builder inheritFrom(AbstractUserPreferences<?> parent) {
      super.inheritFrom(parent);
      if (parent instanceof WithStairs withStairs) {
        this.stairs = withStairs.stairs();
      }
      return this;
    }

    @Override
    WalkPreferences doBuild() {
      return new WalkPreferences(this);
    }
  }
}
