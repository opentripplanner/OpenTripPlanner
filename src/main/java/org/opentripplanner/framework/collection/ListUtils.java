package org.opentripplanner.framework.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
}
