package org.opentripplanner.routing.api.request.preference;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class is used for storing default and mode specific stop count limits for access and egress
 * routing.
 */
public class MaxStopCountLimit {

  private static final int DEFAULT_LIMIT = 500;
  private static final Map<StreetMode, Integer> DEFAULT_FOR_MODE = Map.of();

  public static final MaxStopCountLimit DEFAULT = new MaxStopCountLimit();

  private final int defaultLimit;
  private final Map<StreetMode, Integer> limitForMode;

  public MaxStopCountLimit() {
    this.defaultLimit = DEFAULT_LIMIT;
    this.limitForMode = DEFAULT_FOR_MODE;
  }

  MaxStopCountLimit(Builder builder) {
    this.defaultLimit = IntUtils.requireNotNegative(builder.defaultLimit());
    this.limitForMode = builder.copyValueForEnum();
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Get the max stop count limit for mode. If no custom value is defined, default is used.
   */
  public int limitForMode(StreetMode mode) {
    return limitForMode.getOrDefault(mode, defaultLimit);
  }

  public int defaultLimit() {
    return defaultLimit;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(MaxStopCountLimit.class)
      .addNum("defaultLimit", defaultLimit, DEFAULT_LIMIT)
      .addObj("limitForMode", limitForMode, DEFAULT_FOR_MODE)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    var that = (MaxStopCountLimit) o;

    return (defaultLimit == that.defaultLimit && limitForMode.equals(that.limitForMode));
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultLimit, limitForMode);
  }

  private Map<StreetMode, Integer> limitForMode() {
    return limitForMode;
  }

  private boolean hasCustomLimit(StreetMode mode) {
    return limitForMode.containsKey(mode);
  }

  public static class Builder {

    private final MaxStopCountLimit original;
    private int defaultLimit;
    private Map<StreetMode, Integer> limitForMode = null;

    Builder(MaxStopCountLimit original) {
      this.original = original;
      this.defaultLimit = original.defaultLimit();
    }

    int defaultLimit() {
      return defaultLimit;
    }

    public Builder withDefaultLimit(int defaultLimit) {
      this.defaultLimit = defaultLimit;
      return this;
    }

    public Builder with(StreetMode mode, Integer limit) {
      // Lazy initialize the valueForEnum map
      if (limitForMode == null) {
        limitForMode = new EnumMap<>(StreetMode.class);
        for (StreetMode it : StreetMode.values()) {
          if (original.hasCustomLimit(it)) {
            limitForMode.put(it, original.limitForMode(it));
          }
        }
      }
      limitForMode.put(mode, limit);
      return this;
    }

    public Builder withValues(Map<StreetMode, Integer> values) {
      for (Map.Entry<StreetMode, Integer> e : values.entrySet()) {
        with(e.getKey(), e.getValue());
      }
      return this;
    }

    /**
     * Build a copy of the current values, excluding the defaultLimit from the map. This
     * ensures equality and make a defencive copy of the builder values. Hence, the builder
     * can be used to generate new values if desired.
     * */
    Map<StreetMode, Integer> copyValueForEnum() {
      if (limitForMode == null) {
        // The limitForMode is protected and should never be mutated, so we can reuse it
        return original.limitForMode();
      }

      var copy = new EnumMap<StreetMode, Integer>(StreetMode.class);
      for (Map.Entry<StreetMode, Integer> it : limitForMode.entrySet()) {
        if (defaultLimit != it.getValue()) {
          copy.put(it.getKey(), it.getValue());
        }
      }
      return copy;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public MaxStopCountLimit build() {
      var it = new MaxStopCountLimit(this);

      // Return original if not change, subscriber is not notified
      if (original != null && original.equals(it)) {
        return original;
      }

      return it;
    }
  }
}
