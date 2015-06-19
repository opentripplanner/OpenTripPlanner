package org.opentripplanner.api.model;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.onebusaway.gtfs.model.AgencyAndId;

import java.io.IOException;

// Map key (de)serializers are always separate from value ones, because they must be strings.
public class AgencyAndIdKeyDeserializer extends KeyDeserializer {

    public static final String SEPARATOR = ":";

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        String[] parts = key.split(SEPARATOR, 2);
        return new AgencyAndId(parts[0], parts[1]);
    }
}
