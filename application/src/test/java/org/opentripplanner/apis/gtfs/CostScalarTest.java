package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;

class CostScalarTest {

  private static final Cost COST_THIRTY = Cost.costOfSeconds(30);
  private static final int THIRTY = 30;
  private static final int NEGATIVE_THIRTY = -30;
  private static final int TOO_HIGH = 300000000;
  private static final String TEXT = "foo";

  @Test
  void testSerialize() {
    var cost = GraphQLScalars.COST_SCALAR.getCoercing().serialize(COST_THIRTY);
    assertEquals(THIRTY, cost);
    var costNumber = GraphQLScalars.COST_SCALAR.getCoercing().serialize(THIRTY);
    assertEquals(THIRTY, costNumber);
    assertThrows(CoercingSerializeException.class, () ->
      GraphQLScalars.COST_SCALAR.getCoercing().serialize(TEXT)
    );
  }

  @Test
  void testParseValue() {
    var cost = GraphQLScalars.COST_SCALAR.getCoercing().parseValue(THIRTY);
    assertEquals(COST_THIRTY, cost);
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.COST_SCALAR.getCoercing().parseValue(TEXT)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.COST_SCALAR.getCoercing().parseValue(NEGATIVE_THIRTY)
    );
    assertThrows(CoercingParseValueException.class, () ->
      GraphQLScalars.COST_SCALAR.getCoercing().parseValue(TOO_HIGH)
    );
  }

  @Test
  void testParseLiteral() {
    var cost = GraphQLScalars.COST_SCALAR.getCoercing()
      .parseLiteral(new IntValue(BigInteger.valueOf(THIRTY)));
    assertEquals(COST_THIRTY, cost);
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.COST_SCALAR.getCoercing().parseLiteral(new StringValue(TEXT))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.COST_SCALAR.getCoercing()
        .parseLiteral(new IntValue(BigInteger.valueOf(NEGATIVE_THIRTY)))
    );
    assertThrows(CoercingParseLiteralException.class, () ->
      GraphQLScalars.COST_SCALAR.getCoercing()
        .parseLiteral(new IntValue(BigInteger.valueOf(TOO_HIGH)))
    );
  }
}
