package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.routing.api.request.RoutingTag;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Configure system related features - a system feature is a non-functional feature. It
 * describes how the system should work, but not change the output of a travel request.
 * <p>
 * Some parameters in this class are related to functional-features, but does not have a clear
 * place where they belong. We should refactor and move these.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE
 */
public class SystemPreferences implements Serializable {

  public static final SystemPreferences DEFAULT = new SystemPreferences();

  private final Set<RoutingTag> tags;
  private final DataOverlayParameters dataOverlay;
  private final boolean geoidElevation;
  private final Duration maxJourneyDuration;

  private SystemPreferences() {
    this.tags = Set.of();
    this.dataOverlay = null;
    this.geoidElevation = false;
    this.maxJourneyDuration = Duration.ofHours(24);
  }

  private SystemPreferences(Builder builder) {
    this.tags = Set.copyOf(builder.tags);
    this.dataOverlay = builder.dataOverlay;
    this.geoidElevation = builder.geoidElevation;
    this.maxJourneyDuration = Objects.requireNonNull(builder.maxJourneyDuration);
  }

  public static SystemPreferences.Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * List of OTP request tags, these are used to cross-cutting concerns like logging and micrometer
   * tags. Currently, all tags are added to all the timer instances for this request.
   */
  public Set<RoutingTag> tags() {
    return tags;
  }

  public DataOverlayParameters dataOverlay() {
    return dataOverlay;
  }

  /** Whether to apply the ellipsoid→geoid offset to all elevations in the response */
  public boolean geoidElevation() {
    return geoidElevation;
  }

  public Duration maxJourneyDuration() {
    return maxJourneyDuration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SystemPreferences that = (SystemPreferences) o;
    return (
      geoidElevation == that.geoidElevation &&
      tags.equals(that.tags) &&
      Objects.equals(dataOverlay, that.dataOverlay) &&
      maxJourneyDuration.equals(that.maxJourneyDuration)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags, dataOverlay, geoidElevation, maxJourneyDuration);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SystemPreferences.class)
      .addCol("tags", tags, DEFAULT.tags)
      .addObj("dataOverlay", dataOverlay, DEFAULT.dataOverlay)
      .addBoolIfTrue("geoidElevation", geoidElevation)
      .addDuration("maxJourneyDuration", maxJourneyDuration, DEFAULT.maxJourneyDuration)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final SystemPreferences original;
    private final List<RoutingTag> tags = new ArrayList<>();
    private DataOverlayParameters dataOverlay;
    private boolean geoidElevation;
    private Duration maxJourneyDuration;

    public Builder(SystemPreferences original) {
      this.original = original;
      this.tags.addAll(original.tags);
      this.dataOverlay = original.dataOverlay;
      this.geoidElevation = original.geoidElevation;
      this.maxJourneyDuration = original.maxJourneyDuration;
    }

    public SystemPreferences original() {
      return original;
    }

    public Builder addTags(Collection<RoutingTag> tags) {
      this.tags.addAll(tags);
      return this;
    }

    public Builder withDataOverlay(DataOverlayParameters dataOverlay) {
      this.dataOverlay = dataOverlay;
      return this;
    }

    public Builder withGeoidElevation(boolean geoidElevation) {
      this.geoidElevation = geoidElevation;
      return this;
    }

    public Builder withMaxJourneyDuration(Duration maxJourneyDuration) {
      this.maxJourneyDuration = maxJourneyDuration;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public SystemPreferences build() {
      var value = new SystemPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
