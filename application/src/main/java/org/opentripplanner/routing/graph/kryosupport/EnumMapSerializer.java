package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * A custom serializer for {@link EnumMap} which fails to serialize properly with Kryo. A regular
 * {@link HashMap} is used, since there are build in functions in Java to convert between the two
 * classes.
 */
public class EnumMapSerializer extends Serializer<EnumMap<? extends Enum<?>, ?>> {

  @Override
  public void write(Kryo kryo, Output output, EnumMap<? extends Enum<?>, ?> obj) {
    kryo.writeObject(output, new HashMap<>(obj));
  }

  @Override
  public EnumMap read(Kryo kryo, Input input, Class<? extends EnumMap<?, ?>> type) {
    return new EnumMap(kryo.readObject(input, HashMap.class));
  }
}
