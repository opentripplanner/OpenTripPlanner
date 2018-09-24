package org.opentripplanner.model.json_serialization;

import java.io.IOException;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PackedCoordinateSequenceSerializer extends JsonSerializer<PackedCoordinateSequence> {
    int precision = 2;
    
    @Override
    public void serialize(PackedCoordinateSequence value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        for (int i = 0; i < value.size(); ++i) {
            jgen.writeStartArray();
            for (int j = 0; j < value.getDimension(); j++) {
                double ordinate = value.getOrdinate(i, j);
                if (!Double.isNaN(ordinate)) {
                    if (precision == -1) {
                        jgen.writeObject(ordinate);
                    } else {
                        jgen.writeRawValue(String.format("%." + precision + "f", ordinate));
                    }
                }
            }
            jgen.writeEndArray();
        }
        jgen.writeEndArray();
    }
    
    @Override
    public Class<PackedCoordinateSequence> handledType() {
        return PackedCoordinateSequence.class;
    }

}
