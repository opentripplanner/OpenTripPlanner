package org.opentripplanner.apis.transmodel.model.network;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.transit.model.network.Route;

public class PresentationType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("Presentation")
      .description("Types describing common presentation properties")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("colour")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Route) environment.getSource()).getColor())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("textColour")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Route) environment.getSource()).getTextColor())
          .build()
      )
      .build();
  }
}
