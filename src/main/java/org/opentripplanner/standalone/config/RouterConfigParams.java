package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class is an object representation of the 'router-config.json'.
 */
public class RouterConfigParams {
    /**
     * The raw JsonNode three kept for reference and (de)serialization.
     */
    public final JsonNode rawJson;

    // TODO OTP2 - Add routing parameters here

    public RouterConfigParams(NodeAdapter c) {
        rawJson = c.asRawNode();
    }
}
