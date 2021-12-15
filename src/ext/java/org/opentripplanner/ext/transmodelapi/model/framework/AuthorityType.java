package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.Agency;

import java.util.Objects;
import java.util.stream.Collectors;

import static org.opentripplanner.ext.transmodelapi.support.GqlUtil.getRoutingService;

public class AuthorityType {

  public static GraphQLObjectType create(
      GraphQLOutputType lineType,
      GraphQLOutputType ptSituationElementType,
      GqlUtil gqlUtil
  ) {
    return GraphQLObjectType.newObject()
        .name("Authority")
        .description("Authority involved in public transportation. An organisation under which the responsibility of organising the transport service in a certain area is placed.")
        .field(GqlUtil.newTransitIdField())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("name")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("url")
                .type(Scalars.GraphQLString)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("timezone")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lang")
                .type(Scalars.GraphQLString)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("phone")
                .type(Scalars.GraphQLString)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("fareUrl")
                .type(Scalars.GraphQLString)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("lines")
                .withDirective(gqlUtil.timingData)
                .type(new GraphQLNonNull(new GraphQLList(lineType)))
                .dataFetcher(environment -> getRoutingService(environment).getAllRoutes()
                        .stream()
                        .filter(route -> Objects.equals(route.getAgency(), environment.getSource()))
                        .collect(Collectors.toList()))
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("situations")
                .withDirective(gqlUtil.timingData)
                .description("Get all situations active for the authority.")
                .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                .dataFetcher(environment -> getRoutingService(environment)
                    .getTransitAlertService()
                    .getAgencyAlerts(((Agency)environment.getSource()).getId()))
                .build())
        .build();
  }
}
