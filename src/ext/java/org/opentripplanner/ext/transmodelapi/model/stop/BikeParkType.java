package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.bike_park.BikePark;

public class BikeParkType {

  public static final String NAME = "BikePark";

  public static GraphQLObjectType createB(GraphQLInterfaceType placeInterface) {
    return GraphQLObjectType.newObject()
            .name(NAME)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                    .dataFetcher(environment -> ((BikePark) environment.getSource()).id)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("name")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .dataFetcher(environment -> ((BikePark) environment.getSource()).name)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("spacesAvailable")
                    .type(Scalars.GraphQLInt)
                    .dataFetcher(environment -> ((BikePark) environment.getSource()).spacesAvailable)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("realtime")
                    .type(Scalars.GraphQLBoolean)
                    .dataFetcher(environment -> ((BikePark) environment.getSource()).realTimeData)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("longitude")
                    .type(Scalars.GraphQLFloat)
                    .dataFetcher(environment -> ((BikePark) environment.getSource()).x)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("latitude")
                    .type(Scalars.GraphQLFloat)
                    .dataFetcher(environment -> ((BikePark) environment.getSource()).y)
                    .build())
            .build();
  }
}
