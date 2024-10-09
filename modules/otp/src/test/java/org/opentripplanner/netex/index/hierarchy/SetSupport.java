package org.opentripplanner.netex.index.hierarchy;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Small utility class with Set support methods for local tests
 */
class SetSupport {

  /**
   * Create a new sorted list of the given values.
   */
  static <E extends Comparable<E>> List<E> sort(Collection<E> values) {
    return values.stream().sorted().collect(Collectors.toList());
  }
}
