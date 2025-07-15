package org.opentripplanner.utils.lang;

import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * A box around a mutable value reference. This can be used inside a lambda or passed into
 * a function.
 * @param <T> the type of the wrapped value.
 */
public class Box<T> {

  private T value;

  private Box(T value) {
    this.value = value;
  }

  public Box() {
    this(null);
  }

  public static <T> Box<T> empty() {
    return new Box<>();
  }

  public static <T> Box<T> of(T value) {
    return new Box<>(value);
  }

  @Nullable
  public T get() {
    return value;
  }

  public void set(@Nullable T value) {
    this.value = value;
  }

  public void modify(@Nullable Function<T, T> body) {
    this.value = body.apply(value);
  }

  public boolean isEmpty() {
    return value == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Box<?> box = (Box<?>) o;
    return Objects.equals(value, box.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "[" + value + ']';
  }
}
