package org.opentripplanner.ext.transmodelapi.model.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import java.util.Locale;
import org.opentripplanner.api.mapping.RelaxMapper;
import org.opentripplanner.routing.api.request.preference.Relax;

public class RelaxScalarFactory {

  private static final String DOCUMENTATION =
    """
    A linear function f(x) to calculate a relaxed value based on a parameter `x`:
    ```
    f(x) = A * x + C
    ```
    Pass in a ratio A and a slack C for use in the computation. The `x` and `+` sign
    are required, but not the `*`.
    
    Example: `1.05 * x + 1800` 
    """;

  private RelaxScalarFactory() {}

  public static GraphQLScalarType createRelaxFunctionScalar(
    final double maxRatio,
    final int maxSlack
  ) {
    return GraphQLScalarType
      .newScalar()
      .name("RelaxFunction")
      .description(DOCUMENTATION)
      .coercing(
        new Coercing<Relax, String>() {
          @Override
          public String serialize(Object dataFetcherResult) {
            var r = (Relax) (dataFetcherResult);
            return String.format(Locale.ROOT, "%.2f * x + %d", r.ratio(), r.slack());
          }

          @Override
          public Relax parseValue(Object input) throws CoercingParseValueException {
            try {
              Relax relax = RelaxMapper.mapRelax((String) input);
              if (relax.ratio() < 1.0 || relax.ratio() > maxRatio) {
                throw new CoercingParseValueException(
                  "Ratio %f is not in range: [1.0 .. %.1f]".formatted(relax.ratio(), maxRatio)
                );
              }
              if (relax.slack() < 0.0 || relax.slack() > maxSlack) {
                throw new CoercingParseValueException(
                  "Ratio %f is not in range: [0 .. %d]".formatted(relax.ratio(), maxSlack)
                );
              }
              return relax;
            } catch (IllegalArgumentException e) {
              throw new CoercingParseValueException(e.getMessage(), e);
            }
          }

          @Override
          public Relax parseLiteral(Object input) throws CoercingParseLiteralException {
            if (input instanceof StringValue) {
              return parseValue(((StringValue) input).getValue());
            }
            return null;
          }
        }
      )
      .build();
  }
}
