package org.opentripplanner.framework.graphql.scalar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import graphql.schema.GraphQLScalarType;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DateScalarFactoryTest {

  private static final GraphQLScalarType SCALAR = DateScalarFactory.createDateScalar("Date");

  @ParameterizedTest
  @ValueSource(strings = { "2024-05-23", "20240523" })
  void parse(String input) {
    var result = SCALAR.getCoercing().parseValue(input);
    assertInstanceOf(LocalDate.class, result);
    var date = (LocalDate) result;
    assertEquals(LocalDate.of(2024, 5, 23), date);
  }
}
