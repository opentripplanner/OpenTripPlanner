package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.language.StringValue;
import graphql.schema.CoercingSerializeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.json.ObjectMappers;

class GraphQLScalarsTest {

  static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.of(
    LocalDate.of(2024, 2, 4),
    LocalTime.MIDNIGHT,
    ZoneOffset.UTC
  );

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

  @Nested
  class OffsetDateTimeTests {

    static List<Arguments> offsetDateTimeCases() {
      return List.of(
        of(OFFSET_DATE_TIME, "2024-02-04T00:00:00Z"),
        of(OFFSET_DATE_TIME.plusHours(12).plusMinutes(8).plusSeconds(22), "2024-02-04T12:08:22Z"),
        of(
          OFFSET_DATE_TIME.atZoneSameInstant(ZoneIds.BERLIN).toOffsetDateTime(),
          "2024-02-04T01:00:00+01:00"
        ),
        of(
          OFFSET_DATE_TIME.atZoneSameInstant(ZoneIds.NEW_YORK).toOffsetDateTime(),
          "2024-02-03T19:00:00-05:00"
        )
      );
    }

    @ParameterizedTest
    @MethodSource("offsetDateTimeCases")
    void serializeOffsetDateTime(OffsetDateTime odt, String expected) {
      var string = GraphQLScalars.offsetDateTimeScalar.getCoercing().serialize(odt);
      assertEquals(expected, string);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTimeCases")
    void parseOffsetDateTime(OffsetDateTime expected, String input) {
      var odt = GraphQLScalars.offsetDateTimeScalar.getCoercing().parseValue(input);
      assertEquals(expected, odt);
    }

    @ParameterizedTest
    @MethodSource("offsetDateTimeCases")
    void parseOffsetDateTimeLiteral(OffsetDateTime expected, String input) {
      var odt = GraphQLScalars.offsetDateTimeScalar
        .getCoercing()
        .parseLiteral(new StringValue(input));
      assertEquals(expected, odt);
    }
  }
}
