package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * A map of {@link TimeAndCost} indexed by enum type {@code T}.
 * <p>
 * This class is immutable and thread-safe.
 */

public class TimeAndCostPenaltyForEnum<E extends Enum<E>> implements Serializable {

  private final Class<E> type;
  private final Map<E, TimeAndCostPenalty> values;

  public TimeAndCostPenaltyForEnum(Class<E> type) {
    this.type = Objects.requireNonNull(type);
    this.values = Map.of();
  }

  private TimeAndCostPenaltyForEnum(TimeAndCostPenaltyForEnum.Builder<E> builder) {
    this.type = Objects.requireNonNull(builder.type());
    this.values = builder.valuesCopy();
  }

  public static <S extends Enum<S>> TimeAndCostPenaltyForEnum<S> ofDefault(Class<S> type) {
    return new TimeAndCostPenaltyForEnum<S>(type);
  }

  public static <S extends Enum<S>> TimeAndCostPenaltyForEnum.Builder<S> of(Class<S> type) {
    return ofDefault(type).copyOf();
  }

  public TimeAndCostPenaltyForEnum.Builder<E> copyOf() {
    return new TimeAndCostPenaltyForEnum.Builder<>(this);
  }

  Class<E> type() {
    return type;
  }

  public TimeAndCostPenalty valueOf(E type) {
    return values.getOrDefault(type, TimeAndCostPenalty.ZERO);
  }

  public boolean isSet(E key) {
    return values.containsKey(key);
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.of(TimeAndCostPenaltyForEnum.class);

    var sortedEntryList = values
      .entrySet()
      .stream()
      .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
      .toList();

    for (Map.Entry<E, TimeAndCostPenalty> e : sortedEntryList) {
      builder.addObj(e.getKey().name(), e.getValue());
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimeAndCostPenaltyForEnum<?> that = (TimeAndCostPenaltyForEnum<?>) o;

    return type.equals(that.type) && values.equals(that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, values);
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder<E extends Enum<E>> {

    private final TimeAndCostPenaltyForEnum<E> original;
    private Map<E, TimeAndCostPenalty> values = null;

    Builder(TimeAndCostPenaltyForEnum<E> original) {
      this.original = original;
    }

    Class<E> type() {
      return original.type();
    }

    public Builder<E> with(E key, TimeAndCostPenalty value) {
      // Lazy initialize the valueForEnum map
      if (values == null) {
        values = original.isEmpty() ? new EnumMap<>(original.type) : new EnumMap<>(original.values);
      }
      if (TimeAndCostPenalty.ZERO.equals(value)) {
        values.remove(key);
      } else {
        values.put(key, value);
      }
      return this;
    }

    /**
     * Build a copy of the current values. This will create a defensive copy of the builder values.
     * Hence, the builder can be used to generate new values if desired.
     * */
    Map<E, TimeAndCostPenalty> valuesCopy() {
      return values == null ? original.values : new EnumMap<>(values);
    }

    public Builder<E> withValues(Map<E, TimeAndCostPenalty> values) {
      for (Map.Entry<E, TimeAndCostPenalty> e : values.entrySet()) {
        with(e.getKey(), e.getValue());
      }
      return this;
    }

    public Builder<E> apply(Consumer<Builder<E>> body) {
      body.accept(this);
      return this;
    }

    public TimeAndCostPenaltyForEnum<E> build() {
      var it = new TimeAndCostPenaltyForEnum<>(this);
      // Return original if not changed
      return original.equals(it) ? original : it;
    }
  }
}
