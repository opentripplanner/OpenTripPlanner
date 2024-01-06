package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.schema.CoercingSerializeException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.json.ObjectMappers;

class GraphQLScalarsTest {

  static List<Arguments> durationCases() {
    return List.of(
      of(Duration.ofMinutes(30), "PT30M"),
      of(Duration.ofHours(23), "PT23H"),
      of(Duration.ofMinutes(-10), "PT-10M")
    );
  }

  @ParameterizedTest
  @MethodSource("durationCases")
  void duration(Duration duration, String expected) {
    var string = GraphQLScalars.durationScalar.getCoercing().serialize(duration);
    assertEquals(expected, string);
  }

  @Test
  void nonDuration() {
    Assertions.assertThrows(
      CoercingSerializeException.class,
      () -> GraphQLScalars.durationScalar.getCoercing().serialize(new Object())
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
    var geoJson = GraphQLScalars.geoJsonScalar.getCoercing().serialize(polygon);

    var jsonNode = ObjectMappers
      .ignoringExtraFields()
      .readTree("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,1],[2,2],[0,0]]]}");
    assertEquals(jsonNode.toString(), geoJson.toString());
  }
}
