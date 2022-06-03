package org.opentripplanner.util.lang;

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
}
