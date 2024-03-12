package org.opentripplanner.apis.transmodel.model.scalars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner._support.time.ZoneIds.OSLO;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DateTimeScalarFactoryTest {

  public static final String DATE_TIME = "2023-01-27T12:59:00+01:00";
  public static final Long EPOCH_MILLIS = Instant.parse(DATE_TIME).toEpochMilli();
  private static GraphQLScalarType subject;

  @BeforeAll
  static void setup() {
    subject = DateTimeScalarFactory.createMillisecondsSinceEpochAsDateTimeStringScalar(OSLO);
  }

  @Test
  void serialize() {
    var result = subject.getCoercing().serialize(EPOCH_MILLIS);
    assertEquals(DATE_TIME, result);
  }

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(DATE_TIME),
      Arguments.of("2023-01-27T12:59:00.000+01:00"),
      Arguments.of("2023-01-27T12:59:00+0100"),
      Arguments.of("2023-01-27T12:59:00+01"),
      Arguments.of("2023-01-27T12:59:00"),
      Arguments.of("2023-01-27T11:59:00Z")
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void parse(String input) {
    var result = subject.getCoercing().parseValue(input);
    assertEquals(EPOCH_MILLIS, result);
  }

  @Test
  void parseInvalidInputType() {
    Coercing<?, ?> coercing = subject.getCoercing();
    assertThrows(CoercingParseValueException.class, () -> coercing.parseValue(Boolean.TRUE));
  }
}
