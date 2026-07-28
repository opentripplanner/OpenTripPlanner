package org.opentripplanner.utils.collection;

import java.util.stream.Stream;

public class IterableUtils {

  /**
   * Returns an iterable from the given stream.
   */
  public static <T> Iterable<T> of(Stream<T> stream) {
    return stream::iterator;
  }
}
