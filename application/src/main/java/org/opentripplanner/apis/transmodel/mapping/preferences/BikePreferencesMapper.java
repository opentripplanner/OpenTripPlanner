package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.opentripplanner.apis.transmodel.mapping.preferences.RentalPreferencesMapper.mapRentalPreferences;

import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;

public class BikePreferencesMapper {

  public static final double WALK_BIKE_RELATIVE_RELUCTANCE = 2.7;

  public static void mapBikePreferences(
    BikePreferences.Builder bike,
    DataFetcherDecorator callWith
  ) {
    // First, apply deprecated fields (only present if user explicitly provided them)
    callWith.argument("bikeSpeed", bike::withSpeed);
    callWith.argument("bicycleOptimisationMethod", bike::withOptimizeType);

    // Then, apply values from bikePreferences wrapper (takes precedence over deprecated)
    callWith.argument("bikePreferences.speed", bike::withSpeed);
    callWith.argument("bikePreferences.reluctance", (Double reluctance) -> {
      bike.withReluctance(reluctance);
      bike.withWalking(w -> w.withReluctance(WALK_BIKE_RELATIVE_RELUCTANCE * reluctance));
    });
    callWith.argument("bikePreferences.optimisationMethod", bike::withOptimizeType);

    // Handle triangle factors - wrapper takes precedence over deprecated
    if (bike.optimizeType() == VehicleRoutingOptimizeType.TRIANGLE) {
      bike.withOptimizeTriangle(triangle -> {
        // First apply deprecated top-level if provided
        callWith.argument("triangleFactors.time", triangle::withTime);
        callWith.argument("triangleFactors.slope", triangle::withSlope);
        callWith.argument("triangleFactors.safety", triangle::withSafety);
        // Then apply from bikePreferences (takes precedence)
        callWith.argument("bikePreferences.triangleFactors.time", triangle::withTime);
        callWith.argument("bikePreferences.triangleFactors.slope", triangle::withSlope);
        callWith.argument("bikePreferences.triangleFactors.safety", triangle::withSafety);
      });
    }

    bike.withRental(rental -> mapRentalPreferences(rental, callWith));
  }
}
