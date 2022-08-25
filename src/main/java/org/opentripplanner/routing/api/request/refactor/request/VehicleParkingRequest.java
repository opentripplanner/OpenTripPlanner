package org.opentripplanner.routing.api.request.refactor.request;

import java.util.Set;

public class VehicleParkingRequest {

  private Set<String> requiredTags = Set.of();
  private Set<String> bannedTags = Set.of();

  public void setRequiredTags(Set<String> requiredTags) {
    this.requiredTags = requiredTags;
  }

  public Set<String> requiredTags() {
    return requiredTags;
  }

  public void setBannedTags(Set<String> bannedTags) {
    this.bannedTags = bannedTags;
  }

  public Set<String> bannedTags() {
    return bannedTags;
  }
}
