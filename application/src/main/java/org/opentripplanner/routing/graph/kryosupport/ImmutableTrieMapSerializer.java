package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.HashMap;
import org.opentripplanner.utils.collection.trie.ImmutableTrieMap;

/**
 * Kryo serializer for {@link ImmutableTrieMap}. A map is written as its entries and rebuilt when it
 * is read, so a map that was empty reads back as {@link ImmutableTrieMap#empty()}.
 */
class ImmutableTrieMapSerializer extends Serializer<ImmutableTrieMap<?, ?>> {

  @Override
  public void write(Kryo kryo, Output output, ImmutableTrieMap<?, ?> map) {
    output.writeVarInt(map.size(), true);
    map.forEach((key, value) -> {
      kryo.writeClassAndObject(output, key);
      kryo.writeClassAndObject(output, value);
    });
  }

  @Override
  public ImmutableTrieMap<?, ?> read(
    Kryo kryo,
    Input input,
    Class<? extends ImmutableTrieMap<?, ?>> type
  ) {
    int size = input.readVarInt(true);
    var entries = HashMap.newHashMap(size);
    for (int i = 0; i < size; i++) {
      entries.put(kryo.readClassAndObject(input), kryo.readClassAndObject(input));
    }
    return ImmutableTrieMap.of(entries);
  }
}
