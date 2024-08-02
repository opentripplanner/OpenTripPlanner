package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class MappingPreferences implements Serializable {

  public static final MappingPreferences DEFAULT = new MappingPreferences();

  private final Set<MappingFeature> optInFeatures;

  public MappingPreferences() {
    this.optInFeatures = EnumSet.noneOf(MappingFeature.class);
  }

  private MappingPreferences(Builder builder) {
    this.optInFeatures = Set.copyOf(builder.optInFeatures);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MappingPreferences that = (MappingPreferences) o;
    return optInFeatures.equals(that.optInFeatures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(optInFeatures);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(MappingPreferences.class)
      .addObj("optInFeatures", optInFeatures, DEFAULT.optInFeatures)
      .toString();
  }

  public boolean optsInto(MappingFeature feature) {
    return optInFeatures.contains(feature);
  }

  public static class Builder {

    private final MappingPreferences original;
    private Set<MappingFeature> optInFeatures = EnumSet.noneOf(MappingFeature.class);

    public Builder(MappingPreferences original) {
      this.original = original;
      this.optInFeatures = original.optInFeatures;
    }

    public MappingPreferences original() {
      return original;
    }

    public Builder withOptInFeatures(Set<MappingFeature> optInFeatures) {
      this.optInFeatures = optInFeatures;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public MappingPreferences build() {
      var value = new MappingPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
