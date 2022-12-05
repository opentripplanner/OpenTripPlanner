package org.opentripplanner.framework.collection;

import java.util.Comparator;

/**
 * This class take a list of comparators and turn them into one, iterating over the vector passes in
 * at construction time.
 * <p>
 * This class implement the composite design pattern.
 * <p>
 * THIS CLASS IS THREAD-SAFE
 */
public class CompositeComparator<T> implements Comparator<T> {

  private final Comparator<T>[] compareVector;

  @SafeVarargs
  public CompositeComparator(Comparator<T>... compareVector) {
    this.compareVector = compareVector;
  }

  @Override
  public int compare(T o1, T o2) {
    int v;
    for (Comparator<T> c : compareVector) {
      v = c.compare(o1, o2);
      if (v != 0) {
        return v;
      }
    }
    return 0;
  }
}
