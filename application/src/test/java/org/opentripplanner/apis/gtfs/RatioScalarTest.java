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

class RatioScalarTest {

  private static final Double HALF = 0.5;
  private static final Integer ZERO = 0;
  private static final Integer ONE = 1;
  private static final double TOO_HIGH = 1.1;
  private static final double TOO_LOW = -1.1;
  private static final String TEXT = "foo";
  private static final double DELTA = 0.0001;

  @Test
  void testSerialize() {
    var ratio = (Double) GraphQLScalars.RATIO_SCALAR.getCoercing().serialize(HALF);
    assertEquals(HALF, ratio, DELTA);
    ratio = (Double) GraphQLScalars.RATIO_SCALAR.getCoercing().serialize(HALF.floatValue());
    assertEquals(HALF, ratio, DELTA);
    assertThrows(CoercingSerializeException.class, () ->
      GraphQLScalars.RATIO_SCALAR.getCoercing().serialize(TEXT)
    );
  }

  @Test
  void testParseValue() {
    var ratio = (Double) GraphQLScalars.RATIO_SCALAR.getCoercing().parseValue(HALF);
    assertEquals(HALF, ratio, DELTA);
    ratio = (Double) GraphQLScalars.RATIO_SCALAR.getCoercing().parseValue(ZERO);
    assertEquals(ZERO, ratio, DELTA);
    ratio = (Double) GraphQLScalars.RATIO_SCALAR.getCoercing().parseValue(ONE);
    assertEquals(ONE, ratio, DELTA);
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.RATIO_SCALAR.getCoercing().parseValue(TEXT)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.RATIO_SCALAR.getCoercing().parseValue(TOO_LOW)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.RATIO_SCALAR.getCoercing().parseValue(TOO_HIGH)
    );
  }

  @Test
  void testParseLiteral() {
    var ratioDouble = (Double) GraphQLScalars.RATIO_SCALAR.getCoercing()
      .parseLiteral(new FloatValue(BigDecimal.valueOf(HALF)));
    assertEquals(HALF, ratioDouble, DELTA);
    var ratioInt = (Double) GraphQLScalars.RATIO_SCALAR.getCoercing()
      .parseLiteral(new IntValue(BigInteger.valueOf(HALF.intValue())));
    assertEquals(HALF.intValue(), ratioInt, DELTA);
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.RATIO_SCALAR.getCoercing().parseLiteral(new StringValue(TEXT))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.RATIO_SCALAR.getCoercing()
        .parseLiteral(new FloatValue(BigDecimal.valueOf(TOO_HIGH)))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.RATIO_SCALAR.getCoercing()
        .parseLiteral(new FloatValue(BigDecimal.valueOf(TOO_LOW)))
    );
  }
}
