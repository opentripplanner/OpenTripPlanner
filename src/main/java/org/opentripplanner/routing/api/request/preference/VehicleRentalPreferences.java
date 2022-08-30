package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

public class VehicleRentalPreferences implements Cloneable, Serializable {

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
  private boolean useAvailabilityInformation = false;
  /**
   * Whether arriving at the destination with a rented (station) bicycle is allowed without dropping
   * it off.
   *
   * @see VehicleRentalPreferences#keepingVehicleAtDestinationCost
   * @see VehicleRentalStation#isKeepingVehicleRentalAtDestinationAllowed
   */
  private boolean allowKeepingRentedVehicleAtDestination = false;
  /**
   * The cost of arriving at the destination with the rented bicycle, to discourage doing so.
   *
   * @see RoutingRequest#allowKeepingRentedVehicleAtDestination
   */
  private double keepingVehicleAtDestinationCost = 0;

  public VehicleRentalPreferences clone() {
    try {
      var clone = (VehicleRentalPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public void setPickupTime(int pickupTime) {
    this.pickupTime = pickupTime;
  }

  public int pickupTime() {
    return pickupTime;
  }

  public void setPickupCost(int pickupCost) {
    this.pickupCost = pickupCost;
  }

  public int pickupCost() {
    return pickupCost;
  }

  public void setDropoffTime(int dropoffTime) {
    this.dropoffTime = dropoffTime;
  }

  public int dropoffTime() {
    return dropoffTime;
  }

  public void setDropoffCost(int dropoffCost) {
    this.dropoffCost = dropoffCost;
  }

  public int dropoffCost() {
    return dropoffCost;
  }

  public void setUseAvailabilityInformation(boolean useAvailabilityInformation) {
    this.useAvailabilityInformation = useAvailabilityInformation;
  }

  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  public void setAllowKeepingRentedVehicleAtDestination(
    boolean allowKeepingRentedVehicleAtDestination
  ) {
    this.allowKeepingRentedVehicleAtDestination = allowKeepingRentedVehicleAtDestination;
  }

  public boolean allowKeepingRentedVehicleAtDestination() {
    return allowKeepingRentedVehicleAtDestination;
  }

  public void setKeepingVehicleAtDestinationCost(double keepingVehicleAtDestinationCost) {
    this.keepingVehicleAtDestinationCost = keepingVehicleAtDestinationCost;
  }

  public double keepingVehicleAtDestinationCost() {
    return keepingVehicleAtDestinationCost;
  }
}
