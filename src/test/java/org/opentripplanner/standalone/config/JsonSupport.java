package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonSupport {

    /**
     * Convert text to a JsonNode.
     *
     * Comments and unquoted fields are allowed as well as using ' instead of ".
     */
    public static JsonNode jsonNodeForTest(String jsonText) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

            // Replace ' with "
            jsonText = jsonText.replace("'", "\"");

            return mapper.readTree(jsonText);
        }
        catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static NodeAdapter newNodeAdapterForTest(String configText) {
        JsonNode config = jsonNodeForTest(configText);
        return new NodeAdapter(config, "Test");
    }
}
