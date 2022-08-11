package org.opentripplanner.common.model;

/**
 * An ordered objects of three (the same type)
 *
 * @param <E>
 */
public class P3<E> extends T3<E, E, E> {

  private static final long serialVersionUID = 1L;

  public static <E> P3<E> create(E first, E second, E third) {
    return new P3<E>(first, second, third);
  }

  public P3(E first, E second, E third) {
    super(first, second, third);
  }

  public P3(E[] entries) {
    super(entries[0], entries[1], entries[2]);
    if (entries.length != 3) {
      throw new IllegalArgumentException("This only takes arrays of 3 arguments");
    }
  }

  public String toString() {
    return "P3(" + first + ", " + second + ", " + third + ")";
  }
}