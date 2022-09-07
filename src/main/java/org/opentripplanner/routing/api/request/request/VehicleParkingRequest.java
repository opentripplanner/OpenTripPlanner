package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

// TODO VIA: Javadoc
public class VehicleParkingRequest implements Cloneable, Serializable {

  private Set<String> requiredTags = Set.of();
  private Set<String> bannedTags = Set.of();

  // TODO: Move useAvailabilityInformation here

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

  public VehicleParkingRequest clone() {
    try {
      var clone = (VehicleParkingRequest) super.clone();
      clone.requiredTags = new HashSet<>(this.requiredTags);
      clone.bannedTags = new HashSet<>(this.bannedTags);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
