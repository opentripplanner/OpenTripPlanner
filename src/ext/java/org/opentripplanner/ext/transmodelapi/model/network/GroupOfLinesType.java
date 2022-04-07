package org.opentripplanner.ext.transmodelapi.model.network;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.model.GroupOfRoutes;

public class GroupOfLinesType {

  private static final String NAME = "GroupOfLines";

  public static GraphQLObjectType create() {
    return GraphQLObjectType
      .newObject()
      .name(NAME)
      .description(
        "Additional (optional) grouping of lines for particular purposes such as e.g. fare harmonisation or public presentation."
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(env -> TransitIdMapper.mapEntityIDToApi(env.getSource()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("privateCode")
          .description("For internal use by operator/authority.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getPrivateCode())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("shortName")
          .description("Short name for group of lines.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getShortName())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("name")
          .description("Full name for group of lines.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getName())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("description")
          .description("Description of group of lines")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getDescription())
          .build()
      )
      .build();
  }
}
