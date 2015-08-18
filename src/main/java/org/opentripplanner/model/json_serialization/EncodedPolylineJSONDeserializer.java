package org.opentripplanner.model.json_serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.io.IOException;
import java.util.List;

/**
 * Decode an encoded polyline.
 */
public class EncodedPolylineJSONDeserializer extends JsonDeserializer<Geometry> {
    @Override public Geometry deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        EncodedPolylineBean bean = new EncodedPolylineBean(jsonParser.getValueAsString(), null, 0);
        List<Coordinate> coords = PolylineEncoder.decode(bean);
        return GeometryUtils.getGeometryFactory().createLineString(coords.toArray(new Coordinate[coords.size()]));
    }
}
