package org.opentripplanner.apis.transmodel.mapping.preferences;

import static org.opentripplanner.apis.transmodel.mapping.preferences.RentalPreferencesMapper.mapRentalPreferences;

import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.CarPreferences;

public class CarPreferencesMapper {

  public static void mapCarPreferences(CarPreferences.Builder car, DataFetcherDecorator callWith) {
    // Walk reluctance is used for backward compatibility
    callWith.argument("walkReluctance", car::withReluctance);
    car.withRental(rental -> mapRentalPreferences(rental, callWith));
    // UNSUPPORTED PARAMETERS

    // Override WALK reluctance with CAR reluctance
    // callWith.argument("car.reluctance", car::withReluctance);
    // callWith.argument("car.speed", car::withSpeed);
    // callWith.argument("car.park.cost", car::withParkCost);
    // callWith.argument("car.park.time", car::withParkTime);
    // callWith.argument("car.pickup.cost", car::withPickupCost);
    // callWith.argument("car.pickup.time", car::withPickupTime);
  }
}
