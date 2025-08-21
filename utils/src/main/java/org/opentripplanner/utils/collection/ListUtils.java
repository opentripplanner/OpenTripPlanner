package org.opentripplanner.utils.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

public class ListUtils {

  /**
   * Return the first element in the list. {@code null} is returned if the list is
   * null or empty.
   */
  public static <T> T first(List<T> list) {
    return list == null || list.isEmpty() ? null : list.getFirst();
  }

  /**
   * Return the last element in the list. {@code null} is returned if the list is
   * null or empty.
   */
  public static <T> T last(List<T> list) {
    return list == null || list.isEmpty() ? null : list.getLast();
  }

  /**
   * Combine a number of collections into a single list.
   */
  @SafeVarargs
  public static <T> List<T> combine(Collection<T>... lists) {
    return Arrays.stream(lists).flatMap(Collection::stream).toList();
  }

  /**
   * Take a collection and a {@code keyExtractor} to remove duplicates where the key to be compared
   * is not the entity itself but a field of it.
   * <p>
   * Note: Duplicate check is based on equality not identity.
   */
  public static <T> List<T> distinctByKey(
    Collection<T> original,
    Function<? super T, ?> keyExtractor
  ) {
    Set<Object> seen = new HashSet<>();
    var ret = new ArrayList<T>();

    original.forEach(elem -> {
      var key = keyExtractor.apply(elem);
      if (!seen.contains(key)) {
        seen.add(key);
        ret.add(elem);
      }
    });

    return ret;
  }

  /**
   * Take a single nullable variable and return an empty list if it is null. Otherwise
   * return a list with one element.
   */
  public static <T> List<T> ofNullable(@Nullable T input) {
    if (input == null) {
      return List.of();
    } else {
      return List.of(input);
    }
  }

  /**
   * This method converts the given collection to an instance of a List. If the input is
   * {@code null} an empty collection is returned. If not the {@link List#copyOf(Collection)} is
   * called.
   */
  public static <T> List<T> nullSafeImmutableList(@Nullable Collection<T> c) {
    return (c == null) ? List.of() : List.copyOf(c);
  }

  /**
   * Check if a list has at least the given {@code minLimit} number of elements(inclusive).
   * @throws IllegalStateException if the list has fewer elements.
   * @throws NumberFormatException if the list is {@code null}
   */
  public static <T> List<T> requireAtLeastNElements(List<T> list, int minLimit) {
    if (list.size() < minLimit) {
      throw new IllegalArgumentException("The list must have at least " + minLimit + " elements.");
    }
    return list;
  }

  /**
   * Take a list of items and split it into a list of "overlapping" pairs. For example
   * [A,B,C,D] becomes [[A,B],[B,C],[C,D]].
   */
  public static <T> List<Pair<T>> partitionIntoOverlappingPairs(List<T> input) {
    if (input.size() < 2) {
      return List.of();
    }
    if (input.size() == 2) {
      return List.of(new Pair<>(input.getFirst(), input.getLast()));
    }
    var output = new ArrayList<Pair<T>>(input.size() - 1);
    for (int i = 0; i < input.size() - 1; i++) {
      T first = input.get(i);
      T second = input.get(i + 1);
      output.add(new org.opentripplanner.utils.collection.Pair<>(first, second));
    }
    return output;
  }

  /**
   * Takes a list of at least 2 items and partitions them into "splits".
   * For example, [A,B,C,D] becomes
   *   [
   *     [A,[B,C,D]],
   *     [B,[C,D],
   *     [C,[D]]
   *   ]
   */
  public static <T> List<Split<T>> partitionIntoSplits(List<T> input) {
    requireAtLeastNElements(input, 2);
    var ret = new ArrayList<Split<T>>();
    for (int i = 0; i < input.size() - 1; i++) {
      var sublist = input.subList(i, input.size());
      ret.add(partitionIntoSplit(sublist));
    }
    return ret;
  }

  private static <T> Split<T> partitionIntoSplit(List<T> list) {
    requireAtLeastNElements(list, 2);
    return new Split<>(list.getFirst(), list.subList(1, list.size()));
  }
}
