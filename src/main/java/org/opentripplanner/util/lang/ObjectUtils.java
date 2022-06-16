package org.opentripplanner.util.lang;

import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Simple generic utility functions.
 */
public class ObjectUtils {

  /**
   * Similar to the {@link java.util.Objects#requireNonNullElse(Object, Object)}, but allow
   * the defaultValue to be {@code null}.
   */
  @Nullable
  public static <T> T ifNotNull(@Nullable T value, @Nullable T defaultValue) {
    return value != null ? value : defaultValue;
  }

  @Nullable
  public static <E, T> T ifNotNull(
    @Nullable E entity,
    Function<E, T> getter,
    @Nullable T defaultValue
  ) {
    if (entity == null) {
      return defaultValue;
    }
    return ifNotNull(getter.apply(entity), defaultValue);
  }
}
