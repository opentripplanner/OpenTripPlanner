package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.util.ArrayList;

public class BikeRentalStationType {

  public static final String NAME = "BikeRentalStation";

  public static GraphQLObjectType create(GraphQLInterfaceType placeInterface) {
  return GraphQLObjectType.newObject()
          .name(NAME)
          .withInterface(placeInterface)
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("id")
                  .type(new GraphQLNonNull(Scalars.GraphQLID))
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).id)
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("name")
                  .type(new GraphQLNonNull(Scalars.GraphQLString))
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).name.toString())
                  .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("description")
//                        .type(Scalars.GraphQLString)
//                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).description)
//                        .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("bikesAvailable")
                  .type(Scalars.GraphQLInt)
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).bikesAvailable)
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("spacesAvailable")
                  .type(Scalars.GraphQLInt)
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).spacesAvailable)
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("realtimeOccupancyAvailable")
                  .type(Scalars.GraphQLBoolean)
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).realTimeData)
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("allowDropoff")
                  .type(Scalars.GraphQLBoolean)
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).allowDropoff)
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("networks")
                  .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                  .dataFetcher(environment -> new ArrayList<>(((BikeRentalStation) environment.getSource()).networks))
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("longitude")
                  .type(Scalars.GraphQLFloat)
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).longitude)
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("latitude")
                  .type(Scalars.GraphQLFloat)
                  .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).latitude)
                  .build())
          .build();
}
}
