package org.opentripplanner.ext.transmodelapi.model.scalars;

import graphql.Scalars;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeScalarFactory {

    private static final String EXAMPLE_TIME = "18:25:43";
    private static final String TIME_PATTERN = "HH:mm:ss";
    private static final String DATE_SCALAR_DESCRIPTION = "Time using the format: " + TIME_PATTERN + ". Example: " + EXAMPLE_TIME;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final long SECONDS_PER_DAY = Duration.ofDays(1).getSeconds();

    private TimeScalarFactory() {
    }


    public static GraphQLObjectType createSecondsSinceMidnightAsTimeObject() {
        GraphQLScalarType secondsSinceMidnightAsTimeStringScalar = TimeScalarFactory.createSecondsSinceMidnightAsTimeStringScalar();
        return GraphQLObjectType.newObject()
                       .name("TimeAndDayOffset")
                       .field(GraphQLFieldDefinition.newFieldDefinition()
                                      .name("time")
                                      .description("Local time")
                                      .type(secondsSinceMidnightAsTimeStringScalar)
                                      .dataFetcher(environment -> (environment.getSource()))
                                      .build())
                       .field(GraphQLFieldDefinition.newFieldDefinition()
                                      .name("dayOffset")
                                      .description("Number of days offset from base line time")
                                      .type(Scalars.GraphQLInt)
                                      .dataFetcher(environment -> ((Integer) environment.getSource()) / SECONDS_PER_DAY)
                                      .build())
                       .build();


    }

    public static GraphQLScalarType createSecondsSinceMidnightAsTimeStringScalar() {
        return new GraphQLScalarType("Time", DATE_SCALAR_DESCRIPTION, new Coercing() {
            @Override
            public String serialize(Object input) {
                if (input instanceof Integer) {
                    return (LocalTime.ofSecondOfDay(((Integer) input % SECONDS_PER_DAY))).format(FORMATTER);
                }
                return null;
            }

            @Override
            public Integer parseValue(Object input) {
                try {
                    return LocalTime.from(FORMATTER.parse((CharSequence) input)).toSecondOfDay();
                } catch (DateTimeParseException dtpe) {
                    throw new CoercingParseValueException("Expected type 'Time' but was '" + input + "'.");
                }
            }

            @Override
            public Integer parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    return parseValue(((StringValue) input).getValue());
                }
                return null;
            }
        });
    }
}
