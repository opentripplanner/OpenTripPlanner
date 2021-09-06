package org.opentripplanner.ext.transmodelapi.model.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

public class DateScalarFactory {
    private static final String DOCUMENTATION =
        "Local date using the ISO 8601 format: `YYYY-MM-DD`. Example: `2020-05-17`.";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateScalarFactory() {
    }

    public static GraphQLScalarType createSecondsSinceEpochAsDateStringScalar(TimeZone timeZone) {
        return GraphQLScalarType.newScalar().name("Date").description(DOCUMENTATION).coercing(new Coercing<>() {
            @Override
            public String serialize(Object input) {
                if (input instanceof Long) {
                    // Add 1 hour before converting to date, to account for daylight savings time. This is done because
                    // in for example TripTimeShort, only epoch time 12 hours before noon is stored instead of
                    // the original service date.
                    return ((Instant.ofEpochSecond((Long) input))).atZone(timeZone.toZoneId()).toLocalDateTime().plusHours(1).toLocalDate().format(FORMATTER);
                }
                return null;
            }

            @Override
            public Long parseValue(Object input) {
                try {
                    return LocalDate.from(FORMATTER.parse((CharSequence) input)).atStartOfDay(timeZone.toZoneId()).toEpochSecond();
                } catch (DateTimeParseException dtpe) {
                    throw new CoercingParseValueException("Expected type 'Date' but was '" + input + "'.");
                }
            }

            @Override
            public Long parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    return parseValue(((StringValue) input).getValue());
                }
                return null;
            }
        }).build();
    }
}
