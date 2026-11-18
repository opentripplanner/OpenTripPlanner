package org.opentripplanner.routing.graph.kryosupport.hppc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.write;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import com.carrotsearch.hppc.IntObjectHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntObjectHashMapSerializerTest {

  @Test
  void roundTrip() {
    var map = new IntObjectHashMap<String>();
    map.put(1, "one");
    map.put(-42, "minus forty-two");
    map.put(100_000, "hundred thousand");

    IntObjectHashMap<?> read = writeAndRead(map);

    assertEquals(3, read.size());
    assertEquals("one", read.get(1));
    assertEquals("minus forty-two", read.get(-42));
    assertEquals("hundred thousand", read.get(100_000));
  }

  @Test
  void emptyMap() {
    IntObjectHashMap<?> read = writeAndRead(new IntObjectHashMap<String>());
    assertTrue(read.isEmpty());
  }

  @Test
  void sharedValuesKeepIdentity() {
    var shared = List.of("shared");
    var map = new IntObjectHashMap<List<String>>();
    map.put(1, shared);
    map.put(2, shared);

    IntObjectHashMap<?> read = writeAndRead(map);

    assertSame(read.get(1), read.get(2));
  }

  @Test
  void serializedFormIsIndependentOfIterationOrder() {
    // HPPC containers iterate in a per-instance order, so each instance has a different order.
    var expected = write(map(5, 3, 9, 1, 7, 100, 200, 300));

    for (int i = 0; i < 20; i++) {
      assertArrayEquals(expected, write(map(300, 200, 100, 7, 1, 9, 3, 5)));
    }
  }

  private static IntObjectHashMap<String> map(int... keys) {
    var map = new IntObjectHashMap<String>();

    for (int key : keys) {
      map.put(key, "v" + key);
    }

    return map;
  }
}
