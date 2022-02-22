package org.opentripplanner.routing.graph.kryosupport;

import com.conveyal.kryo.TIntArrayListSerializer;
import com.conveyal.kryo.TIntIntHashMapSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.ExternalizableSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import gnu.trove.impl.hash.TPrimitiveHash;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.opentripplanner.kryo.BuildConfigSerializer;
import org.opentripplanner.kryo.HashBiMapSerializer;
import org.opentripplanner.kryo.RouterConfigSerializer;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;

public final class KryoBuilder {

    /**
     * This method allows reproducibly creating Kryo (de)serializer instances with exactly the same
     * configuration. This allows us to use identically configured instances for serialization and
     * deserialization.
     *
     * When configuring serializers, there's a difference between kryo.register() and
     * kryo.addDefaultSerializer(). The latter will set the default for a whole tree of classes.
     * The former matches only the specified class. By default Kryo will serialize all the
     * non-transient fields of an instance. If the class has its own overridden Java serialization
     * methods Kryo will not automatically use those, a JavaSerializer must be registered.
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
        // Not supported in the current com.conveyal:kryo-tools:1.3.0.
        // This provide support for List.of, Set.of, Map.of and Collectors.toUnmodifiable(Set|List|Map)
        kryo.register(List.of().getClass(), new JavaImmutableListSerializer());
        kryo.register(List.of(1).getClass(), new JavaImmutableListSerializer());
        kryo.register(Set.of().getClass(), new JavaImmutableSetSerializer());
        kryo.register(Set.of(1).getClass(), new JavaImmutableSetSerializer());
        kryo.register(Map.of().getClass(), new JavaSerializer());
        kryo.register(Map.of(1, 1).getClass(), new JavaSerializer());

        // Kryo's default instantiation and deserialization of BitSets leaves them empty.
        // The Kryo BitSet serializer in magro/kryo-serializers naively writes out a dense stream of booleans.
        // BitSet's built-in Java serializer saves the internal bitfields, which is efficient. We use that one.
        kryo.register(BitSet.class, new JavaSerializer());

        // BiMap has a constructor that uses its putAll method, which just puts each item in turn.
        // It should be possible to reconstruct this like a standard Map. However, the HashBiMap constructor calls an
        // init method that creates the two internal maps. So we have to subclass the generic Map serializer.
        kryo.register(HashBiMap.class, new HashBiMapSerializer());
        kryo.register(HashMultimap.class, new HashMultimapSerializer());
        kryo.register(ArrayListMultimap.class, new ArrayListMultimapSerializer());

        // Add serializers for "immutable" config classes
        kryo.register(RouterConfig.class, new RouterConfigSerializer());
        kryo.register(BuildConfig.class, new BuildConfigSerializer());

        // OBA uses unmodifiable collections, but those classes have package-private visibility. Workaround.
        // FIXME we're importing all the contributed kryo-serializers just for this one serializer
        try {
            Class<?> unmodifiableCollection = Class.forName("java.util.Collections$UnmodifiableCollection");
            kryo.addDefaultSerializer(unmodifiableCollection , UnmodifiableCollectionsSerializer.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // Instantiation strategy: how should Kryo make new instances of objects when they are deserialized?
        // The default strategy requires every class you serialize, even in your dependencies, to have a zero-arg
        // constructor (which can be private). The setInstantiatorStrategy method completely replaces that default
        // strategy. The nesting below specifies the Java approach as a fallback strategy to the default strategy.
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy()));
        return kryo;
    }
}
