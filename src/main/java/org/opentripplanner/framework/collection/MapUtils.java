package org.opentripplanner.framework.collection;

import gnu.trove.map.TLongObjectMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapUtils {

  public static <U> void addToMapSet(TLongObjectMap<Set<U>> mapSet, long key, U value) {
    Set<U> set = mapSet.get(key);
    if (set == null) {
      set = new HashSet<>();
      mapSet.put(key, set);
    }
    set.add(value);
  }

  /**
   * Map a collection of objects of type <em>S</em> to a list of type <em>T</em> using the provided
   * mapping function.
   * <p>
   * Nullsafe: if <em>entities</em> is <code>null</code>, then <code>null</code> is returned.
   */
  public static <S, T> List<T> mapToList(Collection<S> entities, Function<S, T> mapper) {
    return entities == null ? null : entities.stream().map(mapper).collect(Collectors.toList());
  }
}
