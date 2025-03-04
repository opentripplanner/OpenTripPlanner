package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;
import java.time.Duration;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * This class is used to store a {@link Duration} value for each of the enum type values.
 * If an enum value does not exist, it falls back to the default value.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE
 */
public class DurationForEnum<E extends Enum<E>> implements Serializable {

  private final Class<E> type;
  private final Duration defaultValue;
  private final Map<E, Duration> valueForEnum;

  public DurationForEnum(Class<E> type) {
    this.type = Objects.requireNonNull(type);
    this.defaultValue = Duration.ZERO;
    this.valueForEnum = Map.of();
  }

  DurationForEnum(Builder<E> builder) {
    this.type = Objects.requireNonNull(builder.type());
    this.defaultValue = Objects.requireNonNull(builder.defaultValue());
    this.valueForEnum = builder.copyValueForEnum();
  }

  public static <S extends Enum<S>> Builder<S> of(Class<S> type) {
    return new DurationForEnum<S>(type).copyOf();
  }

  public Builder<E> copyOf() {
    return new Builder<>(this);
  }

  Class<E> type() {
    return type;
  }

  public Duration defaultValue() {
    return defaultValue;
  }

  /**
   * Utility method to get {@link #defaultValue} as an number in unit seconds. Equivalent to
   * {@code (int) defaultValue.toSeconds()}. The downcast is safe since we only allow days, hours,
   * and so on in the duration.
   */
  public int defaultValueSeconds() {
    return (int) defaultValue.toSeconds();
  }

  public Duration valueOf(E type) {
    return valueForEnum.getOrDefault(type, defaultValue);
  }

  public boolean isSet(E key) {
    return !defaultValue.equals(valueOf(key));
  }

  @Override
  public String toString() {
    var builder = ValueObjectToStringBuilder.of()
      .addText("DurationFor" + type.getSimpleName() + "{")
      .addText("default:")
      .addDuration(defaultValue);

    var sortedEntryList = valueForEnum
      .entrySet()
      .stream()
      .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
      .toList();

    for (Map.Entry<E, Duration> e : sortedEntryList) {
      if (!defaultValue.equals(e.getValue())) {
        builder.addText(", ").addText(e.getKey().name()).addText(":").addDuration(e.getValue());
      }
    }
    return builder.addText("}").toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DurationForEnum<?> that = (DurationForEnum<?>) o;

    return (
      type.equals(that.type) &&
      defaultValue.equals(that.defaultValue) &&
      valueForEnum.equals(that.valueForEnum)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, defaultValue, valueForEnum);
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder<E extends Enum<E>> {

    private final DurationForEnum<E> original;
    private Duration defaultValue;
    private EnumMap<E, Duration> valueForEnum = null;

    Builder(DurationForEnum<E> original) {
      this.original = original;
      this.defaultValue = original.defaultValue();
    }

    Class<E> type() {
      return original.type();
    }

    Duration defaultValue() {
      return defaultValue;
    }

    public Builder<E> withDefault(Duration defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder<E> withDefaultSec(int defaultValue) {
      return withDefault(Duration.ofSeconds(defaultValue));
    }

    public Builder<E> with(E key, Duration value) {
      // Lazy initialize the valueForEnum map
      if (valueForEnum == null) {
        valueForEnum = new EnumMap<>(original.type);
        for (E it : original.type.getEnumConstants()) {
          if (original.isSet(it)) {
            valueForEnum.put(it, original.valueOf(it));
          }
        }
      }
      valueForEnum.put(key, value);
      return this;
    }

    /**
     * Build a copy of the current values, excluding the defaultValue from the map. This
     * ensures equality and makes a defensive copy of the builder values. Hence, the builder
     * can be used to generate new values if desired.
     * */
    Map<E, Duration> copyValueForEnum() {
      if (valueForEnum == null) {
        // The valueForEnum is protected and should never be mutated, so we can reuse it
        return original.valueForEnum;
      }

      var copy = new EnumMap<E, Duration>(original.type);
      for (Map.Entry<E, Duration> it : valueForEnum.entrySet()) {
        if (!defaultValue.equals(it.getValue())) {
          copy.put(it.getKey(), it.getValue());
        }
      }
      return copy;
    }

    public Builder<E> withValues(Map<E, Duration> values) {
      for (Map.Entry<E, Duration> e : values.entrySet()) {
        with(e.getKey(), e.getValue());
      }
      return this;
    }

    public Builder<E> apply(Consumer<Builder<E>> body) {
      body.accept(this);
      return this;
    }

    public DurationForEnum<E> build() {
      var it = new DurationForEnum<>(this);

      // Return the original if there are no changes, the subscriber is not notified.
      if (original != null && original.equals(it)) {
        return original;
      }

      return it;
    }
  }
}
