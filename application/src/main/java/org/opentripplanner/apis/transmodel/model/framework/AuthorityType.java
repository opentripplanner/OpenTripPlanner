package org.opentripplanner.apis.transmodel.model.framework;

import static org.opentripplanner.apis.transmodel.support.GqlUtil.getTransitService;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.organization.Agency;

public class AuthorityType {

  public static GraphQLObjectType create(
    GraphQLOutputType lineType,
    GraphQLOutputType ptSituationElementType
  ) {
    return GraphQLObjectType.newObject()
      .name("Authority")
      .description(
        "Authority involved in public transportation. An organisation under which the responsibility of organising the transport service in a certain area is placed."
      )
      .field(GqlUtil.newTransitIdField())
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition().name("url").type(Scalars.GraphQLString).build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timezone")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition().name("lang").type(Scalars.GraphQLString).build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("phone")
          .type(Scalars.GraphQLString)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("fareUrl")
          .type(Scalars.GraphQLString)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("lines")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(lineType)))
          .dataFetcher(environment ->
            getTransitService(environment)
              .listRoutes()
              .stream()
              .filter(route -> Objects.equals(route.getAgency(), environment.getSource()))
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("Get all situations active for the authority.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(environment ->
            getTransitService(environment)
              .getTransitAlertService()
              .getAgencyAlerts(((Agency) environment.getSource()).getId())
          )
          .build()
      )
      .build();
  }
}
