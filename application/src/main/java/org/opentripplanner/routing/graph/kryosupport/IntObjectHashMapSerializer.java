package org.opentripplanner.routing.graph.kryosupport;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Direct Kryo serializer for HPPC's {@link IntObjectHashMap}, replacing the default
 * reflection-based field serialization.
 */
class IntObjectHashMapSerializer extends Serializer<IntObjectHashMap<?>> {

  @Override
  public void write(Kryo kryo, Output output, IntObjectHashMap<?> map) {
    output.writeVarInt(map.size(), true);
    for (IntObjectCursor<?> c : map) {
      output.writeVarInt(c.key, true);
      kryo.writeClassAndObject(output, c.value);
    }
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public IntObjectHashMap<?> read(
    Kryo kryo,
    Input input,
    Class<? extends IntObjectHashMap<?>> type
  ) {
    int size = input.readVarInt(true);
    IntObjectHashMap map = new IntObjectHashMap(size);
    // Register the map before reading the values, so values referencing back to the map resolve.
    kryo.reference(map);
    for (int i = 0; i < size; i++) {
      int key = input.readVarInt(true);
      map.put(key, kryo.readClassAndObject(input));
    }
    return map;
  }
}
