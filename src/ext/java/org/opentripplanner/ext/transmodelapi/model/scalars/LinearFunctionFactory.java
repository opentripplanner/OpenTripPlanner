package org.opentripplanner.ext.transmodelapi.model.scalars;

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
import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.request.framework.LinearFunctionSerialization;

public class LinearFunctionFactory {

  private static final String DOCUMENTATION =
    """
    A linear function`f(t)` to calculate a value based on a variable(t). The variable can be the
    duration/time or cost for a leg or section of a path/itinerary. The function `f(t) = a + bt`
    has a constant(a) and a coefficient(b) that will be used in OTP to compute `f(t)`.
    
    Format: `a + b t`. Example: `1800 + 2.0 t`. The constant `a` accept both whole numbers and
    duration input format like: `60` = `60s` = `1m` and `3791` = `1h3m11s`. `b` must be a positive
    decimal number between `0.0` and `100.0`.
    """;

  private LinearFunctionFactory() {}

  public static GraphQLScalarType createLinearFunctionScalar() {
    return GraphQLScalarType
      .newScalar()
      .name("LinearFunction")
      .description(DOCUMENTATION)
      .coercing(
        new Coercing<LinearFunction, String>() {
          @Override
          public String serialize(
            @Nonnull Object dataFetcherResult,
            @Nonnull GraphQLContext graphQLContext,
            @Nonnull Locale locale
          ) {
            var value = (LinearFunction) dataFetcherResult;
            return LinearFunctionSerialization.serialize(value.constant(), value.coefficient());
          }

          @Override
          public LinearFunction parseValue(
            @Nonnull Object input,
            @Nonnull GraphQLContext graphQLContext,
            @Nonnull Locale locale
          ) throws CoercingParseValueException {
            try {
              String text = (String) input;
              return LinearFunctionSerialization.parse(text, LinearFunction::new).orElseThrow();
            } catch (IllegalArgumentException | NoSuchElementException e) {
              throw new CoercingParseValueException(e.getMessage(), e);
            }
          }

          @Override
          public LinearFunction parseLiteral(
            @Nonnull Value<?> input,
            @Nonnull CoercedVariables variables,
            @Nonnull GraphQLContext graphQLContext,
            @Nonnull Locale locale
          ) throws CoercingParseLiteralException {
            if (input instanceof StringValue stringValue) {
              return parseValue(stringValue.getValue(), graphQLContext, locale);
            }
            return null;
          }
        }
      )
      .build();
  }
}
