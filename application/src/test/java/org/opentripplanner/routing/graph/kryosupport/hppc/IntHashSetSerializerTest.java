package org.opentripplanner.routing.graph.kryosupport.hppc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.write;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import com.carrotsearch.hppc.IntHashSet;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class IntHashSetSerializerTest {

  @Test
  void roundTrip() {
    var set = IntHashSet.from(0, 1, -42, 100_000, Integer.MIN_VALUE, Integer.MAX_VALUE);

    IntHashSet read = writeAndRead(set);

    assertEquals(set, read);
    assertTrue(read.contains(0));
    assertTrue(read.contains(Integer.MIN_VALUE));
  }

  @Test
  void emptySet() {
    IntHashSet read = writeAndRead(new IntHashSet());
    assertTrue(read.isEmpty());
  }

  @Test
  void setCanBeModifiedAfterRead() {
    IntHashSet read = writeAndRead(IntHashSet.from(1, 2, 3));
    IntStream.range(10, 1000).forEach(read::add);
    assertEquals(993, read.size());
  }

  @Test
  void setInsideImmutableMap() {
    var map = Map.of("a", IntHashSet.from(1, 2), "b", IntHashSet.from(3));

    Map<String, IntHashSet> read = writeAndRead(map);

    assertEquals(map, read);
  }

  @Test
  void serializedFormIsIndependentOfIterationOrder() {
    // HPPC containers iterate in a per-instance order, so each instance has a different order.
    var expected = write(IntHashSet.from(5, 3, 9, 1, 7, 100, 200, 300));
    for (int i = 0; i < 20; i++) {
      var set = new IntHashSet();
      for (int v : new int[] { 300, 200, 100, 7, 1, 9, 3, 5 }) {
        set.add(v);
      }
      assertArrayEquals(expected, write(set));
    }
  }
}
