package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;

import java.util.stream.Collectors;

public class TripType {
  public static GraphQLObjectType create(
      GraphQLObjectType placeType,
      GraphQLObjectType tripPatternType,
      GraphQLObjectType tripMetadataType,
      GqlUtil gqlUtil

  ) {
    return GraphQLObjectType.newObject()
        .name("Trip")
        .description("Description of a travel between two places.")
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("dateTime")
            .description("The time and date of travel")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.date.getTime())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("metadata")
            .description("The trip request metadata.")
            .type(tripMetadataType)
            .dataFetcher(env -> ((PlanResponse) env.getSource()).metadata)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("fromPlace")
            .description("The origin")
            .type(new GraphQLNonNull(placeType))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.from)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("toPlace")
            .description("The destination")
            .type(new GraphQLNonNull(placeType))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.to)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("tripPatterns")
            .description("A list of possible trip patterns")
            .type(new GraphQLNonNull(new GraphQLList(tripPatternType)))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.itineraries)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("messageEnums")
            .description("A list of possible error messages as enum")
            .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).messages
                .stream().map(Enum::name).collect(Collectors.toList()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("messageStrings")
            .description("A list of possible error messages in cleartext")
            .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
            .dataFetcher(
                env -> ((PlanResponse) env.getSource())
                    .listErrorMessages(env.getArgument("locale"))
            )
            .build())
        // TODO OTP2 - Next version: Wrap errors, include data like witch parameter
        //           - is causing a problem (like from/to not found).
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("debugOutput")
            .description("Information about the timings for the trip generation")
            .type(new GraphQLNonNull(GraphQLObjectType.newObject()
                .name("debugOutput")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("totalTime")
                    .type(Scalars.GraphQLLong)
                    .build())
                .build()))
            .dataFetcher(env -> ((PlanResponse) env.getSource()).debugOutput)
            .build())
        .build();
  }
}
