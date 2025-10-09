package org.opentripplanner.routing.graph.kryosupport;

import com.conveyal.kryo.TIntArrayListSerializer;
import com.conveyal.kryo.TIntIntHashMapSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.ExternalizableSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import gnu.trove.impl.hash.TPrimitiveHash;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.opentripplanner.kryo.BuildConfigSerializer;
import org.opentripplanner.kryo.RouterConfigSerializer;
import org.opentripplanner.kryo.UnmodifiableCollectionsSerializer;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;

public final class KryoBuilder {

  /**
   * This method allows reproducibly creating Kryo (de)serializer instances with exactly the same
   * configuration. This allows us to use identically configured instances for serialization and
   * deserialization.
   * <p>
   * When configuring serializers, there's a difference between kryo.register() and
   * kryo.addDefaultSerializer(). The latter will set the default for a whole tree of classes. The
   * former matches only the specified class. By default Kryo will serialize all the non-transient
   * fields of an instance. If the class has its own overridden Java serialization methods Kryo will
   * not automatically use those, a JavaSerializer must be registered.
   */
  public static Kryo create() {
    // For generating a histogram of serialized classes with associated serializers:
    // Kryo kryo = new Kryo(new InstanceCountingClassResolver(), new MapReferenceResolver(), new DefaultStreamFactory());
    Kryo kryo = new Kryo();
    // Allow serialization of unrecognized classes, for which we haven't manually set up a serializer.
    // We might actually want to manually register a serializer for every class, to be safe.
    kryo.setRegistrationRequired(false);
    kryo.setReferences(true);
    kryo.addDefaultSerializer(TPrimitiveHash.class, ExternalizableSerializer.class);
    kryo.register(TIntArrayList.class, new TIntArrayListSerializer());
    kryo.register(TIntIntHashMap.class, new TIntIntHashMapSerializer());

    // Add support for the package local java.util.ImmutableCollections.
    // Not supported properly in the current com.conveyal:kryo-tools:1.4.0.
    // This provide support for List.of, Set.of, Map.of and Collectors.toUnmodifiable(Set|List|Map)
    kryo.register(List.of().getClass(), new JavaImmutableListSerializer());
    kryo.register(List.of(1).getClass(), new JavaImmutableListSerializer());
    kryo.register(Set.of().getClass(), new JavaImmutableSetSerializer());
    kryo.register(Set.of(1).getClass(), new JavaImmutableSetSerializer());
    kryo.register(Map.of().getClass(), new JavaImmutableMapSerializer());
    kryo.register(Map.of(1, 1).getClass(), new JavaImmutableMapSerializer());
    kryo.register(EnumMap.class, new EnumMapSerializer());

    kryo.register(HashMultimap.class, new HashMultimapSerializer());
    kryo.register(ArrayListMultimap.class, new ArrayListMultimapSerializer());

    // Add serializers for "immutable" config classes
    kryo.register(RouterConfig.class, new RouterConfigSerializer());
    kryo.register(BuildConfig.class, new BuildConfigSerializer());
    kryo.register(AtomicInteger.class, new AtomicIntegerSerializer());

    UnmodifiableCollectionsSerializer.registerSerializers(kryo);
    // Instantiation strategy: how should Kryo make new instances of objects when they are deserialized?
    // The default strategy requires every class you serialize, even in your dependencies, to have a zero-arg
    // constructor (which can be private). The setInstantiatorStrategy method completely replaces that default
    // strategy. The nesting below specifies the Java approach as a fallback strategy to the default strategy.
    kryo.setInstantiatorStrategy(
      new DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy())
    );
    return kryo;
  }
}
