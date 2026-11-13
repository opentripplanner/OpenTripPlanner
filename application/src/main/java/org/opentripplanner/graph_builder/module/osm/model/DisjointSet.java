package org.opentripplanner.graph_builder.module.osm.model;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.procedures.ObjectIntProcedure;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

  /**
   * Return the disjoint sets in a stable order, so that the result is the same on every call and
   * the graph build is deterministic. The sets are ordered by the index of their representative,
   * which is assigned when the element is first added.
   * <p>
   * This is called once per graph build and is not performance critical; the cost is O(n log g) for
   * n elements in g sets.
   */
  public List<Set<T>> sets() {
    Map<Integer, Set<T>> out = new TreeMap<>();
    setMapping.forEach(
      (ObjectIntProcedure<T>) (k, v) -> out.computeIfAbsent(compact(v), i -> new HashSet<>()).add(k)
    );
    return new ArrayList<>(out.values());
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
