package org.opentripplanner.framework.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class ListUtils {

  /**
   * Combine a number of collections into a single list.
   */
  @SafeVarargs
  public static <T> List<T> combine(Collection<T>... lists) {
    return Arrays.stream(lists).flatMap(Collection::stream).toList();
  }
  @SafeVarargs
  public static <T> List<T> combine(Stream<T>... streams) {
    return Arrays.stream(streams).flatMap(Function.identity()).toList();
  }
}
