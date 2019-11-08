package org.opentripplanner.ext.transmodelapi.model.scalars;

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
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;

public class DateTimeScalarFactory {

    private static final String EXAMPLE_DATE_TIME = "2017-04-23T18:25:43+0100";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXXX";
    private static final String PARSE_DATE_TIME_PATTERN = "[yyyyMMdd][yyyy-MM-dd][yyyy-DDD]['T'[HHmmss][HHmm][HH:mm:ss][HH:mm][.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]][OOOO][O][z][XXXXX][XXXX]['['VV']']";

    private static final String DATE_SCALAR_DESCRIPTION = "DateTime format accepting ISO dates. Return values on format: " + DATE_TIME_PATTERN + ". Example: " + EXAMPLE_DATE_TIME;

    public static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .appendPattern(PARSE_DATE_TIME_PATTERN)
            .toFormatter();

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern(DATE_TIME_PATTERN)
            .toFormatter();


    private DateTimeScalarFactory() {
    }

    public static GraphQLScalarType createMillisecondsSinceEpochAsDateTimeStringScalar(TimeZone timeZone) {

        return new GraphQLScalarType("DateTime", DATE_SCALAR_DESCRIPTION, new Coercing() {
            @Override
            public String serialize(Object input) {
                if (input instanceof Long) {
                    return ((Instant.ofEpochMilli((Long) input))).atZone(timeZone.toZoneId()).format(FORMATTER);
                }
                return null;
            }

            @Override
            public Long parseValue(Object input) {
                Instant instant;
                try {
                    TemporalAccessor temporalAccessor = PARSER.parseBest((CharSequence) input, OffsetDateTime::from, ZonedDateTime::from, LocalDateTime::from);

                    if (temporalAccessor instanceof LocalDateTime) {
                        instant = ((LocalDateTime) temporalAccessor).atZone(ZoneId.systemDefault()).toInstant();
                    } else {
                        instant = Instant.from(temporalAccessor);
                    }
                } catch (DateTimeParseException dtpe) {
                    instant = null;
                }
                if (instant == null) {
                    throw new CoercingParseValueException("Expected type 'DateTime' but was '" + input + "'.");
                }

                return instant.toEpochMilli();
            }

            @Override
            public Long parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    return parseValue(((StringValue) input).getValue());
                }
                return null;
            }
        });
    }

}
