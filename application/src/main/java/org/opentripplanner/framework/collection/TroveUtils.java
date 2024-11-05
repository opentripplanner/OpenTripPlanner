package org.opentripplanner.framework.collection;

import gnu.trove.map.TLongObjectMap;
import java.util.HashSet;
import java.util.Set;

public class TroveUtils {

  public static <U> void addToMapSet(TLongObjectMap<Set<U>> mapSet, long key, U value) {
    Set<U> set = mapSet.get(key);
    if (set == null) {
      set = new HashSet<>();
      mapSet.put(key, set);
    }
    set.add(value);
  }
}
