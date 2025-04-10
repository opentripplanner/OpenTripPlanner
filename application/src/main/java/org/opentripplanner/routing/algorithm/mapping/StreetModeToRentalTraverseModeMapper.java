package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.TraverseMode;

public class StreetModeToRentalTraverseModeMapper {

  /**
   * Maps street mode to rental traverse mode (i.e. CAR_RENTAL -> CAR).
   */
  public static TraverseMode map(StreetMode mode) {
    return switch (mode) {
      case BIKE_RENTAL -> TraverseMode.BICYCLE;
      case SCOOTER_RENTAL -> TraverseMode.SCOOTER;
      case CAR_RENTAL -> TraverseMode.CAR;
      case NOT_SET,
        WALK,
        BIKE,
        BIKE_TO_PARK,
        CAR,
        CAR_TO_PARK,
        CAR_PICKUP,
        CAR_HAILING,
        FLEXIBLE -> throw new IllegalArgumentException("%s is not a rental mode.".formatted(mode));
    };
  }
}
