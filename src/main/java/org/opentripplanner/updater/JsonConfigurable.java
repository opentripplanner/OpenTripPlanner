package org.opentripplanner.updater;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;

/**
 * Interface for a class than can be configured through a Jackson JSON tree.
 */
public interface JsonConfigurable {

    void configure (Graph graph, JsonNode updaterConfigItem) throws Exception;

}
