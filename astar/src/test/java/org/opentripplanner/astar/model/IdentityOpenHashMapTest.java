package org.opentripplanner.astar.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdentityOpenHashMapTest {

  @Test
  void emptyMap() {
    var map = new IdentityOpenHashMap<String, String>(4);
    assertEquals(0, map.size());
    assertNull(map.get("key"));
  }

  @Test
  void putAndGet() {
    var map = new IdentityOpenHashMap<String, Integer>(4);
    String key = "hello";
    assertNull(map.put(key, 42));
    assertEquals(42, map.get(key));
    assertEquals(1, map.size());
  }

  @Test
  void putReplace() {
    var map = new IdentityOpenHashMap<String, Integer>(4);
    String key = "key";
    assertNull(map.put(key, 1));
    assertEquals(1, map.put(key, 2));
    assertEquals(2, map.get(key));
    assertEquals(1, map.size());
  }

  @Test
  void identitySemantics() {
    var map = new IdentityOpenHashMap<String, Integer>(4);
    // new String() forces a different identity
    String key1 = new String("same");
    String key2 = new String("same");
    map.put(key1, 1);
    map.put(key2, 2);
    assertEquals(1, map.get(key1));
    assertEquals(2, map.get(key2));
    assertEquals(2, map.size());
  }

  @Test
  void resize() {
    // Small initial capacity to force resize
    var map = new IdentityOpenHashMap<Object, Integer>(2);
    Object[] keys = new Object[100];
    for (int i = 0; i < keys.length; i++) {
      keys[i] = new Object();
      map.put(keys[i], i);
    }
    assertEquals(100, map.size());
    for (int i = 0; i < keys.length; i++) {
      assertEquals(i, map.get(keys[i]));
    }
  }

  @Test
  void largeMap() {
    int count = 100_000;
    var map = new IdentityOpenHashMap<Object, Integer>(count);
    Object[] keys = new Object[count];
    for (int i = 0; i < count; i++) {
      keys[i] = new Object();
      map.put(keys[i], i);
    }
    assertEquals(count, map.size());
    for (int i = 0; i < count; i++) {
      assertEquals(i, map.get(keys[i]));
    }
  }

  @Test
  void forEachKey() {
    var map = new IdentityOpenHashMap<String, Integer>(8);
    String a = "a",
      b = "b",
      c = "c";
    map.put(a, 1);
    map.put(b, 2);
    map.put(c, 3);

    Set<String> visited = new HashSet<>();
    map.forEachKey(visited::add);
    assertEquals(Set.of(a, b, c), visited);
  }

  @Test
  void forEachValue() {
    var map = new IdentityOpenHashMap<String, Integer>(8);
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);

    Set<Integer> visited = new HashSet<>();
    map.forEachValue(visited::add);
    assertEquals(Set.of(1, 2, 3), visited);
  }

  @Test
  void multipleSegments() {
    // expectedSize=25_000 forces 4 segments (totalSlots=65536, 16384 slots each)
    int count = 25_000;
    var map = new IdentityOpenHashMap<Object, Integer>(count);
    Object[] keys = new Object[count];
    for (int i = 0; i < count; i++) {
      keys[i] = new Object();
      map.put(keys[i], i);
    }
    assertEquals(count, map.size());
    for (int i = 0; i < count; i++) {
      assertEquals(i, map.get(keys[i]));
    }
  }

  @Test
  void sizeTracking() {
    var map = new IdentityOpenHashMap<String, Integer>(8);
    assertEquals(0, map.size());
    String key = "key";
    map.put(key, 1);
    assertEquals(1, map.size());
    // Replace does not increment size
    map.put(key, 2);
    assertEquals(1, map.size());
    map.put("other", 3);
    assertEquals(2, map.size());
  }
}
