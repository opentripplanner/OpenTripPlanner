package org.opentripplanner.routing.graph.kryosupport.hppc;

import com.carrotsearch.hppc.IntHashSet;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.Arrays;

/**
 * Direct Kryo serializer for HPPC's {@link IntHashSet}. It uses only the public HPPC API, so it
 * works without reflective access to the HPPC module. The values are written in sorted order to
 * make the serialized form independent of the set's internal iteration order.
 */
class IntHashSetSerializer extends Serializer<IntHashSet> {

  @Override
  public void write(Kryo kryo, Output output, IntHashSet set) {
    int[] values = set.toArray();
    Arrays.sort(values);
    output.writeVarInt(values.length, true);

    for (int value : values) {
      output.writeVarInt(value, false);
    }
  }

  @Override
  public IntHashSet read(Kryo kryo, Input input, Class<? extends IntHashSet> type) {
    int size = input.readVarInt(true);
    var set = new IntHashSet(size);

    for (int i = 0; i < size; i++) {
      set.add(input.readVarInt(false));
    }

    return set;
  }
}
