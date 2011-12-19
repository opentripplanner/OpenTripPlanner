package org.opentripplanner.api.model.json_serializers;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;

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
