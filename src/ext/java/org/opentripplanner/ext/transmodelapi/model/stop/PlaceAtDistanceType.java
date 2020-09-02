package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;

public class PlaceAtDistanceType {

  public static final String NAME = "PlaceAtDistance";

  public static GraphQLObjectType create(Relay relay, GraphQLInterfaceType placeInterface) {
  return GraphQLObjectType.newObject()
          .name(NAME)
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("id")
                  .type(new GraphQLNonNull(Scalars.GraphQLID))
                  .deprecate("Id is not referable or meaningful and will be removed")
                  .dataFetcher(environment -> relay.toGlobalId(NAME, "N/A"))
                  .build()
          )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("place")
                        .type(placeInterface)
                        .dataFetcher(environment -> ((PlaceAtDistance) environment.getSource()).place)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((PlaceAtDistance) environment.getSource()).distance)
                        .build())
          .build();
}
}
