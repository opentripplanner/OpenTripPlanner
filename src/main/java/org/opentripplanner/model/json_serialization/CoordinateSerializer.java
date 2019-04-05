package org.opentripplanner.model.json_serialization;

import java.io.IOException;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.locationtech.jts.geom.Coordinate;

public class CoordinateSerializer extends JsonSerializer<Coordinate> {

    @Override
    public void serialize(Coordinate value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        jgen.writeObject(value.x);
        jgen.writeObject(value.y);
        jgen.writeEndArray();
    }
    
    public Class<Coordinate> handledType() {
        return Coordinate.class;
    }
    
}
