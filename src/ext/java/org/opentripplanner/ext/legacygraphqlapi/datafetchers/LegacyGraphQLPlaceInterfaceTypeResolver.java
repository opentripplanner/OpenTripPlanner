package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.language.InlineFragment;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

public class LegacyGraphQLPlaceInterfaceTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof BikePark) { return schema.getObjectType("BikePark"); }
    if (o instanceof VehicleRentalStation) {
      SelectionSet set = environment.getField().getFields().get(0).getSelectionSet();
      boolean queryHasVehicleRentalStationFragment = set != null && set.getSelections()
              .stream()
              .filter(selection -> selection instanceof InlineFragment)
              .map(InlineFragment.class::cast)
              .anyMatch(fragment -> fragment.getTypeCondition()
                      .getName()
                      .equals("VehicleRentalStation"));
      return queryHasVehicleRentalStationFragment
              ? schema.getObjectType("VehicleRentalStation")
              : schema.getObjectType("BikeRentalStation");
    }
    if (o instanceof VehicleRentalVehicle) {
      return schema.getObjectType("RentalVehicle");
    }
    // if (o instanceof CarPark) { return schema.getObjectType("CarPark"); }
    if (o instanceof PatternAtStop) { return schema.getObjectType("DepartureRow"); }
    if (o instanceof Stop) { return schema.getObjectType("Stop"); }

    return null;
  }
}
