package org.opentripplanner.framework.collection;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class CollectionUtils {

  /**
   * A null-safe version of toString() for a collections.
   * <p>
   * If the collection is {@code null} the given {@code nullText} is returned.
   * <p>
   * All elements are also converted to a sting using the {@code toString()} method or if
   * {@code null} to the given {@code nullText}.
   * <p>
   * If the collection is a set, but not a SortedSet then the elements are sorted. This is done
   * to return the elements in a deterministic manner, which is important if this is used in
   * for example a unit-test.
   * <p>
   * The final result string examples: {@code "[]", "[a]", "[a, b, c]"}
   */
  public static <T> String toString(@Nullable Collection<T> c, String nullText) {
    if (c == null) {
      return nullText;
    }
    var stream = c.stream().map(it -> it == null ? nullText : it.toString());
    if (c instanceof Set && !(c instanceof SortedSet<T>)) {
      stream = stream.sorted();
    }
    return stream.collect(Collectors.joining(", ", "[", "]"));
  }
}
