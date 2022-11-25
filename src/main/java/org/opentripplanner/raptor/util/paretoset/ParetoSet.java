package org.opentripplanner.raptor.util.paretoset;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This {@link java.util.Collection} store all pareto-optimal elements. The {@link #add(Object)}
 * method returns {@code true} if and only if the element was added successfully. When an element is
 * added other elements which are no longer pareto-optimal are dropped.
 * <p/>
 * Like the {@link java.util.ArrayList} the elements are stored internally in an array for
 * performance reasons, and the order is guaranteed to be the same as the order the elements are
 * added. New elements are added at the end, while dominated elements are removed. Elements in
 * between are shifted towards the beginning of the list:
 * <p/>
 * {@code  [[1,7], [3,5], [5,3]] + [2,4] => [[1,7], [5,3], [2,4]]   -- less than dominates}
 * <p/>
 * No methods for removing elements like {@link #remove(Object)} are supported.
 *
 * @param <T> the element type
 */
public class ParetoSet<T> extends AbstractCollection<T> {

  private final ParetoComparator<T> comparator;
  private final ParetoSetEventListener<? super T> eventListener;

  @SuppressWarnings("unchecked")
  private T[] elements = (T[]) new Object[16];

  private int size = 0;

  private T goodElement = null;

  /**
   * Create a new ParetoSet with a comparator and a drop event listener.
   *
   * @param comparator    The comparator to use with this set
   * @param eventListener At most one listener can be registered to listen for drop events.
   */
  public ParetoSet(
    ParetoComparator<T> comparator,
    ParetoSetEventListener<? super T> eventListener
  ) {
    this.comparator = comparator;
    this.eventListener = eventListener;
  }

  /**
   * Create a new ParetoSet with a comparator.
   */
  public ParetoSet(ParetoComparator<T> comparator) {
    this(comparator, null);
  }

  public T get(int index) {
    return elements[index];
  }

  /**
   * Return an iterator over the contained collection.
   * <p>
   * This is NOT thread-safe and the behavior is undefined if the collection is modified during the
   * iteration.
   */
  @Override
  public final Iterator<T> iterator() {
    return tailIterator(0);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean add(T newValue) {
    if (size == 0) {
      acceptAndAppendValue(newValue);
      return true;
    }

    // Quick shortcut, one element probably dominate most of the new elements
    if (goodElement != null && leftVectorDominatesRightVector(goodElement, newValue)) {
      notifyElementRejected(newValue, goodElement);
      return false;
    }

    boolean mutualDominanceExist = false;
    boolean equivalentVectorExist = false;

    for (int i = 0; i < size; ++i) {
      T it = elements[i];

      boolean leftDominance = leftDominanceExist(newValue, it);
      boolean rightDominance = rightDominanceExist(newValue, it);

      if (leftDominance && rightDominance) {
        mutualDominanceExist = true;
      } else if (leftDominance) {
        removeDominatedElementsFromRestOfSetAndAddNewElement(newValue, i);
        return true;
      } else if (rightDominance) {
        goodElement = elements[i];
        notifyElementRejected(newValue, it);
        return false;
      } else {
        equivalentVectorExist = true;
      }
    }

    if (mutualDominanceExist && !equivalentVectorExist) {
      assertEnoughSpaceInSet();
      acceptAndAppendValue(newValue);
      return true;
    }

    // No dominance found, newValue is equivalent with all values in the set
    notifyElementRejected(newValue, elements[0]);
    return false;
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    size = 0;
    goodElement = null;
  }

  @Override
  public String toString() {
    return toString(Objects::toString);
  }

  /**
   * Test if an element qualify - the element is NOT added. Use the {@link #add(T)} method directly
   * if the purpose is to add the new element to the collection.
   * <p/>
   * Both methods are optimized for performance; hence the add method does not use this method.
   */
  public boolean qualify(T newValue) {
    if (size == 0) {
      return true;
    }

    // Quick shortcut, one element probably dominate most of the new elements
    if (goodElement != null && leftVectorDominatesRightVector(goodElement, newValue)) {
      notifyElementRejected(newValue, goodElement);
      return false;
    }

    boolean mutualDominanceExist = false;
    boolean equivalentVectorExist = false;

    for (int i = size - 1; i >= 0; --i) {
      boolean leftDominance = leftDominanceExist(newValue, elements[i]);
      boolean rightDominance = rightDominanceExist(newValue, elements[i]);

      if (leftDominance && rightDominance) {
        if (equivalentVectorExist) {
          return false;
        }
        mutualDominanceExist = true;
      } else if (leftDominance) {
        return true;
      } else if (rightDominance) {
        goodElement = elements[i];
        return false;
      } else {
        if (mutualDominanceExist) {
          return false;
        }
        equivalentVectorExist = true;
      }
    }
    return mutualDominanceExist;
  }

  /**
   * This is used for logging and tuning purposes - by looking at the statistics we can decide a
   * good value for the initial size.
   */
  public final int internalArrayLength() {
    return elements.length;
  }

  /**
   * A special toSting method which allows the caller to provide a to-string-mapper for the elements
   * in the set.
   */
  public String toString(Function<? super T, String> toStringMapper) {
    return (
      "{" +
      Arrays.stream(elements, 0, size).map(toStringMapper).collect(Collectors.joining(", ")) +
      "}"
    );
  }

  /**
   * Notify subclasses about reindexing. This method is empty, and only exist for subclasses to
   * override it.
   */
  protected void notifyElementMoved(int fromIndex, int toIndex) {
    // Noop
  }

  /**
   * Return an iterable instance. This is made to be as FAST AS POSSIBLE, sacrificing thread-safety
   * and modifiable protection.
   * <p>
   * The iterator created by this iterable is NOT thread-safe.
   * <p>
   * Do not modify the collection between the iterator is created, until the iterator complete.
   * <p>
   * It is safe to create as many iterators as you like. The iterator created will reflect the
   * current set of elements in this set at the time of creation.
   *
   * @param startIndexInclusive the first element to include in the iterator, unlike the elements in
   *                            this set the index is cached until an iterator is created.
   */
  final Iterable<T> tail(final int startIndexInclusive) {
    return () -> tailIterator(startIndexInclusive);
  }

  /**
   * This tail iterator is made to be FAST, it is NOT thread-safe and it the underlying collection
   * is changed the returned values of the iterator also changes. Do not update on this collection
   * while using this iterator.
   */
  private Iterator<T> tailIterator(final int startInclusive) {
    return new Iterator<>() {
      int i = startInclusive;

      @Override
      public boolean hasNext() {
        return i < size;
      }

      @Override
      public T next() {
        return elements[i++];
      }
    };
  }

  /**
   * Remove all elements dominated by the {@code newValue} starting from {@code index + 1}. The
   * element at {@code index} is dropped.
   */
  private void removeDominatedElementsFromRestOfSetAndAddNewElement(
    final T newValue,
    final int index
  ) {
    // Let 'i' be the current element index for removal
    int i = index;
    // Let 'j' be the next element to compare
    int j = index + 1;

    notifyElementDropped(elements[i], newValue);

    while (j < size) {
      notifyElementMoved(j, i);
      // Move next element(j) forward if it is not dominated by the new value
      if (!leftVectorDominatesRightVector(newValue, elements[j])) {
        elements[i] = elements[j];
        ++i;
      } else {
        notifyElementDropped(elements[j], newValue);
      }
      // Goto the next element
      ++j;
    }
    notifyElementMoved(j, i);
    notifyElementAccepted(newValue);
    elements[i] = newValue;
    size = i + 1;
  }

  private boolean leftVectorDominatesRightVector(T left, T right) {
    return leftDominanceExist(left, right) && !rightDominanceExist(left, right);
  }

  private void acceptAndAppendValue(T newValue) {
    notifyElementAccepted(newValue);
    elements[size++] = newValue;
  }

  private void assertEnoughSpaceInSet() {
    if (size == elements.length) {
      elements = Arrays.copyOf(elements, elements.length * 2);
    }
  }

  private boolean leftDominanceExist(T left, T right) {
    return comparator.leftDominanceExist(left, right);
  }

  private boolean rightDominanceExist(T left, T right) {
    return comparator.leftDominanceExist(right, left);
  }

  private void notifyElementAccepted(T newElement) {
    if (eventListener != null) {
      eventListener.notifyElementAccepted(newElement);
    }
  }

  private void notifyElementDropped(T element, T droppedByElement) {
    if (eventListener != null) {
      eventListener.notifyElementDropped(element, droppedByElement);
    }
  }

  private void notifyElementRejected(T element, T rejectByElement) {
    if (eventListener != null) {
      eventListener.notifyElementRejected(element, rejectByElement);
    }
  }
}
