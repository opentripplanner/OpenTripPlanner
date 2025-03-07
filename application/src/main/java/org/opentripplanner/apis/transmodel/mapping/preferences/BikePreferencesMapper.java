package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.opentripplanner.apis.transmodel.mapping.preferences.RentalPreferencesMapper.mapRentalPreferences;

import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

public class BikePreferencesMapper {

  public static final double WALK_BIKE_RELATIVE_RELUCTANCE = 2.7;

  public static void mapBikePreferences(
    BikePreferences.Builder bike,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("bikeSpeed", bike::withSpeed);

    // These are not supported on the Transmodel API
    // callWith.argument("bikeSwitchTime", bike::withSwitchTime);
    // callWith.argument("bikeSwitchCost", bike::withSwitchCost);

    callWith.argument("bicycleOptimisationMethod", bike::withOptimizeType);

    // WALK reluctance is used for backwards compatibility, then overridden
    callWith.argument("walkReluctance", r -> {
      bike.withReluctance((double) r);
      bike.withWalking(w -> w.withReluctance(WALK_BIKE_RELATIVE_RELUCTANCE * (double) r));
    });

    // TODO: Override WALK reluctance with BIKE reluctance
    // callWith.argument("bike.reluctance", r -> {
    //  bike.withReluctance((double)r);
    //  bike.withWalkingReluctance(WALK_BIKE_RELATIVE_RELUCTANCE * (double)r );
    //});

    if (bike.optimizeType() == VehicleRoutingOptimizeType.TRIANGLE) {
      bike.withOptimizeTriangle(triangle -> {
        callWith.argument("triangleFactors.time", triangle::withTime);
        callWith.argument("triangleFactors.slope", triangle::withSlope);
        callWith.argument("triangleFactors.safety", triangle::withSafety);
      });
    }

    bike.withRental(rental -> mapRentalPreferences(rental, callWith));
  }
}
