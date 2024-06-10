package org.opentripplanner.framework.graphql.scalar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DateScalarFactoryTest {

  private static final GraphQLScalarType SCALAR = DateScalarFactory.createDateScalar("Date", null);

  @ParameterizedTest
  @ValueSource(strings = { "2024-05-23" })
  void parse(String input) {
    var result = SCALAR.getCoercing().parseValue(input);
    assertInstanceOf(LocalDate.class, result);
    var date = (LocalDate) result;
    assertEquals(LocalDate.of(2024, 5, 23), date);
  }

  @ParameterizedTest
  @ValueSource(strings = { "2024-05", "2024", "2024-99-04", "202405-23", "20240523" })
  void failParsing(String input) {
    assertThrows(CoercingParseValueException.class, () -> SCALAR.getCoercing().parseValue(input));
  }
}
