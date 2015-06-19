package org.opentripplanner.api.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;

/** serializer/deserializer for LocalDates to ISO dates, YYYY-MM-DD */
public class JodaLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override public LocalDate deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        return DateTimeFormat.forPattern("YYYY-MM-DD").parseLocalDate(jsonParser.getValueAsString());
    }
}
