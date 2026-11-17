package org.opentripplanner.routing.graph.kryosupport.java;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * A custom serializer for {@link EnumMap}.
 * <p>
 * Kryo cannot instantiate an {@link EnumMap} by itself. It uses its {@code MapSerializer} for all
 * maps, which needs to create the map without calling a public constructor. {@link EnumMap} has no
 * no-arg constructor, and the fallback, Java serialization style instantiation, fails because the
 * JDK does not open {@code java.util} for reflective access to the constructor of the superclass
 * {@link java.util.AbstractMap}. The map must be created with a public constructor instead.
 * <p>
 * A non-empty map is written as a regular {@link HashMap}. The enum type is found from the keys
 * when it is read, using the {@link EnumMap#EnumMap(java.util.Map)} constructor.
 * <p>
 * An empty map has no keys to find the enum type from, and the type is not accessible through the
 * public API of {@link EnumMap}. An empty map is therefore written using Java serialization, which
 * preserves the enum type. An empty map has no values, so Java serialization is safe here.
 */
public class EnumMapSerializer extends Serializer<EnumMap<? extends Enum<?>, ?>> {

  private final JavaSerializer javaSerializer = new JavaSerializer();

  @Override
  public void write(Kryo kryo, Output output, EnumMap<? extends Enum<?>, ?> obj) {
    if (obj.isEmpty()) {
      output.writeBoolean(true);
      javaSerializer.write(kryo, output, obj);
    } else {
      output.writeBoolean(false);
      kryo.writeObject(output, new HashMap<>(obj));
    }
  }

  @Override
  public EnumMap read(Kryo kryo, Input input, Class<? extends EnumMap<?, ?>> type) {
    if (input.readBoolean()) {
      return (EnumMap) javaSerializer.read(kryo, input, (Class) type);
    }
    return new EnumMap(kryo.readObject(input, HashMap.class));
  }
}
