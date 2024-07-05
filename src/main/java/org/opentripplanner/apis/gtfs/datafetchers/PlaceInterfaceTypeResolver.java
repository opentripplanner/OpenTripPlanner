package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;

public class PlaceInterfaceTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof VehicleParking) {
      var vehicleParking = (VehicleParking) o;
      if (
        NodeTypeResolver.queryContainsFragment("BikePark", environment) &&
        vehicleParking.hasBicyclePlaces()
      ) {
        return schema.getObjectType("BikePark");
      }
      if (
        NodeTypeResolver.queryContainsFragment("CarPark", environment) &&
        vehicleParking.hasAnyCarPlaces()
      ) {
        return schema.getObjectType("CarPark");
      }
      return schema.getObjectType("VehicleParking");
    }
    if (o instanceof VehicleRentalStation) {
      if (NodeTypeResolver.queryContainsFragment("BikeRentalStation", environment)) {
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
    if (o instanceof RegularStop || o instanceof Station) {
      return schema.getObjectType("Stop");
    }

    return null;
  }
}
