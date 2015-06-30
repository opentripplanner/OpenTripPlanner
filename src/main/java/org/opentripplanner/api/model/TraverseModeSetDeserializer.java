package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.io.IOException;

/**
 * Deserialize a TraverseModeSet from its string constituents.
 */
public class TraverseModeSetDeserializer extends JsonDeserializer<TraverseModeSet> {
    @Override public TraverseModeSet deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        return new TraverseModeSet(jsonParser.getValueAsString());
    }
}
