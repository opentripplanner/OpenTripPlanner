package org.opentripplanner.graph_builder.module;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;

import java.util.HashMap;

/**
 * A graph builder that will embed the JSON graph builder and router configuration into the Graph.
 * This allows us to check whether a graph was built with a different configuration file than one found on disk,
 * and allows us to provide all configuration information to the server within a single Graph file.
 */
public class EmbedConfig implements GraphBuilderModule {

    /* We are storing the JSON as a string because JsonNodes are not serializable. */
    private final JsonNode builderConfig;
    private final JsonNode routerConfig;

    // maybe save the GraphBuilderParameters instead of the JSON
    public EmbedConfig(JsonNode builderConfig, JsonNode routerConfig) {
        this.builderConfig = builderConfig;
        this.routerConfig = routerConfig;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            graph.builderConfig = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(builderConfig);
            graph.routerConfig = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(routerConfig);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void checkInputs() {

    }

}
