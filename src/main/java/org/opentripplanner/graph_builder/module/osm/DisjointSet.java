package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.collection.MapUtils;

/** Basic union-find data structure with path compression */
public class DisjointSet<T> {

  TIntList sets = new TIntArrayList();

  TObjectIntMap<T> setMapping = new TObjectIntHashMap<>();

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
    TLongObjectMap<Set<T>> out = new TLongObjectHashMap<>();
    setMapping.forEachEntry((k, v) -> {
      MapUtils.addToMapSet(out, compact(v), k);
      return true;
    });
    return new ArrayList<>(out.valueCollection());
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
