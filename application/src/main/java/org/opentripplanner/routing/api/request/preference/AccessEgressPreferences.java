package org.opentripplanner.routing.api.request.preference;

import static java.time.Duration.ofMinutes;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenaltyForEnum;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Preferences for access/egress routing on street network
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class AccessEgressPreferences implements Serializable {

  private static final TimeAndCostPenaltyForEnum<StreetMode> DEFAULT_TIME_AND_COST = createDefaultCarPenalty();

  public static final AccessEgressPreferences DEFAULT = new AccessEgressPreferences();

  private final TimeAndCostPenaltyForEnum<StreetMode> penalty;
  private final DurationForEnum<StreetMode> maxDuration;
  private final int defaultMaxStopCount;
  private final Map<StreetMode, Integer> maxStopCountForMode;

  private AccessEgressPreferences() {
    this.maxDuration = durationForStreetModeOf(ofMinutes(45));
    this.penalty = DEFAULT_TIME_AND_COST;
    this.defaultMaxStopCount = 500;
    this.maxStopCountForMode = Map.of();
  }

  private AccessEgressPreferences(Builder builder) {
    this.maxDuration = builder.maxDuration;
    this.penalty = builder.penalty;
    this.defaultMaxStopCount = builder.defaultMaxStopCount;
    this.maxStopCountForMode = Collections.unmodifiableMap(builder.maxStopCountForMode);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public TimeAndCostPenaltyForEnum<StreetMode> penalty() {
    return penalty;
  }

  public DurationForEnum<StreetMode> maxDuration() {
    return maxDuration;
  }

  public int defaultMaxStopCount() {
    return defaultMaxStopCount;
  }

  public Map<StreetMode, Integer> maxStopCountForMode() {
    return maxStopCountForMode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessEgressPreferences that = (AccessEgressPreferences) o;
    return (
      penalty.equals(that.penalty) &&
      maxDuration.equals(that.maxDuration) &&
      defaultMaxStopCount == that.defaultMaxStopCount &&
      maxStopCountForMode.equals(that.maxStopCountForMode)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(penalty, maxDuration, defaultMaxStopCount, maxStopCountForMode);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(AccessEgressPreferences.class)
      .addObj("penalty", penalty, DEFAULT.penalty)
      .addObj("maxDuration", maxDuration, DEFAULT.maxDuration)
      .addObj("defaultMaxStopCount", defaultMaxStopCount, DEFAULT.defaultMaxStopCount)
      .addObj("maxStopCountForMode", maxStopCountForMode, DEFAULT.maxStopCountForMode)
      .toString();
  }

  public static class Builder {

    private final AccessEgressPreferences original;
    private TimeAndCostPenaltyForEnum<StreetMode> penalty;
    private DurationForEnum<StreetMode> maxDuration;
    private Map<StreetMode, Integer> maxStopCountForMode;
    private int defaultMaxStopCount;

    public Builder(AccessEgressPreferences original) {
      this.original = original;
      this.maxDuration = original.maxDuration;
      this.penalty = original.penalty;
      this.defaultMaxStopCount = original.defaultMaxStopCount;
      this.maxStopCountForMode = original.maxStopCountForMode;
    }

    public Builder withMaxDuration(Consumer<DurationForEnum.Builder<StreetMode>> body) {
      this.maxDuration = this.maxDuration.copyOf().apply(body).build();
      return this;
    }

    /** Utility method to simplify config parsing */
    public Builder withMaxDuration(Duration defaultValue, Map<StreetMode, Duration> values) {
      return withMaxDuration(b -> b.withDefault(defaultValue).withValues(values));
    }

    public Builder withMaxStopCount(
      int defaultMaxStopCount,
      Map<StreetMode, Integer> maxStopCountForMode
    ) {
      this.defaultMaxStopCount = defaultMaxStopCount;
      this.maxStopCountForMode = maxStopCountForMode;
      return this;
    }

    public Builder withPenalty(Consumer<TimeAndCostPenaltyForEnum.Builder<StreetMode>> body) {
      this.penalty = this.penalty.copyOf().apply(body).build();
      return this;
    }

    /** Utility method to simplify config parsing */
    public Builder withPenalty(Map<StreetMode, TimeAndCostPenalty> values) {
      return withPenalty(b -> b.withValues(values));
    }

    public AccessEgressPreferences original() {
      return original;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    AccessEgressPreferences build() {
      var value = new AccessEgressPreferences(this);
      return original.equals(value) ? original : value;
    }
  }

  private static DurationForEnum<StreetMode> durationForStreetModeOf(Duration defaultValue) {
    return DurationForEnum.of(StreetMode.class).withDefault(defaultValue).build();
  }

  private static TimeAndCostPenaltyForEnum<StreetMode> createDefaultCarPenalty() {
    var penaltyBuilder = TimeAndCostPenaltyForEnum.of(StreetMode.class);

    var flexDefaultPenalty = TimeAndCostPenalty.of(TimePenalty.of(ofMinutes(10), 1.3f), 1.3);
    penaltyBuilder.with(StreetMode.FLEXIBLE, flexDefaultPenalty);

    // Add penalty to all car variants with access and/or egress.
    var carPenalty = TimeAndCostPenalty.of(TimePenalty.of(ofMinutes(20), 2f), 1.5);
    for (var it : StreetMode.values()) {
      if (it.includesDriving() && (it.accessAllowed() || it.egressAllowed())) {
        penaltyBuilder.with(it, carPenalty);
      }
    }

    return penaltyBuilder.build();
  }
}
