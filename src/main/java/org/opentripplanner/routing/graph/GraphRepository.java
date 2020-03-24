package org.opentripplanner.routing.graph;

import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GraphRepository.class);

    private final DataSource graphOut;

    public GraphRepository(DataSource graphOut) {
        this.graphOut = graphOut;
    }

    public void verifyTheOutputGraphIsWritableIfDataSourceExist() {
        if (graphOut != null) {
            // Abort building a graph if the file can not be saved
            if (graphOut.exists()) {
                LOG.info("Graph already exists and will be overwritten at the end of the " + "build process. Graph: {}", graphOut.path());
            }
            if (!graphOut.isWritable()) {
                throw new RuntimeException(
                        "Cannot create or write to graph at: " + graphOut.path());
            }
        }
    }

    public void save(
            Graph graph, BuildConfig buildConfig, RouterConfig routerConfig
    ) {
        if (graphOut != null) {
            new SerializedGraphObject(graph, buildConfig, routerConfig).save(graphOut);
        } else {
            LOG.info("Not saving graph to disk, as requested.");
        }
    }
}
