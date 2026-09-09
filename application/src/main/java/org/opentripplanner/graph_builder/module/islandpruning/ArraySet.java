package org.opentripplanner.graph_builder.module.islandpruning;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A small, array-backed, insertion-order collection of distinct elements. Unlike
 * {@link java.util.HashSet}, membership checks are a linear scan rather than a hash lookup, and
 * elements are stored directly in a growable {@code Object[]} rather than delegating to
 * {@link java.util.ArrayList}.
 * <p>
 * This is a deliberate trade-off for very small collections: for the per-vertex street graph
 * neighbor lists held by {@link ArrayMultimap} in {@link IslandPruningModule} (a handful of
 * elements each), the hashing overhead of a hash-based {@code Set} costs more than a linear scan
 * saves. Benchmarked against a full-country OSM extract, a {@code HashSet}-based dedupe of those
 * neighbor lists was ~14% slower overall than this approach.
 * <p>
 * Not a general-purpose collection: this does not implement {@link java.util.Set}, has no
 * removal, and degrades to O(n) per {@link #add} as element count grows.
 */
class ArraySet<T> implements Iterable<T> {

  private static final int INITIAL_CAPACITY = 4;

  private Object[] elements = new Object[INITIAL_CAPACITY];
  private int size = 0;

  /**
   * Adds the element if not already present.
   *
   * @return true if the element was added, false if it was already present
   */
  boolean add(T element) {
    for (int i = 0; i < size; i++) {
      if (elements[i].equals(element)) {
        return false;
      }
    }
    if (size == elements.length) {
      Object[] grown = new Object[elements.length * 2];
      System.arraycopy(elements, 0, grown, 0, size);
      elements = grown;
    }
    elements[size++] = element;
    return true;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T next() {
        if (index >= size) {
          throw new NoSuchElementException();
        }
        return (T) elements[index++];
      }
    };
  }
}
