package org.opentripplanner.routing.graph.kryosupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.collection.trie.ImmutableTrieMap;

class ImmutableTrieMapSerializerTest {

  @Test
  void roundTrip() {
    var expected = new HashMap<String, Integer>();
    for (int i = 0; i < 1_000; ++i) {
      expected.put("K" + i, i);
    }

    var read = writeAndRead(ImmutableTrieMap.of(expected));

    assertEquals(ImmutableTrieMap.class, read.getClass());
    assertEquals(expected, read);
    assertEquals(expected.size(), read.size());
  }

  @Test
  void emptyMapReadsBackAsTheEmptyMap() {
    assertSame(ImmutableTrieMap.empty(), writeAndRead(ImmutableTrieMap.empty()));
  }

  @Test
  void sharedValuesKeepIdentity() {
    var shared = List.of("shared");
    var map = ImmutableTrieMap.<Integer, List<String>>empty().with(1, shared).with(2, shared);

    var read = writeAndRead(map);

    assertSame(read.get(1), read.get(2));
  }

  @Test
  void sharedMapKeepsIdentity() {
    var map = ImmutableTrieMap.<String, Integer>empty().with("A", 1);

    var read = writeAndRead(List.of(map, map));

    assertSame(read.get(0), read.get(1));
  }

  @SuppressWarnings("unchecked")
  private static <T> T writeAndRead(T object) {
    var kryo = KryoBuilder.create();
    var bytes = new ByteArrayOutputStream();
    try (var out = new Output(bytes)) {
      kryo.writeClassAndObject(out, object);
    }
    return (T) kryo.readClassAndObject(new Input(bytes.toByteArray()));
  }
}
