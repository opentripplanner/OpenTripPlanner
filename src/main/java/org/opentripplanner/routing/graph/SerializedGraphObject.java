package org.opentripplanner.routing.graph;

import com.conveyal.kryo.TIntArrayListSerializer;
import com.conveyal.kryo.TIntIntHashMapSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ExternalizableSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import gnu.trove.impl.hash.TPrimitiveHash;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.kryo.BuildConfigSerializer;
import org.opentripplanner.kryo.HashBiMapSerializer;
import org.opentripplanner.kryo.RouterConfigSerializer;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.util.OtpAppException;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    /** The config JSON used to build this graph. Allows checking whether the configuration has changed. */
    public final BuildConfig buildConfig;

    /** Embed a router configuration inside the graph, for starting up with a single file. */
    public final RouterConfig routerConfig;

    public SerializedGraphObject(Graph graph, BuildConfig buildConfig, RouterConfig routerConfig) {
        this.graph = graph;
        this.edges = graph.getEdges();
        this.buildConfig = buildConfig;
        this.routerConfig = routerConfig;
    }

    public static void verifyTheOutputGraphIsWritableIfDataSourceExist(DataSource graphOutput) {
        if (graphOutput != null) {
            // Abort building a graph if the file can not be saved
            if (graphOutput.exists()) {
                LOG.info("Graph already exists and will be overwritten at the end of the " + "build process. Graph: {}", graphOutput.path());
            }
            if (!graphOutput.isWritable()) {
                throw new RuntimeException(
                        "Cannot create or write to graph at: " + graphOutput.path());
            }
        }
    }

    public static SerializedGraphObject load(DataSource source) {
        return load(source.asInputStream(), source.path());
    }

    public static Graph load(File file) {
        try {
            SerializedGraphObject serObj = load(
                    new FileInputStream(file),
                    file.getAbsolutePath()
            );
            return serObj == null ? null : serObj.graph;
        } catch (FileNotFoundException e) {
            LOG.error("Graph file not found: " + file, e);
            throw new OtpAppException(e.getMessage());
        }
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

    /**
     * Save this object to the target it the target data source is not {@code null}.
     */
    public void save(@Nullable DataSource target) {
        if (target != null) {
            save(target.asOutputStream(), target.name(), target.size());
        } else {
            LOG.info("Not saving graph to disk, as requested.");
        }
    }

    /**
     * This method is an alternative to {@link #save(DataSource)} for tests and other purposes,
     * but should not be used within the main OTP application.
     */
    public void saveToFile(File file) throws IOException {
        try {
            save(new FileOutputStream(file), file.getName(), file.length());
        } catch (Exception e) {
            // remove half-written file
            file.deleteOnExit();
            throw e;
        }
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
        kryo.register(HashMultimap.class, new HashMultimapSerializer());

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


    /* private methods */

    private static SerializedGraphObject load(InputStream inputStream, String sourceDescription) {
        // TODO store version information, halt load if versions mismatch
        try(inputStream) {
            LOG.info("Reading graph from '{}'", sourceDescription);
            Input input = new Input(inputStream);
            Kryo kryo = makeKryo();
            SerializedGraphObject serObj = (SerializedGraphObject) kryo.readClassAndObject(input);
            Graph graph = serObj.graph;
            LOG.debug("Graph read.");
            if (graph.graphVersionMismatch()) {
                throw new RuntimeException("Graph version mismatch detected.");
            }
            serObj.reconstructEdgeLists();
            LOG.info("Graph read. |V|={} |E|={}", graph.countVertices(), graph.countEdges());
            return serObj;
        }
        catch (IOException e) {
            LOG.error("Exception while loading graph: {}", e.getLocalizedMessage(), e);
            return null;
        }
        catch (KryoException ke) {
            LOG.warn("Exception while loading graph: {}\n{}", sourceDescription, ke.getLocalizedMessage());
            throw new OtpAppException("Unable to load graph. The deserialization failed. Is the "
                    + "loaded graph build with the same OTP version as you are using to load it? "
                    + "Graph: " + sourceDescription);
        }
    }

    private void save(OutputStream outputStream, String graphName, long size) {
        LOG.info("Writing graph " + graphName + " ...");
        outputStream = wrapOutputStreamWithProgressTracker(outputStream, size);
        Kryo kryo = makeKryo();
        Output output = new Output(outputStream);
        kryo.writeClassAndObject(output, this);
        output.close();
        LOG.info("Graph written: {}", graphName);
        // Summarize serialized classes and associated serializers to stdout:
        // ((InstanceCountingClassResolver) kryo.getClassResolver()).summarize();
    }

    @SuppressWarnings("Convert2MethodRef")
    private static OutputStream wrapOutputStreamWithProgressTracker(OutputStream outputStream, long size) {
        return ProgressTracker.track(
                "Save graph",
                500_000,
                size,
                outputStream,
                // Keep this to get correct logging info for class and line number
                msg -> LOG.info(msg)
        );
    }

}
