package org.opentripplanner.routing.api.request.refactor.request;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;

public class VehicleRentalRequest {

  private final Set<RentalVehicleType.FormFactor> allowedFormFactors = new HashSet<>();
  private Set<String> allowedNetworks = Set.of();
  private Set<String> bannedNetworks = Set.of();
  private boolean allowKeepingVehicleAtDestination = false;

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
}
