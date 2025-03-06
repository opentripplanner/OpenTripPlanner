package org.opentripplanner.framework.graphql.scalar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DateScalarFactoryTest {

  private static final GraphQLScalarType GTFS_SCALAR = DateScalarFactory.createGtfsDateScalar();
  private static final GraphQLScalarType TRANSMODEL_SCALAR =
    DateScalarFactory.createTransmodelDateScalar();
  private static final List<String> INVALID_DATES = List.of(
    "2024-05",
    "2024",
    "2024-99-04",
    "202405-23",
    "20240523"
  );

  static Stream<Arguments> succesfulCases() {
    return Stream.of(GTFS_SCALAR, TRANSMODEL_SCALAR).map(s -> Arguments.of(s, "2024-05-23"));
  }

  @ParameterizedTest
  @MethodSource("succesfulCases")
  void parse(GraphQLScalarType scalar, String input) {
    var result = scalar.getCoercing().parseValue(input);
    assertInstanceOf(LocalDate.class, result);
    var date = (LocalDate) result;
    assertEquals(LocalDate.of(2024, 5, 23), date);
  }

  static Stream<Arguments> invalidCases() {
    return INVALID_DATES.stream()
      .flatMap(date ->
        Stream.of(Arguments.of(GTFS_SCALAR, date), Arguments.of(TRANSMODEL_SCALAR, date))
      );
  }

  @ParameterizedTest
  @MethodSource("invalidCases")
  void failParsing(GraphQLScalarType scalar, String input) {
    assertThrows(CoercingParseValueException.class, () -> scalar.getCoercing().parseValue(input));
  }
}
