package org.opentripplanner.framework.lang;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * This array builder is used to minimize the creation of new objects (arrays). It takes an array as base,
 * the original array. A new array is created only if there are differences.
 * <p>
 * A common case is that one original is updated several times. In this case, you can use the
 * {@link #build(Object[])} method to also make sure that the existing update is reused (deduplicated).
 * <p>
 * Arrays are mutable, so be careful as this class helps you reuse the original if it has the same
 * values. It protects the original while in scope, but you should only use it if you do not
 * modify the original or the result on the outside. This builder does not help protect the arrays.
 */
public final class MemEfficientArrayBuilder<T> {

  private final T[] original;
  private T[] array = null;

  private MemEfficientArrayBuilder(@Nonnull T[] original) {
    this.original = Objects.requireNonNull(original);
  }

  /**
   * Create a new array with the same size and values as the original.
   */
  public static <T> MemEfficientArrayBuilder<T> of(T[] original) {
    return new MemEfficientArrayBuilder<>(original);
  }

  /**
   * The size of the original and new array under construction.
   */
  public int size() {
    return original.length;
  }

  /**
   * Set the value at the given index.
   */
  public MemEfficientArrayBuilder<T> with(int index, T value) {
    if (isNotModified()) {
      if (value == original[index]) {
        return this;
      }
      array = Arrays.copyOf(original, original.length);
    } else if (value == array[index]) {
      return this;
    }
    array[index] = value;
    return this;
  }

  /**
   * Return the value at the given index from the original array.
   */
  public T original(int index) {
    return original[index];
  }

  /**
   * Return the new value or fallback to the original value at the given index.
   */
  public T getOrOriginal(int index) {
    return isNotModified() ? original[index] : array[index];
  }

  /**
   * There are no changes compared to the original array so far.
   */
  public boolean isNotModified() {
    return array == null;
  }

  /**
   * Build a new array.
   * <ol>
   *   <li>If no modifications exist the original array is returned</li>
   *   <li>If the new array equals the candidate the candidate is returned</li>
   *   <li>If not, a new array is returned</li>
   * </ol>
   */
  public T[] build(T[] candidate) {
    if (isNotModified()) {
      return original;
    }
    return Arrays.equals(candidate, array) ? candidate : array;
  }

  /**
   * Create a new array or return the original [if not modified]
   */
  public T[] build() {
    return isNotModified() ? original : array;
  }
}
