package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;

public class PlaceAtDistanceType {

  public static final String NAME = "PlaceAtDistance";

  public static GraphQLObjectType create(Relay relay) {
  return GraphQLObjectType.newObject()
          .name(NAME)
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("id")
                  .type(new GraphQLNonNull(Scalars.GraphQLID))
                  .deprecate("Id is not referable or meaningful and will be removed")
                  .dataFetcher(environment -> relay.toGlobalId(NAME, "N/A"))
                  .build()
          )
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("place")
//                        .type(placeInterface)
//                        .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).place)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("distance")
//                        .type(Scalars.GraphQLInt)
//                        .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).distance)
//                        .build())
          .build();
}
}
