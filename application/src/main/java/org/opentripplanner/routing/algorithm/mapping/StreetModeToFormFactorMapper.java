package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.RentalFormFactor;

public class StreetModeToFormFactorMapper {

  /**
   * Maps street mode to rental form factor (i.e. CAR_RENTAL -> CAR).
   */
  public static RentalFormFactor map(StreetMode streetMode) {
    return switch (streetMode) {
      case BIKE_RENTAL -> RentalFormFactor.BICYCLE;
      case SCOOTER_RENTAL -> RentalFormFactor.SCOOTER;
      case CAR_RENTAL -> RentalFormFactor.CAR;
      // there is no default here, so you get a compiler error when you add a new value to the enum
      case NOT_SET,
        WALK,
        BIKE,
        BIKE_TO_PARK,
        CAR,
        CAR_TO_PARK,
        CAR_PICKUP,
        CAR_HAILING,
        FLEXIBLE -> throw new IllegalStateException(
        "Cannot convert street mode %s to a form factor".formatted(streetMode)
      );
    };
  }
}
