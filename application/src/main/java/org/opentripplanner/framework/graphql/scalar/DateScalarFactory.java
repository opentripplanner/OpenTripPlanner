package org.opentripplanner.framework.graphql.scalar;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.annotation.Nullable;

public class DateScalarFactory {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  public static final String TRANSMODEL_DESCRIPTION =
    "Local date using the ISO 8601 format: `YYYY-MM-DD`. Example: `2020-05-17`.";

  private static final String TRANSMODEL_NAME = "Date";
  private static final String GTFS_NAME = "LocalDate";

  private DateScalarFactory() {}

  public static GraphQLScalarType createTransmodelDateScalar() {
    return createDateScalar(TRANSMODEL_NAME, TRANSMODEL_DESCRIPTION);
  }

  public static GraphQLScalarType createGtfsDateScalar() {
    // description comes from schema.graphqls
    return createDateScalar(GTFS_NAME, null);
  }

  /**
   * @param description Nullable description that allows caller to pass in null which leads to the
   *                    description from schema.graphqls to be used.
   */
  private static GraphQLScalarType createDateScalar(
    String scalarName,
    @Nullable String description
  ) {
    return GraphQLScalarType.newScalar()
      .name(scalarName)
      .description(description)
      .coercing(
        new Coercing<LocalDate, String>() {
          @Override
          public String serialize(Object input) throws CoercingSerializeException {
            if (input instanceof LocalDate) {
              return ((LocalDate) input).toString();
            }

            throw new CoercingSerializeException(
              "Only %s is supported to serialize but found %s".formatted(scalarName, input)
            );
          }

          @Override
          public LocalDate parseValue(Object input) throws CoercingParseValueException {
            try {
              return LocalDate.from(FORMATTER.parse((String) input));
            } catch (DateTimeParseException e) {
              throw new CoercingParseValueException(
                "Expected type '%s' but was '%s'.".formatted(scalarName, input)
              );
            }
          }

          @Override
          public LocalDate parseLiteral(Object input) throws CoercingParseLiteralException {
            if (input instanceof StringValue) {
              return parseValue(((StringValue) input).getValue());
            }

            throw new CoercingParseLiteralException("Expected String type but found " + input);
          }
        }
      )
      .build();
  }
}
