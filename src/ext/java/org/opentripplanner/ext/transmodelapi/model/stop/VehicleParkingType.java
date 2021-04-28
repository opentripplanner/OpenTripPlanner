package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class VehicleParkingType {

  public static final String NAME = "VehicleParking";

  public static GraphQLObjectType createB(GraphQLInterfaceType placeInterface) {
    return GraphQLObjectType.newObject()
            .name(NAME)
            .withInterface(placeInterface)
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getId().toString())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("name")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getName().toString())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("realtime")
                    .type(Scalars.GraphQLBoolean)
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).hasRealTimeData())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("longitude")
                    .type(Scalars.GraphQLFloat)
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getX())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("latitude")
                    .type(Scalars.GraphQLFloat)
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getY())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("detailsUrl")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getDetailsUrl())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("imageUrl")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getImageUrl())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("note")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getNote())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("state")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getState())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("bicycleSpacesAvailable")
                    .type(Scalars.GraphQLInt)
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getAvailability().getBicycleSpaces())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("carSpacesAvailable")
                    .type(Scalars.GraphQLInt)
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getAvailability().getCarSpaces())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("wheelchairAccessibleCarSpacesAvailable")
                    .type(Scalars.GraphQLInt)
                    .dataFetcher(environment -> ((VehicleParking) environment.getSource()).getAvailability().getWheelchairAccessibleCarSpaces())
                    .build())
            .build();
  }
}
