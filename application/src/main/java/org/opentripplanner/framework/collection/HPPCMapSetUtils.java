package org.opentripplanner.framework.collection;

import com.carrotsearch.hppc.LongObjectMap;
import java.util.HashSet;
import java.util.Set;

public class HPPCMapSetUtils {

  public static <U> void addToMapSet(LongObjectMap<Set<U>> mapSet, long key, U value) {
    Set<U> set = mapSet.get(key);
    if (set == null) {
      set = new HashSet<>();
      mapSet.put(key, set);
    }
    set.add(value);
  }
}
