package org.opentripplanner.routing.graph.kryosupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class TIntObjectHashMapSerializerTest {

  @Test
  void roundTrip() {
    var map = new TIntObjectHashMap<String>();
    map.put(1, "one");
    map.put(-42, "minus forty-two");
    map.put(100_000, "hundred thousand");

    TIntObjectHashMap<?> read = writeAndRead(map);

    assertEquals(3, read.size());
    assertEquals("one", read.get(1));
    assertEquals("minus forty-two", read.get(-42));
    assertEquals("hundred thousand", read.get(100_000));
    assertEquals(map.getNoEntryKey(), read.getNoEntryKey());
  }

  @Test
  void emptyMap() {
    TIntObjectHashMap<?> read = writeAndRead(new TIntObjectHashMap<String>());
    assertTrue(read.isEmpty());
  }

  @Test
  void sharedValuesKeepIdentity() {
    var shared = List.of("shared");
    var map = new TIntObjectHashMap<List<String>>();
    map.put(1, shared);
    map.put(2, shared);

    TIntObjectHashMap<?> read = writeAndRead(map);

    assertSame(read.get(1), read.get(2));
  }

  private static TIntObjectHashMap<?> writeAndRead(TIntObjectHashMap<?> map) {
    var kryo = KryoBuilder.create();
    var bytes = new ByteArrayOutputStream();
    try (var out = new Output(bytes)) {
      kryo.writeClassAndObject(out, map);
    }
    return (TIntObjectHashMap<?>) kryo.readClassAndObject(new Input(bytes.toByteArray()));
  }
}
