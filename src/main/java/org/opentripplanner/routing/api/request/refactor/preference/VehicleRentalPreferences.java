package org.opentripplanner.routing.api.request.refactor.preference;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

public class VehicleRentalPreferences {

  private boolean allow = false;

  /** Time to rent a vehicle */
  private int pickupTime = 60;
  /**
   * Cost of renting a vehicle. The cost is a bit more than actual time to model the associated cost
   * and trouble.
   */
  private int pickupCost = 120;
  /** Time to drop-off a rented vehicle */
  private int dropoffTime = 30;
  /** Cost of dropping-off a rented vehicle */
  private int dropoffCost = 30;
  /**
   * Whether or not vehicle rental availability information will be used to plan vehicle rental
   * trips
   */
  private boolean useVehicleRentalAvailabilityInformation = false;
  // TODO: 2022-08-18 fix documentation
  /**
   * Whether arriving at the destination with a rented (station) bicycle is allowed without dropping
   * it off.
   *
   * @see RoutingRequest#keepingRentedVehicleAtDestinationCost
   * @see VehicleRentalStation#isKeepingVehicleRentalAtDestinationAllowed
   */
  private boolean allowKeepingRentedVehicleAtDestination = false;
  /**
   * The cost of arriving at the destination with the rented bicycle, to discourage doing so.
   *
   * @see RoutingRequest#allowKeepingRentedVehicleAtDestination
   */
  private double keepingVehicleAtDestinationCost = 0;



  public int pickupTime() {
    return pickupTime;
  }

  public int pickupCost() {
    return pickupCost;
  }

  public int dropoffTime() {
    return dropoffTime;
  }

  public int dropoffCost() {
    return dropoffCost;
  }

  public boolean useVehicleRentalAvailabilityInformation() {
    return useVehicleRentalAvailabilityInformation;
  }

  public boolean allowKeepingRentedVehicleAtDestination() {
    return allowKeepingRentedVehicleAtDestination;
  }

  public double keepingVehicleAtDestinationCost() {
    return keepingVehicleAtDestinationCost;
  }

  public boolean allow() {
    return allow;
  }

  public void setAllow(boolean allow) {
    this.allow = allow;
  }
}
