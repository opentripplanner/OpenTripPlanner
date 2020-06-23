package org.opentripplanner.ext.transmodelapi.model.network;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

public class DestinationDisplayType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
            .name("DestinationDisplay")
            .description("An advertised destination of a specific journey pattern, usually displayed on a head sign or at other on-board locations.")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("frontText")
                    .description("Name of destination to show on front of vehicle.")
                    .type(Scalars.GraphQLString)
                    .dataFetcher(DataFetchingEnvironment::getSource)
                    .build())
            .build();
  }
}
