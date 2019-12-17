package org.opentripplanner.routing.graph;

import com.conveyal.kryo.TIntArrayListSerializer;
import com.conveyal.kryo.TIntIntHashMapSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ExternalizableSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import gnu.trove.impl.hash.TPrimitiveHash;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.opentripplanner.kryo.HashBiMapSerializer;
import org.opentripplanner.datastore.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;

/**
 * This is the class that get serialized/deserialized into/from the file <em>graph.obj</em>.
 * <p>
 * The Graph object does not contain a collection of edges. The set of edges is generated on demand
 * from the vertices. However, when serializing, we intentionally do not serialize the vertices'
 * edge lists to prevent excessive recursion. So we need to save the edges along with the graph. We
 * used to make two serialization calls, one for the graph and one for the edges. But we need the
 * serializer to know that vertices referenced by the edges are the same vertices stored in the
 * graph itself. The easiest way to do this is to make only one serialization call, serializing a
 * single object that contains both the graph and the edge collection.
 */
public class SerializedGraphObject implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(SerializedGraphObject.class);

    public final Graph graph;

    private final Collection<Edge> edges;

    public SerializedGraphObject(Graph graph) {
        this.graph = graph;
        this.edges = graph.getEdges();
    }

    /**
     * After deserialization, the vertices will all have null outgoing and incoming edge lists
     * because those edge lists are marked transient, to prevent excessive recursion depth while
     * serializing. This method will reconstruct all those edge lists after deserialization.
     */
    public void reconstructEdgeLists() {
        for (Vertex v : graph.getVertices()) {
            v.initEdgeLists();
        }
        for (Edge e : edges) {
            Vertex fromVertex = e.getFromVertex();
            Vertex toVertex = e.getToVertex();
            fromVertex.addOutgoing(e);
            toVertex.addIncoming(e);
        }
    }

    public void save(File file) throws IOException {
        try {
            save(new FileOutputStream(file), file.getName());
        } catch (Exception e) {
            file.delete(); // remove half-written file
            throw e;
        }
    }

    public void save(DataSource target) {
        save(target.asOutputStream(), target.name());
    }

    public void save(OutputStream outputStream, String graphName) {
        Kryo kryo = SerializedGraphObject.makeKryo();
        LOG.debug("Consolidating edges...");
        Output output = new Output(outputStream);
        kryo.writeClassAndObject(output, this);
        output.close();
        LOG.info("Graph written: {}", graphName);
        // Summarize serialized classes and associated serializers to stdout:
        // ((InstanceCountingClassResolver) kryo.getClassResolver()).summarize();
    }

    /**
     * This method allows reproducibly creating Kryo (de)serializer instances with exactly the same configuration.
     * This allows us to use identically configured instances for serialization and deserialization.
     *
     * When configuring serializers, there's a difference between kryo.register() and kryo.addDefaultSerializer().
     * The latter will set the default for a whole tree of classes. The former matches only the specified class.
     * By default Kryo will serialize all the non-transient fields of an instance. If the class has its own overridden
     * Java serialization methods Kryo will not automatically use those, a JavaSerializer must be registered.
     */
    public static Kryo makeKryo() {
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
        kryo.register(HashMultimap.class, new JavaSerializer());
        kryo.register(ArrayListMultimap.class, new JavaSerializer());
        // Kryo's default instantiation and deserialization of BitSets leaves them empty.
        // The Kryo BitSet serializer in magro/kryo-serializers naively writes out a dense stream of booleans.
        // BitSet's built-in Java serializer saves the internal bitfields, which is efficient. We use that one.
        kryo.register(BitSet.class, new JavaSerializer());
        // BiMap has a constructor that uses its putAll method, which just puts each item in turn.
        // It should be possible to reconstruct this like a standard Map. However, the HashBiMap constructor calls an
        // init method that creates the two internal maps. So we have to subclass the generic Map serializer.
        kryo.register(HashBiMap.class, new HashBiMapSerializer());
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
