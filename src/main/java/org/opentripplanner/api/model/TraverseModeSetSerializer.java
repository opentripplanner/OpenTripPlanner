package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.io.IOException;

/**
 * Serialize traverse mode sets as strings.
 */
public class TraverseModeSetSerializer extends JsonSerializer<TraverseModeSet> {
    /** Create a module including the serializer and deserializer for local dates */
    public static SimpleModule makeModule () {
        Version moduleVersion = new Version(1, 0, 0, null, null, null);
        SimpleModule module = new SimpleModule("TraverseModeSet", moduleVersion);
        module.addSerializer(TraverseModeSet.class, new TraverseModeSetSerializer());
        module.addDeserializer(TraverseModeSet.class, new TraverseModeSetDeserializer());
        return module;
    }

    @Override public void serialize(TraverseModeSet traverseModeSet, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeString(traverseModeSet.getAsStr());
    }
}
