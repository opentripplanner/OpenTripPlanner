package org.opentripplanner.routing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import javax.annotation.Nullable;


/**
 * This class provide a method for comparing to lists and create a diff
 * between them.
 * <p>
 */
public final class DiffTool {

  public static class Diff<T> extends ArrayList<Entry<T>> {
    /* All elements exist in both collections, hence no differences. */
    public boolean isEqual() { return stream().allMatch(Entry::isEqual); }
  }

  public static class Entry<T> {
    public final T left;
    public final T right;

    private Entry(T left, T right) {
      this.left = left;
      this.right = right;
    }

    /**
     * Return the left instance if it exist, if not return the right instance.
     * If both exist(left equals right) then left is returned.
     */
    public T element() { return rightOnly() ? right : left; }

    /* The element exist in the left collection, not in the right. */
    public boolean leftOnly() { return right == null; }

    /* The element exist in the right collection, not in the left. */
    public boolean rightOnly() { return left == null; }

    /* The element exist in both collections. Element left equals the right instance. */
    public boolean isEqual() { return left != null && right != null; }

    /**
     *  Return a status message based on the element existence:
     *  - exist in left and right collection
     *  - exist left only
     *  - or, exist right only
     */
    public String status(String equal, String left, String right) {
      return leftOnly() ? left : (rightOnly() ? right : equal);
    }
  }

  /** This utility class have a private constructor to prevent instantiation. */
  private DiffTool() { }

  /**
   * Compare to collections(left and right) and return a list of differences.
   */
  public static <T> Diff<T> diff(
      Collection<T> left,
      Collection<T> right,
      Comparator<T> comparator
  ) {
    Diff<T> result = new Diff<>();

    Iterator<T> iLeft = left.stream().sorted(comparator).iterator();
    Iterator<T> iRight = right.stream().sorted(comparator).iterator();
    T l = next(iLeft);
    T r = next(iRight);

    while (l != null && r != null) {
      int c = comparator.compare(l, r);
      if(c < 0) {
        result.add(new Entry<>(l, null));
        l = next(iLeft);
      }
      else if(c > 0) {
        result.add(new Entry<>(null, r));
        r = next(iRight);
      }
      // c == 0
      else {
        result.add(new Entry<>(l, r));
        l = next(iLeft);
        r = next(iRight);
      }
    }
    while (iLeft.hasNext()) { result.add(new Entry<>(iLeft.next(), null)); }
    while (iRight.hasNext()) { result.add(new Entry<>(null, iRight.next())); }

    result.sort((o1, o2) -> -comparator.compare(o1.element(), o2.element()));

    return result;
  }

  @Nullable
  private static <T> T next(Iterator<T> it) { return it.hasNext() ? it.next() : null; }
}
