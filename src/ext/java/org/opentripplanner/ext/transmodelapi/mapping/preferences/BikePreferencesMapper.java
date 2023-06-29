package org.opentripplanner.ext.transmodelapi.mapping.preferences;

import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.core.BicycleOptimizeType;

public class BikePreferencesMapper {

  public static void mapBikePreferences(
    BikePreferences.Builder bike,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("bikeSpeed", bike::withSpeed);
    callWith.argument("bikeSwitchTime", bike::withSwitchTime);
    callWith.argument("bikeSwitchCost", bike::withSwitchCost);
    callWith.argument("bicycleOptimisationMethod", bike::withOptimizeType);

    if (bike.optimizeType() == BicycleOptimizeType.TRIANGLE) {
      bike.withOptimizeTriangle(triangle -> {
        callWith.argument("triangleFactors.time", triangle::withTime);
        callWith.argument("triangleFactors.slope", triangle::withSlope);
        callWith.argument("triangleFactors.safety", triangle::withSafety);
      });
    }
  }
}
