package org.opentripplanner.framework.lang;

import java.util.function.Function;
import java.util.function.Supplier;
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

  /**
   * Similar to {@link #ifNotNull(Object, Object)}, but take a function to access the
   * entity field. The given {@code defaultValue} can be {@code null}.
   * */
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

  /**
   * Get the value or {@code null}, ignore any exceptions. This is useful if you must traverse
   * a long call-chain like {@code a.b().c().d()...} when e.g. logging.
   */
  public static <T> T safeGetOrNull(Supplier<T> body) {
    try {
      return body.get();
    } catch (Exception ignore) {
      return null;
    }
  }

  public static <T> T requireNotInitialized(T oldValue, T newValue) {
    return requireNotInitialized(null, oldValue, newValue);
  }

  public static <T> T requireNotInitialized(@Nullable String name, T oldValue, T newValue) {
    if (oldValue != null) {
      throw new IllegalStateException(
        "Field%s is already set! Old value: %s, new value: %s.".formatted(
            (name == null ? "" : " " + name),
            oldValue,
            newValue
          )
      );
    }
    return newValue;
  }

  /**
   * Map an object to a string. This is null safe and a empty string is returned if the given input
   * is {@code null}.
   */
  public static String toString(Object object) {
    return object == null ? "" : object.toString();
  }
}
