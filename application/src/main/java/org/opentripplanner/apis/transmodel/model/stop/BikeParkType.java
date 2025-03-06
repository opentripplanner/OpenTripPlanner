package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;

public class BikeParkType {

  public static final String NAME = "BikePark";

  public static GraphQLObjectType createB(GraphQLInterfaceType placeInterface) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .withInterface(placeInterface)
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment ->
            TransitIdMapper.mapIDToApi(((VehicleParking) environment.getSource()).getId())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .dataFetcher(environment ->
            ((VehicleParking) environment.getSource()).getName().toString()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("spacesAvailable")
          .type(Scalars.GraphQLInt)
          .dataFetcher(environment -> {
            var vehicleParking = ((VehicleParking) environment.getSource());
            var availability = vehicleParking.getAvailability();
            if (availability != null) {
              return availability.getBicycleSpaces();
            } else {
              return Integer.MAX_VALUE;
            }
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("realtime")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(environment -> ((VehicleParking) environment.getSource()).hasRealTimeData())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("longitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment ->
            ((VehicleParking) environment.getSource()).getCoordinate().longitude()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment ->
            ((VehicleParking) environment.getSource()).getCoordinate().latitude()
          )
          .build()
      )
      .build();
  }
}
