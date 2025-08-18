package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.organization.Branding;

public class BrandingType {

  private final FeedScopedIdMapper idMapper;

  public BrandingType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("Branding")
      .field(GqlUtil.newTransitIdField(idMapper))
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .description("Full name to be used for branding.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((Branding) env.getSource()).getName())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("shortName")
          .description("Short name to be used for branding.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((Branding) env.getSource()).getShortName())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("description")
          .description("Description of branding.")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((Branding) env.getSource()).getDescription())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("url")
          .description("URL to be used for branding")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((Branding) env.getSource()).getUrl())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("image")
          .description("URL to an image be used for branding")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((Branding) env.getSource()).getImage())
          .build()
      )
      .build();
  }
}
