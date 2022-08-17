package org.opentripplanner.routing.api.request.refactor.request;

import java.time.Duration;
import org.opentripplanner.routing.api.request.StreetMode;

public class StreetRequest {
  Duration maxDuration; // <- Default from StreetPreferences
  StreetMode mode = StreetMode.WALK;
  VehicleRentalRequest vehicleRental;
  VehicleParkingRequest vehicleParking;
}
