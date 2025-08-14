package org.opentripplanner.utils.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SetUtils {

  /**
   * Combine a number of collections into a single Set.
   */
  @SafeVarargs
  public static <T> Set<T> combine(Collection<T>... colls) {
    return Arrays.stream(colls).flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Return the set of values that are present in _all_ input sets.
   */
  public static <T> Set<T> intersection(Collection<Set<T>> colls) {
    var list = List.copyOf(colls);
    if (list.isEmpty()) {
      return Set.of();
    } else if (list.size() == 1) {
      return list.getFirst();
    } else {
      return list
        .stream()
        .skip(1)
        .collect(() -> new HashSet<>(list.getFirst()), Set::retainAll, Set::retainAll);
    }
  }
}
