package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

import java.util.List;

public class BikeRentalStationType {

  public static final String NAME = "BikeRentalStation";

  public static GraphQLObjectType create(GraphQLInterfaceType placeInterface) {
  return GraphQLObjectType.newObject()
          .name(NAME)
          .withInterface(placeInterface)
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("id")
                  .type(new GraphQLNonNull(Scalars.GraphQLID))
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).getStationId())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("name")
                  .type(new GraphQLNonNull(Scalars.GraphQLString))
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).getName().toString())
                  .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("description")
//                        .type(Scalars.GraphQLString)
//                        .dataFetcher(environment -> ((VehicleRentalPlace) environment.getSource()).description)
//                        .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("bikesAvailable")
                  .type(Scalars.GraphQLInt)
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).getVehiclesAvailable())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("spacesAvailable")
                  .type(Scalars.GraphQLInt)
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).getSpacesAvailable())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("realtimeOccupancyAvailable")
                  .type(Scalars.GraphQLBoolean)
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).isRealTimeData())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("allowDropoff")
                  .type(Scalars.GraphQLBoolean)
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).isAllowDropoff())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("networks")
                  .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                  .dataFetcher(environment -> List.of(((VehicleRentalStation) environment.getSource()).getNetwork()))
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("longitude")
                  .type(Scalars.GraphQLFloat)
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).getLongitude())
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("latitude")
                  .type(Scalars.GraphQLFloat)
                  .dataFetcher(environment -> ((VehicleRentalStation) environment.getSource()).getLatitude())
                  .build())
          .build();
}
}
