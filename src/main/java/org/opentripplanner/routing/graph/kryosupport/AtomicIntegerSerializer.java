package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A custom serializer for an {@link AtomicInteger}.
 * <p>
 * Required for writing the stop index counter to the graph, so we can create new stops later.
 */
public class AtomicIntegerSerializer extends Serializer<AtomicInteger> {

  @Override
  public void write(Kryo kryo, Output output, AtomicInteger obj) {
    output.writeInt(obj.intValue());
  }

  @Override
  public AtomicInteger read(Kryo kryo, Input input, Class<? extends AtomicInteger> type) {
    var value = input.readInt();
    return new AtomicInteger(value);
  }
}
