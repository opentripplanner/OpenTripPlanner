package org.opentripplanner.routing.graph.kryosupport.hppc;

import com.carrotsearch.hppc.Accountable;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.esotericsoftware.kryo.Kryo;

/**
 * Kryo support for the HPPC containers stored in the serialized graph.
 * <p>
 * Each container type needs an explicit serializer using only the public HPPC API. Any other HPPC
 * container is rejected with an exception, see {@link UnsupportedHppcSerializer}. To store a new
 * HPPC type in the graph, add a serializer for it here.
 */
public final class HppcKryoSupport {

  private HppcKryoSupport() {}

  public static void register(Kryo kryo) {
    kryo.register(IntHashSet.class, new IntHashSetSerializer());
    kryo.register(IntObjectHashMap.class, new IntObjectHashMapSerializer());
    // All HPPC containers implement Accountable. Registered classes above take precedence.
    kryo.addDefaultSerializer(Accountable.class, new UnsupportedHppcSerializer());
  }
}
