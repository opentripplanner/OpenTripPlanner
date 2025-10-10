package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import java.util.stream.Collectors;
import org.opentripplanner.apis.support.mapping.PlannerErrorMapper;
import org.opentripplanner.apis.transmodel.model.PlanResponse;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;

public class TripType {

  public static GraphQLObjectType create(
    GraphQLObjectType placeType,
    GraphQLObjectType tripPatternType,
    GraphQLObjectType tripMetadataType,
    GraphQLObjectType routingErrorType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLObjectType.newObject()
      .name("Trip")
      .description("Description of a travel between two places.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("dateTime")
          .description("The time and date of travel")
          .type(dateTimeScalar)
          .dataFetcher(env -> env.<PlanResponse>getSource().date().toEpochMilli())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("metadata")
          .description("The trip request metadata.")
          .type(tripMetadataType)
          .dataFetcher(env -> env.<PlanResponse>getSource().metadata())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("fromPlace")
          .description("The origin")
          .type(new GraphQLNonNull(placeType))
          .dataFetcher(env -> env.<PlanResponse>getSource().from())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("toPlace")
          .description("The destination")
          .type(new GraphQLNonNull(placeType))
          .dataFetcher(env -> env.<PlanResponse>getSource().to())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tripPatterns")
          .description("A list of possible trip patterns")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(tripPatternType))))
          .dataFetcher(env -> env.<PlanResponse>getSource().itineraries())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("messageEnums")
          .description("A list of possible error messages as enum")
          .deprecate("Use routingErrors instead")
          .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
          .dataFetcher(env ->
            env
              .<PlanResponse>getSource()
              .messages()
              .stream()
              .map(routingError -> PlannerErrorMapper.mapMessage(routingError).message)
              .map(Enum::name)
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("messageStrings")
          .deprecate("Use routingErrors instead")
          .description("A list of possible error messages in cleartext")
          .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
          .argument(
            GraphQLArgument.newArgument().name("language").type(Scalars.GraphQLString).build()
          )
          .dataFetcher(env ->
            env
              .<PlanResponse>getSource()
              .messages()
              .stream()
              .map(routingError -> PlannerErrorMapper.mapMessage(routingError).message)
              .map(message -> message.get(GraphQLUtils.getLocale(env)))
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("routingErrors")
          .description("A list of routing errors, and fields which caused them")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(routingErrorType))))
          .dataFetcher(env -> env.<PlanResponse>getSource().messages())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("debugOutput")
          .description("Information about the timings for the trip generation")
          .type(
            new GraphQLNonNull(
              GraphQLObjectType.newObject()
                .name("debugOutput")
                .field(
                  GraphQLFieldDefinition.newFieldDefinition()
                    .name("totalTime")
                    .type(ExtendedScalars.GraphQLLong)
                    .build()
                )
                .build()
            )
          )
          .dataFetcher(env -> env.<PlanResponse>getSource().debugOutput())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("previousPageCursor")
          .description(
            "Use the cursor to get the previous page of results. Use this cursor for " +
            "the pageCursor parameter in the trip query in order to get the previous page.\n" +
            "The previous page is a set of itineraries departing BEFORE the first itinerary" +
            " in this result."
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> {
            final PageCursor pageCursor = env.<PlanResponse>getSource().previousPageCursor();
            return pageCursor != null ? pageCursor.encode() : null;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("nextPageCursor")
          .description(
            "Use the cursor to get the next page of results. Use this cursor for " +
            "the pageCursor parameter in the trip query in order to get the next page.\n" +
            "The next page is a set of itineraries departing AFTER the last " +
            "itinerary in this result."
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> {
            final PageCursor pageCursor = env.<PlanResponse>getSource().nextPageCursor();
            return pageCursor != null ? pageCursor.encode() : null;
          })
          .build()
      )
      .build();
  }
}
