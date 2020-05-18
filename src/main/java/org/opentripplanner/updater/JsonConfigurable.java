package org.opentripplanner.updater;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for a class than can be configured through a Jackson JSON tree.
 */
public interface JsonConfigurable {

    void configure (JsonNode updaterConfigItem) throws Exception;

}
