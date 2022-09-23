package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;

// TODO VIA: Javadoc
public class VehicleRentalPreferences implements Cloneable, Serializable {

  private int pickupTime = 60;
  private int pickupCost = 120;
  private int dropoffTime = 30;
  private int dropoffCost = 30;

  private boolean useAvailabilityInformation = false;
  private double arrivingInRentalVehicleAtDestinationCost = 0;

  /** Time to rent a vehicle */
  public int pickupTime() {
    return pickupTime;
  }

  public void setPickupTime(int pickupTime) {
    this.pickupTime = pickupTime;
  }

  /**
   * Cost of renting a vehicle. The cost is a bit more than actual time to model the associated cost
   * and trouble.
   */
  public int pickupCost() {
    return pickupCost;
  }

  public void setPickupCost(int pickupCost) {
    this.pickupCost = pickupCost;
  }

  public void setDropoffTime(int dropoffTime) {
    this.dropoffTime = dropoffTime;
  }

  /** Time to drop-off a rented vehicle */
  public int dropoffTime() {
    return dropoffTime;
  }

  /** Cost of dropping-off a rented vehicle */
  public int dropoffCost() {
    return dropoffCost;
  }

  public void setDropoffCost(int dropoffCost) {
    this.dropoffCost = dropoffCost;
  }

  /**
   * Whether or not vehicle rental availability information will be used to plan vehicle rental
   * trips
   *
   * TODO: This belong in the request?
   */
  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  public void setUseAvailabilityInformation(boolean useAvailabilityInformation) {
    this.useAvailabilityInformation = useAvailabilityInformation;
  }

  /**
   * The cost of arriving at the destination with the rented bicycle, to discourage doing so.
   *
   * @see VehicleRentalRequest#allowArrivingInRentedVehicleAtDestination()
   */
  public double arrivingInRentalVehicleAtDestinationCost() {
    return arrivingInRentalVehicleAtDestinationCost;
  }

  public void setArrivingInRentalVehicleAtDestinationCost(
    double arrivingInRentalVehicleAtDestinationCost
  ) {
    this.arrivingInRentalVehicleAtDestinationCost = arrivingInRentalVehicleAtDestinationCost;
  }

  public VehicleRentalPreferences clone() {
    try {
      var clone = (VehicleRentalPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
