package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import java.util.List;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;

public class BikeRentalStationType {

  public static final String NAME = "BikeRentalStation";

  public static GraphQLObjectType create(GraphQLInterfaceType placeInterface) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .withInterface(placeInterface)
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).stationId())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .dataFetcher(environment ->
            ((VehicleRentalStation) environment.getSource()).name().toString()
          )
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("description")
      //                        .type(Scalars.GraphQLString)
      //                        .dataFetcher(environment -> ((VehicleRentalPlace) environment.getSource()).description)
      //                        .build())
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikesAvailable")
          .type(Scalars.GraphQLInt)
          .dataFetcher(environment ->
            ((VehicleRentalStation) environment.getSource()).vehiclesAvailable()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("spacesAvailable")
          .type(Scalars.GraphQLInt)
          .dataFetcher(environment ->
            ((VehicleRentalStation) environment.getSource()).spacesAvailable()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("realtimeOccupancyAvailable")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(environment ->
            ((VehicleRentalStation) environment.getSource()).isRealTimeData()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("allowDropoff")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(environment ->
            ((VehicleRentalStation) environment.getSource()).isAllowDropoff()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("networks")
          .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
          .dataFetcher(environment ->
            List.of(((VehicleRentalStation) environment.getSource()).network())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("longitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).longitude())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).latitude())
          .build()
      )
      .build();
  }
}
