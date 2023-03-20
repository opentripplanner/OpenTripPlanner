package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.schema.CoercingSerializeException;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.json.ObjectMappers;

class LegacyGraphQLScalarsTest {

  @Test
  void duration() {
    var string = LegacyGraphQLScalars.durationScalar
      .getCoercing()
      .serialize(Duration.ofMinutes(30));
    assertEquals("PT30M", string);
  }

  @Test
  void nonDuration() {
    Assertions.assertThrows(
      CoercingSerializeException.class,
      () -> LegacyGraphQLScalars.durationScalar.getCoercing().serialize(new Object())
    );
  }

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
    var geoJson = LegacyGraphQLScalars.geoJsonScalar.getCoercing().serialize(polygon);

    var jsonNode = ObjectMappers
      .ignoringExtraFields()
      .readTree(
        "{\"type\":\"Polygon\",\"coordinates\":[[[0.0,0.0],[1.0,1.0],[2.0,2.0],[0.0,0.0]]]}"
      );
    assertEquals(jsonNode, geoJson);
  }
}
