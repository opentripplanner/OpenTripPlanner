package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.json.ObjectMappers;

class GeoJsonScalarTest {

  @Test
  void geoJson() throws JsonProcessingException {
    var gm = new GeometryFactory();
    var polygon = gm.createPolygon(
      new Coordinate[] {
        new Coordinate(0, 0),
        new Coordinate(1, 1),
        new Coordinate(2, 2),
        new Coordinate(0, 0),
      }
    );
    var geoJson = GraphQLScalars.GEOJSON_SCALAR.getCoercing().serialize(polygon);

    var expected = ObjectMappers.ignoringExtraFields()
      .readTree(
        "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[1.0,1.0],[2.0,2.0],[0.0,0.0]]]}"
      );
    assertEquals(expected.toString(), geoJson.toString());
  }
}
