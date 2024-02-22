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

  // Do not expose the 'values' outside the class, the EnumMap is mutable, and this class
  // should be immutable.
  private final Map<E, TimeAndCostPenalty> values;

  public TimeAndCostPenaltyForEnum(Class<E> type) {
    this.type = Objects.requireNonNull(type);
    this.values = Map.of();
  }

  private TimeAndCostPenaltyForEnum(TimeAndCostPenaltyForEnum.Builder<E> builder) {
    this.type = Objects.requireNonNull(builder.original.type);
    this.values = Objects.requireNonNull(builder.values);
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
    return toString(TimeAndCostPenaltyForEnum.class, values);
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

  private EnumMap<E, TimeAndCostPenalty> copyValues() {
    return values.isEmpty() ? new EnumMap<>(type) : new EnumMap<>(values);
  }

  /**
   * Convert the values to an {@link EnumMap}.
   */
  public EnumMap<E, TimeAndCostPenalty> asEnumMap() {
    return copyValues();
  }

  private static <F extends Enum<F>> String toString(
    Class<?> clazz,
    Map<F, TimeAndCostPenalty> values
  ) {
    var builder = ToStringBuilder.of(clazz);

    var sortedEntryList = values
      .entrySet()
      .stream()
      .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
      .toList();

    for (Map.Entry<F, TimeAndCostPenalty> e : sortedEntryList) {
      builder.addObj(e.getKey().name(), e.getValue());
    }
    return builder.toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder<E extends Enum<E>> {

    private final TimeAndCostPenaltyForEnum<E> original;
    private final EnumMap<E, TimeAndCostPenalty> values;

    Builder(TimeAndCostPenaltyForEnum<E> original) {
      this.original = original;
      this.values = original.copyValues();
    }

    public Builder<E> with(E key, TimeAndCostPenalty value) {
      Objects.requireNonNull(key);
      if (value == null || value.isEmpty()) {
        values.remove(key);
      } else {
        values.put(key, value);
      }
      return this;
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
      var value = new TimeAndCostPenaltyForEnum<>(this);
      return original.equals(value) ? original : value;
    }

    @Override
    public String toString() {
      return TimeAndCostPenaltyForEnum.toString(TimeAndCostPenaltyForEnum.Builder.class, values);
    }
  }
}
