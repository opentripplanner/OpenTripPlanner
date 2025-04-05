package org.opentripplanner.apis.transmodel.model.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import org.opentripplanner.utils.time.OffsetDateTimeParser;

public final class DateTimeScalarFactory {

  private static final String DOCUMENTATION =
    """
    DateTime format accepting ISO 8601 dates with time zone offset.

    Format:  `YYYY-MM-DD'T'hh:mm[:ss](Z|±01:00)`

    Example: `2017-04-23T18:25:43+02:00` or `2017-04-23T16:25:43Z`""";

  private static final DateTimeFormatter PARSER = OffsetDateTimeParser.LENIENT_PARSER;

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private DateTimeScalarFactory() {}

  public static GraphQLScalarType createMillisecondsSinceEpochAsDateTimeStringScalar(
    ZoneId timeZone
  ) {
    return GraphQLScalarType.newScalar()
      .name("DateTime")
      .description(DOCUMENTATION)
      .coercing(
        new Coercing<>() {
          @Override
          public String serialize(Object input) {
            if (input instanceof Long inputAsLong) {
              return Instant.ofEpochMilli(inputAsLong).atZone(timeZone).format(FORMATTER);
            }
            return null;
          }

          @Override
          public Long parseValue(Object input) {
            Instant instant = null;
            if (input instanceof CharSequence inputAsCharSequence) {
              try {
                TemporalAccessor temporalAccessor = PARSER.parseBest(
                  inputAsCharSequence,
                  OffsetDateTime::from,
                  ZonedDateTime::from,
                  LocalDateTime::from
                );

                if (temporalAccessor instanceof LocalDateTime localDateTime) {
                  instant = localDateTime.atZone(timeZone).toInstant();
                } else {
                  instant = Instant.from(temporalAccessor);
                }
              } catch (DateTimeParseException dtpe) {
                // ignored
              }
            }

            if (instant == null) {
              throw new CoercingParseValueException(
                "Expected type 'DateTime' but was '" + input + "'."
              );
            }

            return instant.toEpochMilli();
          }

          @Override
          public Long parseLiteral(Object input) {
            if (input instanceof StringValue inputAsStringValue) {
              return parseValue(inputAsStringValue.getValue());
            }
            return null;
          }
        }
      )
      .build();
  }
}
