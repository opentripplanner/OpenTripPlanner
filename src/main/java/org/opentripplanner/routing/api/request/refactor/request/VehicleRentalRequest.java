package org.opentripplanner.routing.api.request.refactor.request;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;

public class VehicleRentalRequest {
  Set<RentalVehicleType.FormFactor> allowedFormFactors = new HashSet<>();
  Set<String> allowedNetworks = Set.of();
  Set<String> bannedNetworks = Set.of();
  boolean useAvailabilityInformation = false;
  boolean allowKeepingVehicleAtDestination = false;
}
