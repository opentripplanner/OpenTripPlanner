package org.opentripplanner.routing.api.request.preference;

import static java.time.Duration.ofMinutes;

import java.io.Serializable;
import java.time.Duration;
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

  private static final TimeAndCostPenaltyForEnum<StreetMode> DEFAULT_TIME_AND_COST =
    createDefaultCarPenalty();

  public static final AccessEgressPreferences DEFAULT = new AccessEgressPreferences();

  private final TimeAndCostPenaltyForEnum<StreetMode> penalty;
  private final DurationForEnum<StreetMode> maxDuration;
  private final MaxStopCountLimit maxStopCountLimit;

  private AccessEgressPreferences() {
    this.maxDuration = durationForStreetModeOf(ofMinutes(45));
    this.penalty = DEFAULT_TIME_AND_COST;
    this.maxStopCountLimit = new MaxStopCountLimit();
  }

  private AccessEgressPreferences(Builder builder) {
    this.maxDuration = builder.maxDuration;
    this.penalty = builder.penalty;
    this.maxStopCountLimit = builder.maxStopCountLimit;
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

  public MaxStopCountLimit maxStopCountLimit() {
    return maxStopCountLimit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AccessEgressPreferences that = (AccessEgressPreferences) o;
    return (
      penalty.equals(that.penalty) &&
      maxDuration.equals(that.maxDuration) &&
      maxStopCountLimit.equals(that.maxStopCountLimit)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(penalty, maxDuration, maxStopCountLimit);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AccessEgressPreferences.class)
      .addObj("penalty", penalty, DEFAULT.penalty)
      .addObj("maxDuration", maxDuration, DEFAULT.maxDuration)
      .addObj("maxStopCount", maxStopCountLimit, DEFAULT.maxStopCountLimit)
      .toString();
  }

  public static class Builder {

    private final AccessEgressPreferences original;
    private TimeAndCostPenaltyForEnum<StreetMode> penalty;
    private DurationForEnum<StreetMode> maxDuration;
    private MaxStopCountLimit maxStopCountLimit;

    public Builder(AccessEgressPreferences original) {
      this.original = original;
      this.maxDuration = original.maxDuration;
      this.penalty = original.penalty;
      this.maxStopCountLimit = original.maxStopCountLimit;
    }

    public Builder withMaxDuration(Consumer<DurationForEnum.Builder<StreetMode>> body) {
      this.maxDuration = this.maxDuration.copyOf().apply(body).build();
      return this;
    }

    /** Utility method to simplify config parsing */
    public Builder withMaxDuration(Duration defaultValue, Map<StreetMode, Duration> values) {
      return withMaxDuration(b -> b.withDefault(defaultValue).withValues(values));
    }

    public Builder withMaxStopCount(Consumer<MaxStopCountLimit.Builder> body) {
      this.maxStopCountLimit = this.maxStopCountLimit.copyOf().apply(body).build();
      return this;
    }

    public Builder withMaxStopCount(
      int defaultMaxStopCount,
      Map<StreetMode, Integer> maxStopCountForMode
    ) {
      return withMaxStopCount(b ->
        b.withDefaultLimit(defaultMaxStopCount).withLimitsForModes(maxStopCountForMode)
      );
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

    var carPenalty = TimeAndCostPenalty.of(TimePenalty.of(ofMinutes(20), 2f), 1.5);
    for (var it : StreetMode.values()) {
      // Apply car-penalty to all car modes used in access/egress. Car modes(CAR) used in direct street
      // routing and car modes used when you bring the car with you onto transit should be excluded. The
      // penalty should also be applied to modes used in access, egress AND direct (CAR_TO_PARK and
      // CAR_RENTAL). This is not ideal, since we get an unfair comparison in the itinerary filters. We will
      // live with this for now, but might revisit it later.
      if (
        it.includesDriving() && (it.accessAllowed() || it.egressAllowed()) && it != StreetMode.CAR
      ) {
        penaltyBuilder.with(it, carPenalty);
      }
    }

    return penaltyBuilder.build();
  }
}
