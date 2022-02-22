package org.opentripplanner.routing.graph;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.model.projectinfo.GraphFileHeader;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;
import org.opentripplanner.routing.graph.kryosupport.KryoBuilder;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.util.OtpAppException;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


    /* private methods */

    private static SerializedGraphObject load(InputStream inputStream, String sourceDescription) {
        // TODO store version information, halt load if versions mismatch
        try(inputStream) {
            LOG.info("Reading graph from '{}'", sourceDescription);
            Input input = new Input(inputStream);

            validateGraphSerializationId(
                input.readBytes(GraphFileHeader.headerLength()),
                sourceDescription
            );

            Kryo kryo = KryoBuilder.create();
            SerializedGraphObject serObj = (SerializedGraphObject) kryo.readClassAndObject(input);
            Graph graph = serObj.graph;
            LOG.debug("Graph read.");
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
        Kryo kryo = KryoBuilder.create();
        Output output = new Output(outputStream);
        output.write(OtpProjectInfo.projectInfo().graphFileHeaderInfo.header());
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

    private static void validateGraphSerializationId(byte[] header, String sourceName) {
        var expFileHeader = projectInfo().graphFileHeaderInfo;
        var graphFileHeader = GraphFileHeader.parse(header);

        if(!expFileHeader.equals(graphFileHeader)) {
            if (!expFileHeader.equals(graphFileHeader)) {
                throw new OtpAppException(
                    "The graph file is incompatible with this version of OTP. "
                        + "The OTP serialization version id '%s' do not match the id "
                        + "'%s' in '%s' file-header.",
                    expFileHeader.otpSerializationVersionId(),
                    graphFileHeader.otpSerializationVersionId(),
                    sourceName
                );
            }
        }
    }
}
