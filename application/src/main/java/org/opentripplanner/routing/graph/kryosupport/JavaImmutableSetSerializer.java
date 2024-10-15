package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Serializable;
import java.util.Set;

@SuppressWarnings("rawtypes")
class JavaImmutableSetSerializer extends Serializer<Set> {

  @Override
  public void write(Kryo kryo, Output output, Set set) {
    kryo.writeObject(output, new ImmSerList(set.toArray()));
  }

  @Override
  public Set read(Kryo kryo, Input input, Class<? extends Set> type) {
    return kryo.readObject(input, ImmSerList.class).toSet();
  }

  private static class ImmSerList implements Serializable {

    private final Object[] array;

    private ImmSerList(Object[] array) {
      this.array = array;
    }

    private Set toSet() {
      return Set.of(array);
    }
  }
}
