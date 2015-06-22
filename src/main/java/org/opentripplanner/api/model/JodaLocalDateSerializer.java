package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

/**
 * Serialize localDates to YYYY-MM-DD
 */
public class JodaLocalDateSerializer extends JsonSerializer<LocalDate> {
    /** Create a module including the serializer and deserializer for local dates */
    public static SimpleModule makeModule () {
        Version moduleVersion = new Version(1, 0, 0, null, null, null);
        SimpleModule module = new SimpleModule("LocalDate", moduleVersion);
        module.addSerializer(LocalDate.class, new JodaLocalDateSerializer());
        module.addDeserializer(LocalDate.class, new JodaLocalDateDeserializer());
        return module;
    }

    @Override public void serialize(LocalDate localDate, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeString(localDate.toString(ISODateTimeFormat.date()));
    }
}