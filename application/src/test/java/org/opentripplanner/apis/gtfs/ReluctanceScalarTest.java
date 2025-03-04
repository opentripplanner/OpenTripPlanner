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

class ReluctanceScalarTest {

  private static final Double HALF = 0.5;
  private static final Integer ONE = 1;
  private static final double TOO_HIGH = 100001;
  private static final double TOO_LOW = 0;
  private static final String TEXT = "foo";
  private static final double DELTA = 0.0001;

  @Test
  void testSerialize() {
    var reluctance = (Double) GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().serialize(HALF);
    assertEquals(HALF, reluctance, DELTA);
    reluctance = (Double) GraphQLScalars.RELUCTANCE_SCALAR.getCoercing()
      .serialize(HALF.floatValue());
    assertEquals(HALF, reluctance, DELTA);
    assertThrows(CoercingSerializeException.class, () ->
      GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().serialize(TEXT)
    );
  }

  @Test
  void testParseValue() {
    var reluctanceDouble = (Double) GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().parseValue(HALF);
    assertEquals(HALF, reluctanceDouble, DELTA);
    var reluctanceInt = (Double) GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().parseValue(ONE);
    assertEquals(ONE, reluctanceInt, DELTA);
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().parseValue(TEXT)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().parseValue(TOO_LOW)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().parseValue(TOO_HIGH)
    );
  }

  @Test
  void testParseLiteral() {
    var reluctanceDouble = (Double) GraphQLScalars.RELUCTANCE_SCALAR.getCoercing()
      .parseLiteral(new FloatValue(BigDecimal.valueOf(HALF)));
    assertEquals(HALF, reluctanceDouble, DELTA);
    var reluctanceInt = (Double) GraphQLScalars.RELUCTANCE_SCALAR.getCoercing()
      .parseLiteral(new IntValue(BigInteger.valueOf(ONE)));
    assertEquals(ONE, reluctanceInt, DELTA);
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.RELUCTANCE_SCALAR.getCoercing().parseLiteral(new StringValue(TEXT))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.RELUCTANCE_SCALAR.getCoercing()
        .parseLiteral(new FloatValue(BigDecimal.valueOf(TOO_HIGH)))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.RELUCTANCE_SCALAR.getCoercing()
        .parseLiteral(new FloatValue(BigDecimal.valueOf(TOO_LOW)))
    );
  }
}
