package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLSchema;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.site.RegularStop;

public class PlaceInterfaceType {

  public static GraphQLInterfaceType create() {
    return GraphQLInterfaceType.newInterface()
      .name("PlaceInterface")
      .description("Interface for places, i.e. quays, stop places, parks")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latitude")
          .type(Scalars.GraphQLFloat)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("longitude")
          .type(Scalars.GraphQLFloat)
          .build()
      )
      .typeResolver(typeResolutionEnvironment -> {
        Object o = typeResolutionEnvironment.getObject();
        GraphQLSchema schema = typeResolutionEnvironment.getSchema();

        // Passing in the type itself in the constructor does not work, as the type has not been
        // created yet and you need the actual type and not just a reference to it. That is why
        // we get the type from the schema. This also follows how it is done in the
        // GtfsGraphQLNodeTypeResolver.

        if (o instanceof RegularStop) {
          return schema.getObjectType("Quay");
        }
        if (o instanceof MonoOrMultiModalStation) {
          return schema.getObjectType("StopPlace");
        }
        if (o instanceof VehicleRentalStation) {
          return schema.getObjectType("BikeRentalStation");
        }
        if (o instanceof VehicleRentalVehicle) {
          return schema.getObjectType("RentalVehicle");
        }
        if (o instanceof VehicleParking) {
          return schema.getObjectType(BikeParkType.NAME);
        }
        //if (o instanceof CarPark) {
        //    return (GraphQLObjectType) carParkType;
        //}
        return null;
      })
      .build();
  }
}
