package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Serializable;
import java.util.List;

@SuppressWarnings("rawtypes")
class JavaImmutableListSerializer extends Serializer<List> {

  @Override
  public void write(Kryo kryo, Output output, List list) {
    kryo.writeObject(output, new ImmSerList(list.toArray()));
  }

  @Override
  public List read(Kryo kryo, Input input, Class<? extends List> type) {
    return kryo.readObject(input, ImmSerList.class).toList();
  }

  private static class ImmSerList implements Serializable {

    private final Object[] array;

    private ImmSerList(Object[] array) {
      this.array = array;
    }

    private List toList() {
      return List.of(array);
    }
  }
}
