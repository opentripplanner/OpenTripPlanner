package org.opentripplanner.transit.transfer.regular.api;

import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
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
public final class WalkPreferences extends AbstractUserPreferences {

  public static final WalkPreferences DEFAULT = new WalkPreferences();

  private WalkPreferences() {
    super(Speed.ofMetersPerSecond(1.35), Reluctance.of(2.0), StreetSegmentTypeUsage.ALLOWED);
  }

  private WalkPreferences(Builder builder) {
    super(builder);
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  AbstractUserPreferences defaultInstance() {
    return DEFAULT;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WalkPreferences)) {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return super.appendToString(ToStringBuilder.of(WalkPreferences.class)).toString();
  }

  @Override
  public float normalizedDistance(Object other) {
    return 0;
  }

  public static class Builder extends AbstractUserPreferences.Builder<WalkPreferences, Builder> {

    public Builder(WalkPreferences original) {
      super(original);
    }

    @Override
    WalkPreferences doBuild() {
      return new WalkPreferences(this);
    }
  }
}
