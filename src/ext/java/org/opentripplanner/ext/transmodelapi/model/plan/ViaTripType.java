package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;

public class ViaTripType {

  public static GraphQLOutputType create(
    GraphQLObjectType tripPatternType,
    GraphQLObjectType routingErrorType
  ) {
    return GraphQLObjectType
      .newObject()
      .name("ViaTrip")
      .description("Description of a travel between three or more places.")
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("tripPatterns")
          .description("A list of lists of the trip patterns for each segment of the journey")
          .type(
            new GraphQLNonNull(
              new GraphQLList(
                new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(tripPatternType)))
              )
            )
          )
          .dataFetcher(env -> ((ViaPlanResponse) env.getSource()).viaJourneys())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("tripPatternCombinations")
          .description(
            "A list of lists of which indices of the next segment the trip pattern can be combined with"
          )
          .type(
            new GraphQLNonNull(
              new GraphQLList(
                new GraphQLNonNull(
                  new GraphQLList(
                    new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLInt)))
                  )
                )
              )
            )
          )
          .dataFetcher(env -> ((ViaPlanResponse) env.getSource()).viaJourneyConnections())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("routingErrors")
          .description("A list of routing errors, and fields which caused them")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(routingErrorType))))
          .dataFetcher(env -> ((ViaPlanResponse) env.getSource()).routingErrors())
          .build()
      )
      .build();
  }
}
