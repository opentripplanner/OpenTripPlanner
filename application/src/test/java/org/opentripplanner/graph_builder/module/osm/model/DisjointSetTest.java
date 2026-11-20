package org.opentripplanner.graph_builder.module.osm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DisjointSetTest {

  @Test
  public void testSimple() {
    DisjointSet<String> set = new DisjointSet<>();
    set.union("cats", "dogs");
    assertEquals(set.size(set.find("cats")), 2);

    assertEquals(set.find("cats"), set.find("dogs"));
    assertTrue(set.find("cats") != set.find("sparrows"));
    assertEquals(set.size(set.find("sparrows")), 1);

    set.union("sparrows", "robins");
    assertEquals(set.size(set.find("robins")), 2);

    assertEquals(2, set.sets().size());

    assertTrue(set.find("dogs") != set.find("robins"));
    assertEquals(set.find("sparrows"), set.find("robins"));

    set.union("sparrows", "dogs");
    assertEquals(set.find("dogs"), set.find("robins"));
    assertEquals(set.size(set.find("cats")), 4);

    assertEquals(1, set.sets().size());
  }

  @Test
  public void testRandom() {
    DisjointSet<Integer> set = new DisjointSet<>();
    Random random = new Random(1);
    for (int i = 0; i < 150; ++i) {
      set.union(Math.abs(random.nextInt() % 700), Math.abs(random.nextInt() % 700));
    }

    HashMap<Integer, Integer> seen = new HashMap<>();
    int sizeSum = 0;
    for (int i = 0; i < 700; ++i) {
      int key = set.find(i);
      int size = set.size(key);

      Integer lastSize = seen.get(key);
      assertTrue(lastSize == null || size == lastSize);
      if (lastSize == null) {
        seen.put(key, size);
        sizeSum += size;
      }
      assertTrue(size >= 1 && size <= 150);
    }
    assertEquals(700, sizeSum);
  }

  @Test
  public void setsAreOrderedByFirstInsertedElement() {
    var expected = List.of(Set.of(0, 1), Set.of(2, 3), Set.of(4, 5), Set.of(6, 7), Set.of(8, 9));
    // Repeat with new instances: HPPC hash containers iterate in a per-instance order.
    for (int round = 0; round < 20; ++round) {
      DisjointSet<Integer> set = new DisjointSet<>();
      for (int i = 0; i < 10; i += 2) {
        set.union(i, i + 1);
      }
      assertEquals(expected, set.sets());
    }
  }
}
