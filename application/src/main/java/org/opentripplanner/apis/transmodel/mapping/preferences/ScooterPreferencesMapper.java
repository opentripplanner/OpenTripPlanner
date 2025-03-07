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
    callWith.argument("bikeSpeed", scooter::withSpeed);
    callWith.argument("bicycleOptimisationMethod", scooter::withOptimizeType);

    // WALK reluctance is used for backwards compatibility, then overridden
    callWith.argument("walkReluctance", r -> {
      scooter.withReluctance((double) r);
    });

    if (scooter.optimizeType() == VehicleRoutingOptimizeType.TRIANGLE) {
      scooter.withOptimizeTriangle(triangle -> {
        callWith.argument("triangleFactors.time", triangle::withTime);
        callWith.argument("triangleFactors.slope", triangle::withSlope);
        callWith.argument("triangleFactors.safety", triangle::withSafety);
      });
    }

    scooter.withRental(rental -> mapRentalPreferences(rental, callWith));
  }
}
