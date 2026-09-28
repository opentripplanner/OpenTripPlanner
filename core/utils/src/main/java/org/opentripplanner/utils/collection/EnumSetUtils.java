package org.opentripplanner.utils.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class EnumSetUtils {

  /**
   * This is private to avoid instantiation of static utility class.
   */
  private EnumSetUtils() {}

  /**
   * Create an {@link EnumSet} wrapped using {@link Collections#unmodifiableSet(Set)}. The
   * returned set has almost the same efficiency as EnumSet and is immutable, which EnumSet is not.
   */
  public static <E extends Enum> Set<E> unmodifiableEnumSet(Collection<E> values, Class<E> type) {
    var enumSet = values.isEmpty() ? EnumSet.noneOf(type) : EnumSet.copyOf(values);
    return Collections.unmodifiableSet(enumSet);
  }
}
