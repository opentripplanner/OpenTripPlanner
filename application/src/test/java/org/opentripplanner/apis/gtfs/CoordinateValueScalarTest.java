package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class CoordinateValueScalarTest {

  private static final Double COORDINATE = 10.0;
  private static final Integer COORDINATE_INT = 10;
  private static final double COORDINATE_MAX = 180.0;
  private static final double COORDINATE_MIN = 180.0;
  private static final double TOO_HIGH = 190;
  private static final double TOO_LOW = -190;
  private static final String TEXT = "foo";
  private static final double DELTA = 0.0001;

  @Test
  void testSerialize() {
    var coordinate = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .serialize(COORDINATE);
    assertEquals(COORDINATE, coordinate, DELTA);
    coordinate = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .serialize(COORDINATE.floatValue());
    assertEquals(COORDINATE, coordinate, DELTA);
    assertThrows(CoercingSerializeException.class, () ->
      GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing().serialize(TEXT)
    );
  }

  @Test
  void testParseValue() {
    var coordinate = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .parseValue(COORDINATE);
    assertEquals(COORDINATE, coordinate, DELTA);
    coordinate = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .parseValue(COORDINATE_MIN);
    assertEquals(COORDINATE_MIN, coordinate, DELTA);
    coordinate = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .parseValue(COORDINATE_MAX);
    assertEquals(COORDINATE_MAX, coordinate, DELTA);
    coordinate = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .parseValue(COORDINATE_INT);
    assertEquals(COORDINATE_INT, coordinate, DELTA);
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing().parseValue(TEXT)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing().parseValue(TOO_LOW)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing().parseValue(TOO_HIGH)
    );
  }

  @Test
  void testParseLiteral() {
    var coordinateDouble = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .parseLiteral(new FloatValue(BigDecimal.valueOf(COORDINATE)));
    assertEquals(COORDINATE, coordinateDouble, DELTA);
    var coordinateInt = (Double) GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
      .parseLiteral(new IntValue(BigInteger.valueOf(COORDINATE.intValue())));
    assertEquals(COORDINATE, coordinateInt, DELTA);
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing().parseLiteral(new StringValue(TEXT))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
        .parseLiteral(new FloatValue(BigDecimal.valueOf(TOO_HIGH)))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.COORDINATE_VALUE_SCALAR.getCoercing()
        .parseLiteral(new FloatValue(BigDecimal.valueOf(TOO_LOW)))
    );
  }
}
