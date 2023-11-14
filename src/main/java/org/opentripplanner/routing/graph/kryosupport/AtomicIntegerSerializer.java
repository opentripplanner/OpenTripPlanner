package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerSerializer extends Serializer<AtomicInteger> {
  @Override
  public void write(Kryo kryo, Output output, AtomicInteger obj) {
    kryo.writeObject(output, obj.get());
  }

  @Override
  public AtomicInteger read(Kryo kryo, Input input, Class<? extends AtomicInteger> type) {
    var value = kryo.readObject(input, Integer.class);
    return new AtomicInteger(value);
  }
}
