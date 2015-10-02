package org.opentripplanner.model.json_serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.BitSet;

/**
 * Serialize a BitSet to an array [true, false . . .].
 */
public class BitSetSerializer extends JsonSerializer<BitSet> {

    @Override public void serialize(BitSet bitSet, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        jsonGenerator.writeStartArray();

        for (int i = 0; i < bitSet.length(); i++) {
            jsonGenerator.writeBoolean(bitSet.get(i));
        }

        jsonGenerator.writeEndArray();
    }
}
