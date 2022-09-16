package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;
import org.opentripplanner.util.lang.ObjectUtils;
import org.opentripplanner.util.lang.ValueObjectToStringBuilder;

/**
 * This class is used to store a {@link Duration} value for each of the enum type values.
 * If an enum value does not exist, it falls back to the default value.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE
 */

public class DurationForEnum<E extends Enum<?>> implements Serializable {

  private final Class<E> type;
  private final Duration defaultValue;
  private final Duration[] valueForEnum;

  DurationForEnum(DurationForEnumBuilder<E> builder) {
    this.type = builder.type();
    this.defaultValue = ObjectUtils.ifNotNull(builder.defaultValue(), Duration.ZERO);
    this.valueForEnum = builder.valueForEnum();
    // Set default values to avoid null checks later
    for (int i = 0; i < valueForEnum.length; i++) {
      if (valueForEnum[i] == null) {
        valueForEnum[i] = defaultValue;
      }
    }
  }

  public static <S extends Enum<?>> DurationForEnumBuilder<S> of(Class<S> type) {
    return new DurationForEnumBuilder<>(type);
  }

  public DurationForEnum<E> copyOf(Consumer<DurationForEnumBuilder<E>> body) {
    var builder = new DurationForEnumBuilder<>(this);
    body.accept(builder);
    return builder.build();
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
    return valueForEnum[type.ordinal()];
  }

  public boolean isSet(E key) {
    return valueOf(key) != defaultValue;
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

    return type.equals(that.type) && Arrays.equals(valueForEnum, that.valueForEnum);
  }

  @Override
  public int hashCode() {
    return 31 * type.hashCode() + Arrays.hashCode(valueForEnum);
  }
}
