package org.opentripplanner.api.model;

import java.io.IOException;

import org.onebusaway.gtfs.model.AgencyAndId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class AgencyAndIdSerializer extends JsonSerializer<AgencyAndId> {

    public static final String SEPARATOR = ":";

    public static SimpleModule makeModule () {
        Version moduleVersion = new Version(1, 0, 0, null, null, null);
        SimpleModule module = new SimpleModule("OTP", moduleVersion);
        module.addSerializer(AgencyAndId.class, new AgencyAndIdSerializer());
        return module;
    }

    @Override
    public void serialize(AgencyAndId a, JsonGenerator gen, SerializerProvider prov)
            throws IOException, JsonProcessingException {
        gen.writeString(a.getAgencyId() + SEPARATOR + a.getId());
    }

    // Gets around type erasure, allowing 
    // module.addSerializer(new ThingSerializer()) to correctly associate 
    // this serializer with the proper type.
    @Override
    public Class<AgencyAndId> handledType() {
        return AgencyAndId.class; 
    }

}
