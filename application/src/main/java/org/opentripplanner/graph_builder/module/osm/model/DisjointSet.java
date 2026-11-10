package org.opentripplanner.graph_builder.module.osm.model;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.procedures.ObjectIntProcedure;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.collection.HPPCMapSetUtils;

/** Basic union-find data structure with path compression */
public class DisjointSet<T> {

  IntArrayList sets = new IntArrayList();

  ObjectIntMap<T> setMapping = new ObjectIntHashMap<>();

  public DisjointSet() {}

  public int union(T element1, T element2) {
    int p1 = find(element1);
    int p2 = find(element2);

    if (p1 == p2) {
      return p1;
    }

    int p1size = -sets.get(p1);
    int p2size = -sets.get(p2);

    int totalSize = p1size + p2size;

    if (p1size > p2size) {
      sets.set(p2, p1);
      sets.set(p1, -totalSize);
      return p2;
    } else {
      sets.set(p1, p2);
      sets.set(p2, -totalSize);
      return p1;
    }
  }

  public int find(T element) {
    if (setMapping.containsKey(element)) {
      int i = setMapping.get(element);
      return compact(i);
    } else {
      setMapping.put(element, sets.size());
      sets.add(-1);
      return sets.size() - 1;
    }
  }

  public boolean exists(T element) {
    return setMapping.containsKey(element);
  }

  public List<Set<T>> sets() {
    LongObjectMap<Set<T>> out = new LongObjectHashMap<>();
    setMapping.forEach(
      (ObjectIntProcedure<T>) (k, v) -> HPPCMapSetUtils.addToMapSet(out, compact(v), k)
    );
    List<Set<T>> result = new ArrayList<>();
    for (ObjectCursor<Set<T>> c : out.values()) {
      result.add(c.value);
    }
    return result;
  }

  public int size(int component) {
    return -sets.get(component);
  }

  private int compact(int i) {
    int key = sets.get(i);
    if (key < 0) {
      return i;
    }
    int j = compact(key);
    sets.set(i, j);
    return j;
  }
}
