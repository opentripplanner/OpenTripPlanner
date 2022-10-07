package org.opentripplanner.util.lang;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CollectionUtils {

  /**
   * Combine a number of list into a single one.
   */
  @SafeVarargs
  public static <T> List<T> combine(Collection<T>... lists) {
    return Arrays.stream(lists).flatMap(Collection::stream).toList();
  }
}
