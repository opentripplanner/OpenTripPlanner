package org.opentripplanner.utils.collection;

import java.util.Collection;
import java.util.Map;
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

  /**
   * A null-safe version of isEmpty() for a collection.
   * <p>
   * The main strategy for handling collections in OTP is to avoid nullable collection fields and
   * use empty collections instead. So, before using this method check if the variable/field is
   * indeed `@Nullable`.
   * <p>
   * If the collection is {@code null} then {@code true} is returned.
   * <p>
   * If the collection is empty then {@code true} is returned.
   * <p>
   * Otherwise {@code false} is returned.
   */
  public static boolean isEmpty(@Nullable Collection<?> c) {
    return c == null || c.isEmpty();
  }

  /**
   * Returns true if the collection is non-null and has at least one element. If it is null,
   * it returns false.
   */
  public static boolean hasValue(@Nullable Collection<?> c) {
    return !isEmpty(c);
  }

  /**
   * A null-safe version of isEmpty() for a collection.
   * <p>
   * If the collection is {@code null} then {@code true} is returned.
   * <p>
   * If the collection is empty then {@code true} is returned.
   * <p>
   * Otherwise {@code false} is returned.
   */
  public static boolean isEmpty(@Nullable Map<?, ?> m) {
    return m == null || m.isEmpty();
  }

  /**
   * Look up the given key in a Map, return null if the key is null.
   * This prevents a NullPointerException if the underlying implementation of the map does not
   * accept querying with null keys (e.g. ImmutableMap).
   *
   **/
  @Nullable
  public static <K, V> V getByNullableKey(K key, Map<K, ? extends V> map) {
    if (key == null) {
      return null;
    }
    return map.get(key);
  }

  /**
   * Throws an IllegalArgumentException if the given collection is empty but doesn't if it is null.
   */
  public static void requireNullOrNonEmpty(Collection<?> coll, String name) {
    if (coll != null && coll.isEmpty()) {
      throw new IllegalArgumentException("'%s' must not be empty.".formatted(name));
    }
  }
}
