package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getParkingFilters;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getParkingPreferred;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.parseNotFilters;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.parseSelectFilters;

import graphql.schema.DataFetchingEnvironment;
import java.util.Set;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;

public class BicyclePreferencesMapper {

  static void setBicyclePreferences(
    BikePreferences.Builder preferences,
    GraphQLTypes.GraphQLBicyclePreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args == null) {
      return;
    }

    var speed = args.getGraphQLSpeed();
    if (speed != null) {
      preferences.withSpeed(speed);
    }
    var reluctance = args.getGraphQLReluctance();
    if (reluctance != null) {
      preferences.withReluctance(reluctance);
    }
    var boardCost = args.getGraphQLBoardCost();
    if (boardCost != null) {
      preferences.withBoardCost(boardCost.toSeconds());
    }
    preferences.withWalking(walk -> setBicycleWalkPreferences(walk, args.getGraphQLWalk()));
    preferences.withParking(parking ->
      setBicycleParkingPreferences(parking, args.getGraphQLParking(), environment)
    );
    preferences.withRental(rental -> setBicycleRentalPreferences(rental, args.getGraphQLRental()));
    setBicycleOptimization(preferences, args.getGraphQLOptimization());
  }

  private static void setBicycleWalkPreferences(
    VehicleWalkingPreferences.Builder preferences,
    GraphQLTypes.GraphQLBicycleWalkPreferencesInput args
  ) {
    if (args == null) {
      return;
    }

    var speed = args.getGraphQLSpeed();
    if (speed != null) {
      preferences.withSpeed(speed);
    }
    var mountTime = args.getGraphQLMountDismountTime();
    if (mountTime != null) {
      preferences.withMountDismountTime(
        DurationUtils.requireNonNegativeShort(mountTime, "bicycle mount dismount time")
      );
    }
    var cost = args.getGraphQLCost();
    if (cost != null) {
      var reluctance = cost.getGraphQLReluctance();
      if (reluctance != null) {
        preferences.withReluctance(reluctance);
      }
      var mountCost = cost.getGraphQLMountDismountCost();
      if (mountCost != null) {
        preferences.withMountDismountCost(mountCost.toSeconds());
      }
    }
  }

  private static void setBicycleParkingPreferences(
    VehicleParkingPreferences.Builder preferences,
    GraphQLTypes.GraphQLBicycleParkingPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    if (args == null) {
      return;
    }

    var unpreferredCost = args.getGraphQLUnpreferredCost();
    if (unpreferredCost != null) {
      preferences.withUnpreferredVehicleParkingTagCost(unpreferredCost.toSeconds());
    }
    var filters = getParkingFilters(environment, "bicycle");
    preferences.withRequiredVehicleParkingTags(parseSelectFilters(filters));
    preferences.withBannedVehicleParkingTags(parseNotFilters(filters));
    var preferred = getParkingPreferred(environment, "bicycle");
    preferences.withPreferredVehicleParkingTags(parseSelectFilters(preferred));
    preferences.withNotPreferredVehicleParkingTags(parseNotFilters(preferred));
  }

  private static void setBicycleRentalPreferences(
    VehicleRentalPreferences.Builder preferences,
    GraphQLTypes.GraphQLBicycleRentalPreferencesInput args
  ) {
    if (args == null) {
      return;
    }

    var allowedNetworks = args.getGraphQLAllowedNetworks();
    if (allowedNetworks != null) {
      if (allowedNetworks.isEmpty()) {
        throw new IllegalArgumentException("Allowed bicycle rental networks must not be empty.");
      }
      preferences.withAllowedNetworks(Set.copyOf(allowedNetworks));
    }
    var bannedNetworks = args.getGraphQLBannedNetworks();
    if (bannedNetworks != null) {
      preferences.withBannedNetworks(Set.copyOf(bannedNetworks));
    }
    var destinationPolicy = args.getGraphQLDestinationBicyclePolicy();
    if (destinationPolicy != null) {
      var allowed = destinationPolicy.getGraphQLAllowKeeping();
      if (allowed != null) {
        preferences.withAllowArrivingInRentedVehicleAtDestination(allowed);
      }
      var cost = destinationPolicy.getGraphQLKeepingCost();
      if (cost != null) {
        preferences.withArrivingInRentalVehicleAtDestinationCost(cost.toSeconds());
      }
    }
  }

  private static void setBicycleOptimization(
    BikePreferences.Builder preferences,
    GraphQLTypes.GraphQLCyclingOptimizationInput args
  ) {
    if (args == null) {
      return;
    }

    var type = args.getGraphQLType();
    var mappedType = type != null ? VehicleOptimizationTypeMapper.map(type) : null;
    if (mappedType != null) {
      preferences.withOptimizeType(mappedType);
    }
    var triangleArgs = args.getGraphQLTriangle();
    if (isBicycleTriangleSet(triangleArgs)) {
      preferences.withForcedOptimizeTriangle(triangle -> {
        triangle
          .withSlope(triangleArgs.getGraphQLFlatness())
          .withSafety(triangleArgs.getGraphQLSafety())
          .withTime(triangleArgs.getGraphQLTime());
      });
    }
  }

  private static boolean isBicycleTriangleSet(
    GraphQLTypes.GraphQLTriangleCyclingFactorsInput args
  ) {
    return (
      args != null &&
      args.getGraphQLFlatness() != null &&
      args.getGraphQLSafety() != null &&
      args.getGraphQLTime() != null
    );
  }
}
