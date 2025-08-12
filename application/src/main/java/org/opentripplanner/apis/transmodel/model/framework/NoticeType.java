package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.basic.Notice;

public class NoticeType {

  private final FeedScopedIdMapper idResolver;

  public NoticeType(FeedScopedIdMapper idResolver) {
    this.idResolver = idResolver;
  }

  public GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("Notice")
      .field(GqlUtil.newTransitIdField(idResolver))
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("text")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Notice) environment.getSource()).text())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("publicCode")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Notice) environment.getSource()).publicCode())
          .build()
      )
      .build();
  }
}
