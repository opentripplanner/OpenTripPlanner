package org.opentripplanner.raptor.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable list of groups, indexed by a small, dense, non-negative integer, e.g. the Raptor
 * round (number of rides). It is a lightweight alternative to a {@code Map<Integer, List<T>>} when
 * the keys are known to be close to {@code 0..n}.
 * <p>
 * Every index in the range {@code [0, size())} has a group; indexes without elements have an empty
 * group. There are never any {@code null} groups. The size is the highest index with an element
 * plus one, so trailing empty groups are never stored.
 */
public final class IntIndexList<T> {

  private static final IntIndexList<?> EMPTY = new IntIndexList<>(List.of());

  private final List<List<T>> groups;

  private IntIndexList(List<List<T>> groups) {
    this.groups = groups;
  }

  @SuppressWarnings("unchecked")
  public static <T> IntIndexList<T> empty() {
    return (IntIndexList<T>) EMPTY;
  }

  /**
   * Group the input using the {@code toIndex} function. The function must return a non-negative
   * integer, which is the index of the group the value is added to. The order of the input is kept
   * within each group.
   */
  public static <T> IntIndexList<T> of(Collection<T> input, ToIntFunction<T> toIndex) {
    return of(input, _ -> true, toIndex);
  }

  /**
   * Same as {@link #of(Collection, ToIntFunction)}, but only the elements matching the
   * {@code include} predicate are added.
   */
  public static <T> IntIndexList<T> of(
    Collection<T> input,
    Predicate<T> include,
    ToIntFunction<T> toIndex
  ) {
    var groups = new ArrayList<List<T>>();
    for (T it : input) {
      if (include.test(it)) {
        int index = toIndex.applyAsInt(it);
        while (groups.size() <= index) {
          groups.add(new ArrayList<>());
        }
        groups.get(index).add(it);
      }
    }
    return seal(groups);
  }

  /**
   * Return the group at the given index, or an empty list if the index is outside the range
   * {@code [0, maxIndex()]}. This method never returns {@code null}.
   */
  public List<T> get(int index) {
    return index >= 0 && index < groups.size() ? groups.get(index) : List.of();
  }

  /** The highest index with an element, or {@code -1} if there are no elements. */
  public int maxIndex() {
    return groups.size() - 1;
  }

  /**
   * A lazy, read-only view of all elements of all groups, in index order. Nothing is copied; use
   * this instead of {@link #stream()} when you only need to iterate.
   */
  public Iterable<T> elements() {
    return () -> new FlatIterator<>(groups.iterator());
  }

  /** Stream all elements of all groups, in index order. */
  public Stream<T> stream() {
    return groups.stream().flatMap(List::stream);
  }

  /** Return a new list with only the elements matching the predicate. The index is preserved. */
  public IntIndexList<T> filter(Predicate<T> include) {
    var result = new ArrayList<List<T>>(groups.size());
    for (var group : groups) {
      result.add(group.stream().filter(include).collect(Collectors.toCollection(ArrayList::new)));
    }
    return seal(result);
  }

  /**
   * Drop trailing empty groups, then make the groups and the list immutable. The list is created
   * once and read many times, so we use compact immutable lists. The given list is modified.
   */
  private static <T> IntIndexList<T> seal(ArrayList<List<T>> groups) {
    while (!groups.isEmpty() && groups.getLast().isEmpty()) {
      groups.removeLast();
    }
    if (groups.isEmpty()) {
      return empty();
    }
    groups.replaceAll(List::copyOf);
    return new IntIndexList<>(List.copyOf(groups));
  }

  private static final class FlatIterator<T> implements Iterator<T> {

    private final Iterator<List<T>> groupIterator;
    private Iterator<T> current = Collections.emptyIterator();

    private FlatIterator(Iterator<List<T>> groupIterator) {
      this.groupIterator = groupIterator;
    }

    @Override
    public boolean hasNext() {
      while (!current.hasNext() && groupIterator.hasNext()) {
        current = groupIterator.next().iterator();
      }
      return current.hasNext();
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return current.next();
    }
  }

  @Override
  public String toString() {
    return groups.toString();
  }
}
