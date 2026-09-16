package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Direct Kryo serializer for Trove's {@link TIntObjectHashMap}, replacing the {@code
 * Externalizable} fallback.
 */
class TIntObjectHashMapSerializer extends Serializer<TIntObjectHashMap<?>> {

  @Override
  public void write(Kryo kryo, Output output, TIntObjectHashMap<?> map) {
    output.writeVarInt(map.getNoEntryKey(), false);
    output.writeVarInt(map.size(), true);
    TIntObjectIterator<?> it = map.iterator();
    while (it.hasNext()) {
      it.advance();
      output.writeVarInt(it.key(), true);
      kryo.writeClassAndObject(output, it.value());
    }
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public TIntObjectHashMap<?> read(
    Kryo kryo,
    Input input,
    Class<? extends TIntObjectHashMap<?>> type
  ) {
    int noEntryKey = input.readVarInt(false);
    int size = input.readVarInt(true);
    TIntObjectHashMap map = new TIntObjectHashMap(size, Constants.DEFAULT_LOAD_FACTOR, noEntryKey);
    // Register the map before reading the values, so values referencing back to the map resolve.
    kryo.reference(map);
    for (int i = 0; i < size; i++) {
      int key = input.readVarInt(true);
      map.put(key, kryo.readClassAndObject(input));
    }
    return map;
  }
}
