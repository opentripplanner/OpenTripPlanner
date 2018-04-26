package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.opentripplanner.model.FeedId;

import java.io.IOException;

public class FeedIdDeserializer extends JsonDeserializer<FeedId> {

    public static final String SEPARATOR = ":";

    @Override
    public FeedId deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        String string = jp.getValueAsString();
        String[] parts = string.split(SEPARATOR, 2);
        return new FeedId(parts[0], parts[1]);
    }

    // Gets around type erasure, allowing
    // module.addSerializer(new ThingSerializer()) to correctly associate 
    // this serializer with the proper type.
    @Override
    public Class<FeedId> handledType() {
        return FeedId.class;
    }

}
