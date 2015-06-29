package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.onebusaway.gtfs.model.AgencyAndId;

import java.io.IOException;

public class AgencyAndIdDeserializer extends JsonDeserializer<AgencyAndId> {

    public static final String SEPARATOR = ":";

    @Override
    public AgencyAndId deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        String string = jp.getValueAsString();
        String[] parts = string.split(SEPARATOR, 2);
        return new AgencyAndId(parts[0], parts[1]);
    }

    // Gets around type erasure, allowing
    // module.addSerializer(new ThingSerializer()) to correctly associate 
    // this serializer with the proper type.
    @Override
    public Class<AgencyAndId> handledType() {
        return AgencyAndId.class; 
    }

}
