package org.opentripplanner.apis.transmodel.model.network;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import java.util.Optional;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.GroupOfRoutes;

public class GroupOfLinesType {

  private static final String NAME = "GroupOfLines";

  private final FeedScopedIdMapper idMapper;

  public GroupOfLinesType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "Additional (optional) grouping of lines for particular purposes such as e.g. fare harmonisation or public presentation."
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(env ->
            Optional.ofNullable((AbstractTransitEntity<?, ?>) env.getSource())
              .map(AbstractTransitEntity::getId)
              .map(idMapper::mapToApi)
              .orElse(null)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("privateCode")
          .description("For internal use by operator/authority.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getPrivateCode())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("shortName")
          .description("Short name for group of lines.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getShortName())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .description("Full name for group of lines.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getName())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("description")
          .description("Description of group of lines")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((GroupOfRoutes) env.getSource()).getDescription())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("lines")
          .description("All lines part of this group of lines")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(LineType.REF))))
          .dataFetcher(env ->
            GqlUtil.getTransitService(env).findRoutes((GroupOfRoutes) env.getSource())
          )
          .build()
      )
      .build();
  }
}
