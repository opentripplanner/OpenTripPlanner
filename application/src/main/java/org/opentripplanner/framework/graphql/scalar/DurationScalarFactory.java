package org.opentripplanner.framework.graphql.scalar;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import org.opentripplanner.utils.time.DurationUtils;

public class DurationScalarFactory {

  public static final String DOCUMENTATION =
    "Duration in a lenient ISO-8601 duration format. Example P2DT2H12M40S, 2d2h12m40s or 1h";

  private DurationScalarFactory() {}

  public static GraphQLScalarType createDurationScalar() {
    return GraphQLScalarType.newScalar()
      .name("Duration")
      .description(DOCUMENTATION)
      .coercing(new DurationCoercing())
      .build();
  }

  private static class DurationCoercing implements Coercing<Duration, String> {

    @Override
    public String serialize(Object input) throws CoercingSerializeException {
      if (input instanceof Duration duration) {
        return DurationUtils.formatDurationWithLeadingMinus(duration);
      }

      throw new CoercingSerializeException(input + " cannot be cast to 'Duration'");
    }

    @Override
    public Duration parseValue(Object input) throws CoercingParseValueException {
      try {
        return DurationUtils.duration(input.toString());
      } catch (DateTimeParseException dtpe) {
        throw new CoercingParseValueException("Expected type 'Duration' but was '" + input + "'.");
      }
    }

    @Override
    public Duration parseLiteral(Object input) throws CoercingParseLiteralException {
      if (input instanceof StringValue) {
        return parseValue(((StringValue) input).getValue());
      }
      throw new CoercingParseValueException("Expected type 'Duration' but was '" + input + "'.");
    }
  }
}
