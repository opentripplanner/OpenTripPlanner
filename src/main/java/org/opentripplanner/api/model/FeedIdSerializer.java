package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentripplanner.model.FeedId;

import java.io.IOException;

public class FeedIdSerializer extends JsonSerializer<FeedId> {

    public static final String SEPARATOR = ":";

    /** This creates a Jackson module including both the serializer and deserializer for AgencyAndIds. */
    public static SimpleModule makeModule () {
        Version moduleVersion = new Version(1, 0, 0, null, null, null);
        SimpleModule module = new SimpleModule("OTP", moduleVersion);
        module.addSerializer(FeedId.class, new FeedIdSerializer());
        module.addDeserializer(FeedId.class, new FeedIdDeserializer());
        // Map key (de)serializers are always separate from value ones, because they must be strings.
        module.addKeyDeserializer(FeedId.class, new FeedIdKeyDeserializer());
        return module;
    }

    @Override
    public void serialize(FeedId a, JsonGenerator gen, SerializerProvider prov)
            throws IOException, JsonProcessingException {
        gen.writeString(a.getAgencyId() + SEPARATOR + a.getId());
    }

    // Gets around type erasure, allowing
    // module.addSerializer(new ThingSerializer()) to correctly associate 
    // this serializer with the proper type.
    @Override
    public Class<FeedId> handledType() {
        return FeedId.class;
    }

}
