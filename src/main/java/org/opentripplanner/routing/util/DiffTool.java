package org.opentripplanner.routing.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import javax.annotation.Nullable;

/**
 * This class provide a method for comparing to lists and create a diff between them.
 * <p>
 */
public final class DiffTool {

  /** This utility class have a private constructor to prevent instantiation. */
  private DiffTool() {}

  /**
   * Compare to collections(left and right) and return a list of differences.
   */
  public static <T> DiffList<T> diff(
    Collection<T> left,
    Collection<T> right,
    Comparator<T> comparator
  ) {
    DiffList<T> result = new DiffList<>();

    Iterator<T> leftIterator = left.stream().sorted(comparator).iterator();
    Iterator<T> rightIterator = right.stream().sorted(comparator).iterator();

    T l = next(leftIterator);
    T r = next(rightIterator);

    while (l != null && r != null) {
      int c = comparator.compare(l, r);
      if (c < 0) {
        result.add(DiffEntry.ofLeft(l));
        l = next(leftIterator);
      } else if (c > 0) {
        result.add(DiffEntry.ofRight(r));
        r = next(rightIterator);
      }
      // c == 0
      else {
        result.add(DiffEntry.ofEqual(l, r));
        l = next(leftIterator);
        r = next(rightIterator);
      }
    }

    while (l != null) {
      result.add(DiffEntry.ofLeft(l));
      l = next(leftIterator);
    }
    while (r != null) {
      result.add(DiffEntry.ofRight(r));
      r = next(rightIterator);
    }

    return result;
  }

  @Nullable
  private static <T> T next(Iterator<T> it) {
    return it.hasNext() ? it.next() : null;
  }
}
