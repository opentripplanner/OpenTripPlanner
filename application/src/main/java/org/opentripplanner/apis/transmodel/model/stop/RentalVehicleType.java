package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;

public class RentalVehicleType {

  public static final String NAME = "RentalVehicle";

  public static GraphQLObjectType create(
    GraphQLOutputType vehicleTypeType,
    GraphQLInterfaceType placeInterface
  ) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .withInterface(placeInterface)
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).stationId())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("vehicleType")
          .type(new GraphQLNonNull(vehicleTypeType))
          .dataFetcher(environment ->
            ((VehicleRentalVehicle) environment.getSource()).vehicleType()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("network")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).network())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("longitude")
          .type(new GraphQLNonNull(Scalars.GraphQLFloat))
          .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).longitude())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latitude")
          .type(new GraphQLNonNull(Scalars.GraphQLFloat))
          .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).latitude())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("currentRangeMeters")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment ->
            ((VehicleRentalVehicle) environment.getSource()).getFuel().range()
          )
          .build()
      )
      .build();
  }
}
