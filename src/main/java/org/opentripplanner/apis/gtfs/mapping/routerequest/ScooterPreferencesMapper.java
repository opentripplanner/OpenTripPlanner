package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.Set;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;

public class ScooterPreferencesMapper {

  static void setScooterPreferences(
    ScooterPreferences.Builder preferences,
    GraphQLTypes.GraphQLScooterPreferencesInput args
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
    preferences.withRental(rental -> setScooterRentalPreferences(rental, args.getGraphQLRental()));
    setScooterOptimization(preferences, args.getGraphQLOptimization());
  }

  private static void setScooterRentalPreferences(
    VehicleRentalPreferences.Builder preferences,
    GraphQLTypes.GraphQLScooterRentalPreferencesInput args
  ) {
    if (args == null) {
      return;
    }

    var allowedNetworks = args.getGraphQLAllowedNetworks();
    if (allowedNetworks != null) {
      if (allowedNetworks.isEmpty()) {
        throw new IllegalArgumentException("Allowed scooter rental networks must not be empty.");
      }
      preferences.withAllowedNetworks(Set.copyOf(allowedNetworks));
    }
    var bannedNetworks = args.getGraphQLBannedNetworks();
    if (bannedNetworks != null) {
      preferences.withBannedNetworks(Set.copyOf(bannedNetworks));
    }
    var destinationPolicy = args.getGraphQLDestinationScooterPolicy();
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

  private static void setScooterOptimization(
    ScooterPreferences.Builder preferences,
    GraphQLTypes.GraphQLScooterOptimizationInput args
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
    if (isScooterTriangleSet(triangleArgs)) {
      preferences.withForcedOptimizeTriangle(triangle -> {
        triangle
          .withSlope(triangleArgs.getGraphQLFlatness())
          .withSafety(triangleArgs.getGraphQLSafety())
          .withTime(triangleArgs.getGraphQLTime());
      });
    }
  }

  private static boolean isScooterTriangleSet(
    GraphQLTypes.GraphQLTriangleScooterFactorsInput args
  ) {
    return (
      args != null &&
      args.getGraphQLFlatness() != null &&
      args.getGraphQLSafety() != null &&
      args.getGraphQLTime() != null
    );
  }
}
