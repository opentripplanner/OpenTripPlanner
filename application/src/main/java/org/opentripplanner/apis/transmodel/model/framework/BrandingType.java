package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.transit.model.organization.Branding;

public class BrandingType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("Branding")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(Scalars.GraphQLID)
          .dataFetcher(env -> TransitIdMapper.mapEntityIDToApi(env.getSource()))
          .build()
      )
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
