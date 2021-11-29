package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

import java.util.List;

public class RentalVehicleType {

  public static final String NAME = "RentalVehicle";

  public static GraphQLObjectType create(GraphQLOutputType vehicleTypeType, GraphQLInterfaceType placeInterface) {
  return GraphQLObjectType.newObject()
          .name(NAME)
          .withInterface(placeInterface)
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("id")
                  .type(new GraphQLNonNull(Scalars.GraphQLID))
                  .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).getStationId())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("vehicleType")
                  .type(new GraphQLNonNull(vehicleTypeType))
                  .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).vehicleType)
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("network")
                  .type(new GraphQLNonNull(Scalars.GraphQLString))
                  .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).getNetwork())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("longitude")
                  .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                  .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).getLongitude())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("latitude")
                  .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                  .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).getLatitude())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("currentRangeMeters")
                  .type(Scalars.GraphQLFloat)
                  .dataFetcher(environment -> ((VehicleRentalVehicle) environment.getSource()).currentRangeMeters)
                  .build())
          .build();
}
}
