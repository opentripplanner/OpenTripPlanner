package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.GraphBuildParameters;
import org.opentripplanner.standalone.config.RouterConfigParams;

import java.util.HashMap;

/**
 * A graph builder that will embed the JSON graph builder and router configuration into the Graph.
 * This allows us to check whether a graph was built with a different configuration file than one found on disk,
 * and allows us to provide all configuration information to the server within a single Graph file.
 */
public class EmbedConfig implements GraphBuilderModule {

    /* We are storing the JSON as a string because JsonNodes are not serializable. */
    private final GraphBuildParameters buildConfig;
    private final RouterConfigParams routerConfig;

    // maybe save the GraphBuilderParameters instead of the JSON
    public EmbedConfig(GraphBuildParameters buildConfig, RouterConfigParams routerConfig) {
        this.buildConfig = buildConfig;
        this.routerConfig = routerConfig;
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        try {
            graph.buildConfig = buildConfig;
            graph.routerConfig = routerConfig;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void checkInputs() { }
}
