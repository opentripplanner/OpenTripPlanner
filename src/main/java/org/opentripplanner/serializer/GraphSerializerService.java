package org.opentripplanner.serializer;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.opentripplanner.serializer.GraphSerializerService.SerializationMethod.JAVA;

/**
 * Service for serializing and deserializing Graph objects.
 * Load and save methods are extracted from the {@link Graph}.
 * This implementation allows to switch implementation of GraphSerializer.
 * Having multiple implementations makes it easier to compare speed and size of different implementations.
 */
public class GraphSerializerService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphSerializerService.class);
    public static final String SERIALIZATION_METHOD_PROP = "serialization-method";


    public enum SerializationMethod {KRYO, JAVA, PROTOSTUFF}

    public static final SerializationMethod DEFAULT_SERIALIZATION_METHOD = SerializationMethod.JAVA;

    /**
     * Instantiated here. Seems like there is only one implementation.
     * This class could get the index factory implementation as constructor parameter, if one wanted to override.
     */
    private final StreetVertexIndexFactory streetVertexIndexFactory = new DefaultStreetVertexIndexFactory();

    private final GraphSerializer graphSerializer;


    public GraphSerializerService() {
        SerializationMethod serializationMethod;
        String serializationMethodString = System.getProperty(SERIALIZATION_METHOD_PROP);
        if (serializationMethodString == null) {
            LOG.info("Choosing the the default serialization method: " + DEFAULT_SERIALIZATION_METHOD);
            serializationMethod = DEFAULT_SERIALIZATION_METHOD;
        } else {
            serializationMethod = SerializationMethod.valueOf(serializationMethodString);
        }
        this.graphSerializer = getGraphSerializer(serializationMethod);
    }

    public GraphSerializerService(SerializationMethod serializationMethod) {
        this.graphSerializer = getGraphSerializer(serializationMethod);
    }

    public GraphSerializerService(GraphSerializer graphSerializer) {
        this.graphSerializer = graphSerializer;
    }

    public Graph load(File file) {
        GraphWrapper graphWrapper = deserialize(file);
        return prepareGraphAfterDeserialization(graphWrapper);
    }

    public Graph load(InputStream inputStream) {
        GraphWrapper graphWrapper = deserialize(inputStream);
        return prepareGraphAfterDeserialization(graphWrapper);
    }

    public void save(Graph graph, OutputStream outputStream) {
        serialize(graph, outputStream);
    }

    public void save(Graph graph, File file) {
        serialize(graph, file);
    }

    private void serialize(Graph graph, File file) {
        try {
            LOG.info("Writing graph to file {} using {}", file.getName(), graphSerializer.getClass().getName());
            serialize(graph, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new GraphSerializationException("Cannot read file " + file.getName(), e);
        }
    }

    private void serialize(Graph graph, OutputStream outputStream) {
        GraphWrapper graphWrapper = prepareAndWrap(graph);
        LOG.info("Serializing graph. Main graph size: |V|={} |E|={}", graphWrapper.graph.countVertices(), graphWrapper.graph.countEdges());
        long started = System.currentTimeMillis();
        graphSerializer.serialize(graphWrapper, outputStream);
        long spent = System.currentTimeMillis() - started;
        LOG.info("Graph serialized in {} ms", spent);
    }

    /***
     * Beacuse of legacy reasons the graph object and edges were serialized separately these will be wrapped.
     *
     * Methods for rebuilding vertex/edge/ID numbers and consolidating edges has been moved to this class,
     * so that all implementations of {@link GraphSerializer} can benefit from it.
     *
     * @param graph the graph to wrap before serialization
     * @return the wrapped graph with edges separated
     */
    private GraphWrapper prepareAndWrap(Graph graph) {

        LOG.debug("Preparing graph for serialization");

        GraphWrapper graphWrapper = new GraphWrapper();

        LOG.debug("Assigning vertex/edge ID numbers...");
        graph.rebuildVertexAndEdgeIndices();

        graphWrapper.graph = graph;
        graphWrapper.edges = consolidateEdges(graph);

        return graphWrapper;
    }

    private List<Edge> consolidateEdges(Graph graph) {
        LOG.debug("Consolidating edges...");
        // this is not space efficient
        List<Edge> edges = new ArrayList<Edge>(graph.countEdges());
        for (Vertex v : graph.getVertices()) {
            // there are assumed to be no edges in an incoming list that are not
            // in an outgoing list
            edges.addAll(v.getOutgoing());
            if (v.getDegreeOut() + v.getDegreeIn() == 0)
                LOG.debug("vertex {} has no edges, it will not survive serialization.", v);
        }
        return edges;
    }

    /**
     * Prepare the graph after deserialization.
     * This method should be called regardless of implementaiton of {@link GraphSerializer}.
     *
     * The method reads from the already deserialized wrapper object,
     * it checks the version of the deserialized graph and rebuilds vertices from edges.
     * At last, this method calls the index method.
     *
     * @param graphWrapper the deserializaed wrapper object
     * @return the prepared graph object, indexed and ready for use.
     */
    private Graph prepareGraphAfterDeserialization(GraphWrapper graphWrapper) {

        // Because some fields are marked as transient
        Graph deserializedGraph = graphWrapper.graph;
        List<Edge> edges = graphWrapper.edges;

        LOG.debug("Basic graph info read.");
        if (deserializedGraph.graphVersionMismatch())
            throw new RuntimeException("Graph version mismatch detected.");

        // vertex edge lists are transient to avoid excessive recursion depth
        // vertex list is transient because it can be reconstructed from edges
        deserializedGraph.vertices = new HashMap<>();

        for (Edge e : edges) {
            deserializedGraph.vertices.put(e.getFromVertex().getLabel(), e.getFromVertex());
            deserializedGraph.vertices.put(e.getToVertex().getLabel(), e.getToVertex());
        }

        LOG.info("Main graph read. |V|={} |E|={}", deserializedGraph.countVertices(), deserializedGraph.countEdges());
        deserializedGraph.index(streetVertexIndexFactory);

        return deserializedGraph;

    }

    private GraphWrapper deserialize(File file) {
        try {
            LOG.info("Reading graph from file: " + file.getAbsolutePath() + " ...");
            return deserialize(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new GraphSerializationException("Cannot read file " + file.getName(), e);
        }
    }

    private GraphWrapper deserialize(InputStream is) {
        long started = System.currentTimeMillis();
        GraphWrapper graphWrapper = graphSerializer.deserialize(is);
        long spent = System.currentTimeMillis() - started;

        LOG.info("Deserialized graph using: {} in {} ms", graphSerializer.getClass().getSimpleName(), spent);

        return graphWrapper;
    }

    public GraphSerializer getGraphSerializer() {
        return graphSerializer;
    }

    /**
     * Instantiating the correct graph serializer based on serialization method.
     *
     * @param serializationMethod if null or empty,
     * @return
     */
    private static GraphSerializer getGraphSerializer(SerializationMethod serializationMethod) {

        GraphSerializer graphSerializer;

        if (JAVA.equals(serializationMethod)) {
            graphSerializer = new JavaGraphSerializer();
        } else {
            throw new GraphSerializationException("Cannot find implementation for serializer with name: " + serializationMethod);
        }

        LOG.info("Using the following serializer implementaiton for graph loading/saving: {}", graphSerializer.getClass().getSimpleName());
        return graphSerializer;
    }

}
