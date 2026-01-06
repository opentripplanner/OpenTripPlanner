package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.opentripplanner.apis.transmodel.mapping.preferences.RentalPreferencesMapper.mapRentalPreferences;

import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

public class ScooterPreferencesMapper {

  public static void mapScooterPreferences(
    ScooterPreferences.Builder scooter,
    DataFetcherDecorator callWith
  ) {
    // First, apply deprecated fields (only present if user explicitly provided them)
    callWith.argument("bikeSpeed", scooter::withSpeed);
    callWith.argument("bicycleOptimisationMethod", scooter::withOptimizeType);

    // Then, apply values from scooterPreferences wrapper (takes precedence over deprecated)
    callWith.argument("scooterPreferences.speed", scooter::withSpeed);
    callWith.argument("scooterPreferences.reluctance", scooter::withReluctance);
    callWith.argument("scooterPreferences.optimisationMethod", scooter::withOptimizeType);

    // Handle triangle factors - wrapper takes precedence over deprecated
    if (scooter.optimizeType() == VehicleRoutingOptimizeType.TRIANGLE) {
      scooter.withOptimizeTriangle(triangle -> {
        // First apply deprecated top-level if provided
        callWith.argument("triangleFactors.time", triangle::withTime);
        callWith.argument("triangleFactors.slope", triangle::withSlope);
        callWith.argument("triangleFactors.safety", triangle::withSafety);
        // Then apply from scooterPreferences (takes precedence)
        callWith.argument("scooterPreferences.triangleFactors.time", triangle::withTime);
        callWith.argument("scooterPreferences.triangleFactors.slope", triangle::withSlope);
        callWith.argument("scooterPreferences.triangleFactors.safety", triangle::withSafety);
      });
    }

    scooter.withRental(rental -> mapRentalPreferences(rental, callWith));
  }
}
