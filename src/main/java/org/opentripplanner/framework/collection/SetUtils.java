package org.opentripplanner.framework.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class SetUtils {

  /**
   * Combine a number of collections into a single Set.
   */
  @SafeVarargs
  public static <T> Set<T> combine(Collection<T>... colls) {
    return Arrays.stream(colls).flatMap(Collection::stream).collect(Collectors.toSet());
  }
}
