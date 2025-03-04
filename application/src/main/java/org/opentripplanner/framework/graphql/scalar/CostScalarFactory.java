package org.opentripplanner.framework.graphql.scalar;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.utils.time.DurationUtils;

public class CostScalarFactory {

  private static final String TYPENAME = "Cost";

  private static final String DOCUMENTATION =
    "A cost value, normally a value of 1 is equivalent to riding transit for 1 second, " +
    "but it might not depending on the use-case. Format: 3665 = DT1h1m5s = 1h1m5s";

  private static final GraphQLScalarType SCALAR_INSTANCE = createCostScalar();

  private CostScalarFactory() {}

  public static GraphQLScalarType costScalar() {
    return SCALAR_INSTANCE;
  }

  private static GraphQLScalarType createCostScalar() {
    return GraphQLScalarType.newScalar()
      .name(TYPENAME)
      .description(DOCUMENTATION)
      .coercing(createCoercing())
      .build();
  }

  private static String serializeCost(Cost cost) {
    return cost.asDuration().toString();
  }

  private static Cost parseCost(String input) throws CoercingParseValueException {
    try {
      return Cost.fromDuration(DurationUtils.parseSecondsOrDuration(input).orElseThrow());
    } catch (IllegalArgumentException | NoSuchElementException e) {
      throw new CoercingParseValueException(e.getMessage(), e);
    }
  }

  private static Coercing<Cost, String> createCoercing() {
    return new Coercing<>() {
      @Override
      public String serialize(Object result, GraphQLContext c, Locale l) {
        return serializeCost((Cost) result);
      }

      @Override
      public Cost parseValue(Object input, GraphQLContext c, Locale l)
        throws CoercingParseValueException {
        return parseCost((String) input);
      }

      @Override
      public Cost parseLiteral(Value<?> input, CoercedVariables v, GraphQLContext c, Locale l)
        throws CoercingParseLiteralException {
        if (input instanceof StringValue stringValue) {
          return parseCost(stringValue.getValue());
        }
        return null;
      }

      @Override
      public Value<?> valueToLiteral(Object input, GraphQLContext c, Locale l) {
        return StringValue.of((String) input);
      }
    };
  }
}
