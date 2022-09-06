package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class DurationForEnumBuilder<E extends Enum<?>> {

  private final Class<E> type;
  private Duration defaultValue;
  private Duration[] valueForEnum;

  @Nullable
  private final DurationForEnum<E> original;

  public DurationForEnumBuilder(
    Class<E> type,
    Duration defaultValue,
    @Nullable DurationForEnum<E> original
  ) {
    this.type = Objects.requireNonNull(type);
    this.defaultValue = defaultValue;
    this.original = original;
    this.valueForEnum = new Duration[type.getEnumConstants().length];
  }

  DurationForEnumBuilder(DurationForEnum<E> original) {
    this(original.type(), original.defaultValue(), original);
    for (E key : type.getEnumConstants()) {
      if (original.isSet(key)) {
        with(key, original.valueOf(key));
      }
    }
  }

  DurationForEnumBuilder(Class<E> type) {
    this(type, null, null);
  }

  Class<E> type() {
    return type;
  }

  Duration defaultValue() {
    return defaultValue;
  }

  Duration[] valueForEnum() {
    return valueForEnum;
  }

  public DurationForEnumBuilder<E> withDefault(Duration defaultValue) {
    assertNotBuilt();
    this.defaultValue = defaultValue;
    return this;
  }

  public DurationForEnumBuilder<E> withDefaultSec(int defaultValue) {
    return withDefault(Duration.ofSeconds(defaultValue));
  }

  public DurationForEnumBuilder<E> with(E key, Duration value) {
    assertNotBuilt();
    this.valueForEnum[key.ordinal()] = value;
    return this;
  }

  public DurationForEnumBuilder<E> withValues(Map<E, Duration> values) {
    for (Map.Entry<E, Duration> e : values.entrySet()) {
      with(e.getKey(), e.getValue());
    }
    return this;
  }

  public DurationForEnum<E> build() {
    var it = new DurationForEnum<>(this);

    // Return original if not change, subscriber is not notified
    if (original != null && original.equals(it)) {
      return original;
    }
    // Clear valueForEnum to avoid changes to original (same array used to avoid copy)
    this.valueForEnum = null;

    return it;
  }

  private void assertNotBuilt() {
    if (valueForEnum == null) {
      throw new IllegalStateException(
        "This builder only support building one instance! It is not allowed to change the " +
        "builder after the build() method is invoked."
      );
    }
  }
}
