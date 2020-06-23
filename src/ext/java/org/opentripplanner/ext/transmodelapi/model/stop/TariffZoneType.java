package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

public class TariffZoneType {

  public static GraphQLObjectType createTZ() {
  return GraphQLObjectType
      .newObject()
      .name("TariffZone")
      .field(GraphQLFieldDefinition
          .newFieldDefinition()
          .name("id")
          .type(Scalars.GraphQLID)
          .dataFetcher(e -> "NOT IMPLEMENTED")
          .build())
      .field(GraphQLFieldDefinition
          .newFieldDefinition()
          .name("name")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> "NOT IMPLEMENTED")
          .build())
      .build();
}
}
