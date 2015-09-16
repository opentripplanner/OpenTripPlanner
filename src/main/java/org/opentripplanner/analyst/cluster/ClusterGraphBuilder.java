package org.opentripplanner.analyst.cluster;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.standalone.CommandLineParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Builds and caches graphs as well as the inputs they are built from for use in Analyst Cluster workers.
 */
public class ClusterGraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterGraphBuilder.class);

    private AmazonS3Client s3 = new AmazonS3Client();

    private static final String GRAPH_CACHE_DIR = "graph_cache";

    private final String graphBucket;

    String currGraphId = null;

    Graph currGraph = null;

    public ClusterGraphBuilder (String graphBucket) {
        this.graphBucket = graphBucket;
    }

    /**
     * Return the graph for the given unique identifier for graph builder inputs on S3.
     * If this is the same as the last graph built, just return the pre-built graph.
     * If not, build the graph from the inputs, fetching them from S3 to the local cache as needed.
     */
    public synchronized Graph getGraph(String graphId) {

        LOG.info("Finding a graph for ID {}", graphId);

        if (graphId.equals(currGraphId)) {
            LOG.info("GraphID has not changed. Reusing the last graph that was built.");
            return currGraph;
        }

        // The location of the inputs that will be used to build this graph
        File graphDataDirectory = new File(GRAPH_CACHE_DIR, graphId);

        // If we don't have a local copy of the inputs, fetch graph data as a ZIP from S3 and unzip it
        if( ! graphDataDirectory.exists() || graphDataDirectory.list().length == 0) {
            LOG.info("Downloading graph input files.");
            graphDataDirectory.mkdirs();
            S3Object graphDataZipObject = s3.getObject(graphBucket, graphId + ".zip");
            ZipInputStream zis = new ZipInputStream(graphDataZipObject.getObjectContent());
            try {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File entryDestination = new File(graphDataDirectory, entry.getName());
                    // Are both these mkdirs calls necessary?
                    entryDestination.getParentFile().mkdirs();
                    if (entry.isDirectory())
                        entryDestination.mkdirs();
                    else {
                        OutputStream entryFileOut = new FileOutputStream(entryDestination);
                        IOUtils.copy(zis, entryFileOut);
                        entryFileOut.close();
                    }
                }
                zis.close();
            } catch (Exception e) {
                // TODO delete graph cache dir which is probably corrupted
                LOG.info("Error retrieving graph files", e);
            }
        } else {
            LOG.info("Graph input files were found locally. Using these files from the cache.");
        }

        // Now we have a local copy of these graph inputs. Make a graph out of them.
        CommandLineParameters params = new CommandLineParameters();
        params.build = new File(GRAPH_CACHE_DIR, graphId);
        params.inMemory = true;
        GraphBuilder graphBuilder = GraphBuilder.forDirectory(params, params.build);
        graphBuilder.run();
        Graph graph = graphBuilder.getGraph();
        graph.routerId = graphId;
        graph.index(new DefaultStreetVertexIndexFactory());
        graph.index.clusterStopsAsNeeded();
        this.currGraphId = graphId;
        this.currGraph = graph;
        return graph;

    }

}
