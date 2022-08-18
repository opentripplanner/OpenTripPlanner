package org.opentripplanner.routing.api.request.refactor.request;

import java.util.Set;

public class VehicleParkingRequest {
  private Set<String> requiredTags = Set.of();
  private Set<String> bannedTags = Set.of();
  private boolean useAvailabilityInformation = false;

  public Set<String> requiredTags() {
    return requiredTags;
  }

  public Set<String> bannedTags() {
    return bannedTags;
  }

  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }
}
