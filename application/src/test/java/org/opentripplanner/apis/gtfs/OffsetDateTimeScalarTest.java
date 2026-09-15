package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import graphql.language.StringValue;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;

public class OffsetDateTimeScalarTest {

  static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.of(
    LocalDate.of(2024, 2, 4),
    LocalTime.MIDNIGHT,
    ZoneOffset.UTC
  );

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
    var string = GraphQLScalars.OFFSET_DATETIME_SCALAR.getCoercing().serialize(odt);
    assertEquals(expected, string);
  }

  @ParameterizedTest
  @MethodSource("offsetDateTimeCases")
  void parseOffsetDateTime(OffsetDateTime expected, String input) {
    var odt = GraphQLScalars.OFFSET_DATETIME_SCALAR.getCoercing().parseValue(input);
    assertEquals(expected, odt);
  }

  @ParameterizedTest
  @MethodSource("offsetDateTimeCases")
  void parseOffsetDateTimeLiteral(OffsetDateTime expected, String input) {
    var odt = GraphQLScalars.OFFSET_DATETIME_SCALAR.getCoercing()
      .parseLiteral(new StringValue(input));
    assertEquals(expected, odt);
  }
}
