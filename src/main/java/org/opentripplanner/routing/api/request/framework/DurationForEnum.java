package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import org.opentripplanner.util.lang.ValueObjectToStringBuilder;

/**
 * This class is used to store a {@link Duration} value for each of the enum type values.
 * If an enum value does not exist, it falls back to the default value.
 */

public class DurationForEnum<E extends Enum<?>> {

  private final Class<E> type;
  private final Duration defaultValue;
  private final Duration[] valueForEnum;

  private DurationForEnum(DurationForEnum<E> other) {
    this.type = other.type;
    this.defaultValue = other.defaultValue;
    this.valueForEnum = Arrays.copyOf(other.valueForEnum, other.valueForEnum.length);
  }

  public DurationForEnum(Class<E> type, Duration defaultValue) {
    this(type, defaultValue, Map.of());
  }

  public DurationForEnum(Class<E> type, Duration defaultValue, Map<E, Duration> values) {
    this.type = type;
    this.defaultValue = defaultValue;
    this.valueForEnum = new Duration[type.getEnumConstants().length];
    Arrays.fill(valueForEnum, defaultValue);
    for (Map.Entry<E, Duration> e : values.entrySet()) {
      this.valueForEnum[e.getKey().ordinal()] = e.getValue();
    }
  }

  public Duration defaultValue() {
    return defaultValue;
  }

  public Duration valueOf(E type) {
    return valueForEnum[type.ordinal()];
  }

  public DurationForEnum<E> copyOf() {
    return new DurationForEnum<>(this);
  }

  @Override
  public String toString() {
    var builder = ValueObjectToStringBuilder
      .of()
      .addText("DurationFor" + type.getSimpleName() + "{")
      .addText("default:")
      .addDuration(defaultValue);

    for (int i = 0; i < valueForEnum.length; ++i) {
      if (!defaultValue.equals(valueForEnum[i])) {
        E e = type.getEnumConstants()[i];
        builder.addText(", ").addText(e.name()).addText(":").addDuration(valueForEnum[i]);
      }
    }
    return builder.addText("}").toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DurationForEnum<?> that = (DurationForEnum<?>) o;

    return Arrays.equals(valueForEnum, that.valueForEnum);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(valueForEnum);
  }
}
