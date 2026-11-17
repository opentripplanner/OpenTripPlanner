package org.opentripplanner.routing.graph.kryosupport.hppc;

import com.carrotsearch.hppc.Accountable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Rejects all HPPC containers that have no explicit serializer in {@link HppcKryoSupport}.
 * <p>
 * Without this, Kryo falls back to reflective field access on HPPC's internals. That does not work
 * when HPPC is a named Java module, since it does not open its packages for reflection.
 */
class UnsupportedHppcSerializer extends Serializer<Accountable> {

  @Override
  public void write(Kryo kryo, Output output, Accountable object) {
    throw unsupported(object.getClass());
  }

  @Override
  public Accountable read(Kryo kryo, Input input, Class<? extends Accountable> type) {
    throw unsupported(type);
  }

  private static UnsupportedOperationException unsupported(Class<?> type) {
    return new UnsupportedOperationException(
      "No Kryo serializer for HPPC type %s. Add one in %s.".formatted(
        type.getName(),
        HppcKryoSupport.class.getSimpleName()
      )
    );
  }
}
