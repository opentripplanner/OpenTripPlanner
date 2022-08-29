package org.opentripplanner.routing.api.request.refactor.request;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;

public class VehicleRentalRequest implements Cloneable, Serializable {

  private Set<RentalVehicleType.FormFactor> allowedFormFactors = new HashSet<>();
  private Set<String> allowedNetworks = Set.of();
  private Set<String> bannedNetworks = Set.of();
  private boolean allowKeepingVehicleAtDestination = false;
  private boolean parkAndRide = false;

  private boolean allow = false;

  public Set<RentalVehicleType.FormFactor> allowedFormFactors() {
    return allowedFormFactors;
  }

  public void setAllowedNetworks(Set<String> allowedNetworks) {
    this.allowedNetworks = allowedNetworks;
  }

  public Set<String> allowedNetworks() {
    return allowedNetworks;
  }

  public void setBannedNetworks(Set<String> bannedNetworks) {
    this.bannedNetworks = bannedNetworks;
  }

  public Set<String> bannedNetworks() {
    return bannedNetworks;
  }

  public void setAllowKeepingVehicleAtDestination(boolean allowKeepingVehicleAtDestination) {
    this.allowKeepingVehicleAtDestination = allowKeepingVehicleAtDestination;
  }

  public boolean allowKeepingVehicleAtDestination() {
    return allowKeepingVehicleAtDestination;
  }

  public VehicleRentalRequest clone() {
    try {
      var clone = (VehicleRentalRequest) super.clone();
      clone.allowedFormFactors = new HashSet<>(this.allowedFormFactors);
      clone.allowedNetworks = new HashSet<>(this.allowedNetworks);
      clone.bannedNetworks = new HashSet<>(this.bannedNetworks);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public void setParkAndRide(boolean parkAndRide) {
    this.parkAndRide = parkAndRide;
  }

  public boolean parkAndRide() {
    return parkAndRide;
  }

  public void setAllow(boolean allow) {
    this.allow = allow;
  }

  public boolean allow() {
    return allow;
  }
}
