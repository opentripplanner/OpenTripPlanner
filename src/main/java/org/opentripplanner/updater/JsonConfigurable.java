package org.opentripplanner.updater;

import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;

/**
 * Interface for a class than can be configured through a Jackson JSON tree.
 */
public interface JsonConfigurable {

    public abstract void configure (Graph graph, JsonNode jsonNode) throws Exception;

}
