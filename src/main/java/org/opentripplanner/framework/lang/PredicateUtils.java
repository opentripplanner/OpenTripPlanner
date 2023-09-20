package org.opentripplanner.framework.lang;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility for building predicates to be used for filtering streams.
 */
public class PredicateUtils {

  /**
   * Build a predicate that uses the {@code keyExtractor} to remove any key that has already
   * been seen by this (stateful) predicate.
   * <p>
   * This is useful for removing duplicates from a stream where the key to be compared is not the
   * entity itself but a field of it.
   */
  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
