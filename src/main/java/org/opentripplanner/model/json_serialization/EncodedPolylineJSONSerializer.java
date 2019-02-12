package org.opentripplanner.model.json_serialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.opentripplanner.util.PolylineEncoder;



import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class EncodedPolylineJSONSerializer extends JsonSerializer<Geometry> {

    @Override
    public void serialize(Geometry arg, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        
        if (arg == null) {
            jgen.writeNull();
        }
        Coordinate[] lineCoords = arg.getCoordinates();
        List<Coordinate> coords = Arrays.asList(lineCoords);
        
        jgen.writeObject(PolylineEncoder.createEncodings(coords).getPoints());
    }

    @Override
    public Class<Geometry> handledType() {
        return Geometry.class;
    }
}
