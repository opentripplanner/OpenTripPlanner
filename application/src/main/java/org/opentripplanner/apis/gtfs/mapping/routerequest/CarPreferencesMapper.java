package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getParkingFilters;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getParkingPreferred;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.parseNotFilters;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.parseSelectFilters;

import graphql.schema.DataFetchingEnvironment;
import java.util.Set;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;

public class CarPreferencesMapper {

  static void setCarPreferences(
    CarPreferences.Builder preferences,
    GraphQLTypes.GraphQLCarPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args == null) {
      return;
    }

    var reluctance = args.getGraphQLReluctance();
    if (reluctance != null) {
      preferences.withReluctance(reluctance);
    }
    var boardCost = args.getGraphQLBoardCost();
    if (boardCost != null) {
      preferences.withBoardCost(boardCost.toSeconds());
    }
    preferences.withParking(parking ->
      setCarParkingPreferences(parking, args.getGraphQLParking(), environment)
    );
    preferences.withRental(rental -> setCarRentalPreferences(rental, args.getGraphQLRental()));
  }

  private static void setCarParkingPreferences(
    VehicleParkingPreferences.Builder preferences,
    GraphQLTypes.GraphQLCarParkingPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args == null) {
      return;
    }

    var unpreferredCost = args.getGraphQLUnpreferredCost();
    if (unpreferredCost != null) {
      preferences.withUnpreferredVehicleParkingTagCost(unpreferredCost.toSeconds());
    }
    var filters = getParkingFilters(environment, "car");
    preferences.withRequiredVehicleParkingTags(parseSelectFilters(filters));
    preferences.withBannedVehicleParkingTags(parseNotFilters(filters));
    var preferred = getParkingPreferred(environment, "car");
    preferences.withPreferredVehicleParkingTags(parseSelectFilters(preferred));
    preferences.withNotPreferredVehicleParkingTags(parseNotFilters(preferred));
  }

  private static void setCarRentalPreferences(
    VehicleRentalPreferences.Builder preferences,
    GraphQLTypes.GraphQLCarRentalPreferencesInput args
  ) {
    if (args == null) {
      return;
    }

    var allowedNetworks = args.getGraphQLAllowedNetworks();
    if (allowedNetworks != null) {
      if (allowedNetworks.isEmpty()) {
        throw new IllegalArgumentException("Allowed car rental networks must not be empty.");
      }
      preferences.withAllowedNetworks(Set.copyOf(allowedNetworks));
    }
    var bannedNetworks = args.getGraphQLBannedNetworks();
    if (bannedNetworks != null) {
      preferences.withBannedNetworks(Set.copyOf(bannedNetworks));
    }
  }
}
