package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

// TODO VIA: Javadoc
public class VehicleRentalRequest implements Cloneable, Serializable {

  private Set<String> allowedNetworks = Set.of();
  private Set<String> bannedNetworks = Set.of();

  private boolean allowArrivingInRentedVehicleAtDestination = false;

  // TODO VIA (Hannes): Move useAvailabilityInformation here

  public void setAllowedNetworks(Set<String> allowedNetworks) {
    this.allowedNetworks = allowedNetworks;
  }

  /** The vehicle rental networks which may be used. If empty all networks may be used. */
  public Set<String> allowedNetworks() {
    return allowedNetworks;
  }

  public void setBannedNetworks(Set<String> bannedNetworks) {
    this.bannedNetworks = bannedNetworks;
  }

  /** The vehicle rental networks which may not be used. If empty, no networks are banned. */
  public Set<String> bannedNetworks() {
    return bannedNetworks;
  }

  public void setAllowArrivingInRentedVehicleAtDestination(
    boolean allowArrivingInRentedVehicleAtDestination
  ) {
    this.allowArrivingInRentedVehicleAtDestination = allowArrivingInRentedVehicleAtDestination;
  }

  /**
   * Whether arriving at the destination with a rented (station) bicycle is allowed without dropping
   * it off.
   *
   * @see VehicleRentalPreferences#arrivingInRentalVehicleAtDestinationCost()
   * @see VehicleRentalStation#isArrivingInRentalVehicleAtDestinationAllowed
   */
  public boolean allowArrivingInRentedVehicleAtDestination() {
    return allowArrivingInRentedVehicleAtDestination;
  }

  public VehicleRentalRequest clone() {
    try {
      var clone = (VehicleRentalRequest) super.clone();
      clone.allowedNetworks = new HashSet<>(this.allowedNetworks);
      clone.bannedNetworks = new HashSet<>(this.bannedNetworks);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
