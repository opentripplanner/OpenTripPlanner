package org.opentripplanner.routing.api.request.refactor.request;

import java.time.Duration;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.api.request.StreetMode;

public class StreetRequest {

  /**
   * This is the maximum duration for a direct street search. This is a performance limit and should
   * therefore be set high. Results close to the limit are not guaranteed to be optimal.
   * Use filters to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  private Duration maxDuration; // <- Default from StreetPreferences
  // TODO: 2022-08-18 Do we need this?
  private StreetMode mode = StreetMode.WALK;
  // TODO: 2022-08-18 Not sure if having those two here makes sene
  private VehicleRentalRequest vehicleRental;
  private VehicleParkingRequest vehicleParking;

  public Duration maxDuration() {
    return maxDuration;
  }

  public void setMode(StreetMode mode) {
    this.mode = mode;
  }

  public StreetMode mode() {
    return mode;
  }

  public VehicleRentalRequest vehicleRental() {
    return vehicleRental;
  }

  public VehicleParkingRequest vehicleParking() {
    return vehicleParking;
  }
}
