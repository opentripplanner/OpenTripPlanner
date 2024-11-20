package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;

public class RentalPlaceTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment env) {
    Object o = env.getObject();
    GraphQLSchema schema = env.getSchema();

    if (o instanceof VehicleRentalStation) {
      return schema.getObjectType("VehicleRentalStation");
    }

    if (o instanceof VehicleRentalVehicle) {
      return schema.getObjectType("RentalVehicle");
    }

    return null;
  }
}
