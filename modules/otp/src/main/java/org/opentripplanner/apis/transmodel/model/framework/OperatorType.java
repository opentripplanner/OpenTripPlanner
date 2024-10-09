package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.apis.transmodel.support.GqlUtil;

public class OperatorType {

  public static GraphQLObjectType create(
    GraphQLOutputType lineType,
    GraphQLOutputType serviceJourneyType,
    GqlUtil gqlUtil
  ) {
    return GraphQLObjectType
      .newObject()
      .name("Operator")
      .description("Organisation providing public transport services.")
      .field(GqlUtil.newTransitIdField())
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("name")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition().name("url").type(Scalars.GraphQLString).build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("phone")
          .type(Scalars.GraphQLString)
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("branding")
      //                        .description("Branding for operator.")
      //                        .type(brandingType)
      //                        .build())
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("lines")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(lineType)))
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getAllRoutes()
              .stream()
              .filter(route -> Objects.equals(route.getOperator(), environment.getSource()))
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceJourney")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getAllTrips()
              .stream()
              .filter(trip -> Objects.equals(trip.getOperator(), environment.getSource()))
              .collect(Collectors.toList())
          )
          .build()
      )
      .build();
  }
}
