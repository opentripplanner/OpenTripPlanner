package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentripplanner.api.parameter.QualifiedModeSet;

import java.io.IOException;

/**
 * Serialize qualified mode sets as strings.
 */
public class QualifiedModeSetSerializer extends JsonSerializer<QualifiedModeSet> {

    @Override public void serialize(QualifiedModeSet qualifiedModeSet, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeString(qualifiedModeSet.toString());
    }

    /** Create a module including the serializer and deserializer for qualified mode sets */
    public static SimpleModule makeModule () {
        Version moduleVersion = new Version(1, 0, 0, null, null, null);
        SimpleModule module = new SimpleModule("QualifiedModeSet", moduleVersion);
        module.addSerializer(QualifiedModeSet.class, new QualifiedModeSetSerializer());
        module.addDeserializer(QualifiedModeSet.class, new QualifiedModeSetDeserializer());
        return module;
    }
}
