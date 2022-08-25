package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLNodeTypeResolver.queryContainsFragment;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.transit.model.site.RegularStop;

public class LegacyGraphQLPlaceInterfaceTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof VehicleParking) {
      var vehicleParking = (VehicleParking) o;
      if (queryContainsFragment("BikePark", environment) && vehicleParking.hasBicyclePlaces()) {
        return schema.getObjectType("BikePark");
      }
      if (queryContainsFragment("CarPark", environment) && vehicleParking.hasAnyCarPlaces()) {
        return schema.getObjectType("CarPark");
      }
      return schema.getObjectType("VehicleParking");
    }
    if (o instanceof VehicleRentalStation) {
      if (queryContainsFragment("BikeRentalStation", environment)) {
        return schema.getObjectType("BikeRentalStation");
      }
      return schema.getObjectType("VehicleRentalStation");
    }
    if (o instanceof VehicleRentalVehicle) {
      return schema.getObjectType("RentalVehicle");
    }
    if (o instanceof PatternAtStop) {
      return schema.getObjectType("DepartureRow");
    }
    if (o instanceof RegularStop) {
      return schema.getObjectType("Stop");
    }

    return null;
  }
}
