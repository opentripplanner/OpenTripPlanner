package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.opentripplanner.api.parameter.QualifiedModeSet;

import java.io.IOException;

/**
 * Deserialize qualified mode sets from strings.
 */
public class QualifiedModeSetDeserializer extends JsonDeserializer<QualifiedModeSet> {

    @Override public QualifiedModeSet deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        return new QualifiedModeSet(jsonParser.getValueAsString());
    }
}
