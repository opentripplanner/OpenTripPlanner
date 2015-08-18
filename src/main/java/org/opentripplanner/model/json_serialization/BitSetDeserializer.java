package org.opentripplanner.model.json_serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.BitSet;

/**
 * Deserialize an array [true, false, true . . .] to a bitset
 */
public class BitSetDeserializer extends JsonDeserializer<BitSet> {
    @Override public BitSet deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        BitSet ret = new BitSet();

        int i = 0;
        JsonToken token;
        while (!JsonToken.END_ARRAY.equals(token = jsonParser.nextValue())) {
            if (JsonToken.VALUE_TRUE.equals(token))
                ret.set(i);
            i++;
        }

        return ret;
    }
}
