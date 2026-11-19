package org.opentripplanner.routing.graph.kryosupport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.ByteArrayOutputStream;

/** Serialize with the same Kryo configuration as the graph serialization. */
public final class KryoTestHelper {

  private static final Kryo KRYO = KryoBuilder.create();

  private KryoTestHelper() {}

  public static byte[] write(Object object) {
    var bytes = new ByteArrayOutputStream();

    try (var out = new Output(bytes)) {
      KRYO.writeClassAndObject(out, object);
    }

    return bytes.toByteArray();
  }

  @SuppressWarnings("unchecked")
  public static <T> T writeAndRead(T object) {
    return (T) KRYO.readClassAndObject(new Input(write(object)));
  }
}
