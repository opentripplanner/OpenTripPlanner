package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.language.InlineFragment;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

public class LegacyGraphQLNodeTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof Agency) { return schema.getObjectType("Agency"); }
    if (o instanceof TransitAlert) { return schema.getObjectType("Alert"); }
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
    if (o instanceof VehicleRentalVehicle) { return schema.getObjectType("RentalVehicle"); }
    if (o instanceof VehicleRentalStation) {
      if (queryContainsFragment("BikeRentalStation", environment)) {
        return schema.getObjectType("BikeRentalStation");
      }
      return schema.getObjectType("VehicleRentalStation");
    }
    // if (o instanceof Cluster) { return schema.getObjectType("Cluster"); }
    if (o instanceof PatternAtStop) { return schema.getObjectType("DepartureRow"); }
    if (o instanceof TripPattern) { return schema.getObjectType("Pattern"); }
    if (o instanceof PlaceAtDistance) { return schema.getObjectType("placeAtDistance"); }
    if (o instanceof Route) { return schema.getObjectType("Route"); }
    if (o instanceof Stop) { return schema.getObjectType("Stop"); }
    if (o instanceof Station) { return schema.getObjectType("Stop"); }
    if (o instanceof TripTimeOnDate) { return schema.getObjectType("Stoptime"); }
    if (o instanceof NearbyStop) { return schema.getObjectType("stopAtDistance"); }
    if (o instanceof FareRuleSet) { return schema.getObjectType("TicketType"); }
    if (o instanceof Trip) { return schema.getObjectType("Trip"); }
    return null;
  }

  static boolean queryContainsFragment(String type, TypeResolutionEnvironment environment) {
    SelectionSet set = environment.getField().getFields().get(0).getSelectionSet();
    return set != null && set.getSelections()
            .stream()
            .filter(selection -> selection instanceof InlineFragment)
            .map(InlineFragment.class::cast)
            .anyMatch(fragment -> fragment.getTypeCondition()
                    .getName()
                    .equals(type));
  }
}
