package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Class that stores information about what kind of parking lots should be used for Park & Ride
 * and Bike & Ride searches.
 */
public class VehicleParkingRequest implements Cloneable, Serializable {

  private Set<String> requiredTags = Set.of();
  private Set<String> bannedTags = Set.of();
  private Set<String> preferredTags = Set.of();
  private int unpreferredTagCost = 5 * 60;

  // TODO: Move useAvailabilityInformation here

  public void setRequiredTags(Collection<String> requiredTags) {
    this.requiredTags = Set.copyOf(requiredTags);
  }

  /** Tags which are required to use a vehicle parking. If empty, no tags are required. */
  public Set<String> requiredTags() {
    return requiredTags;
  }

  public void setBannedTags(Collection<String> bannedTags) {
    this.bannedTags = Set.copyOf(bannedTags);
  }

  /** Tags with which a vehicle parking will not be used. If empty, no tags are banned. */
  public Set<String> bannedTags() {
    return bannedTags;
  }

  public void setPreferredTags(Collection<String> tags) {
    this.preferredTags = Set.copyOf(tags);
  }

  /**
   * Which vehicle parking tags are preferred. Vehicle parking facilities that don't have one of these
   * tags receive an extra cost.
   * <p>
   * This is useful if you want to use certain kind of facilities, like lockers for expensive e-bikes.
   */
  public Set<String> preferredTags() {
    return this.preferredTags;
  }

  public void setUnpreferredTagCost(int cost) {
    unpreferredTagCost = cost;
  }

  public int unpreferredTagCost() {
    return unpreferredTagCost;
  }

  public VehicleParkingRequest clone() {
    try {
      var clone = (VehicleParkingRequest) super.clone();
      clone.requiredTags = Set.copyOf(this.requiredTags);
      clone.bannedTags = Set.copyOf(this.bannedTags);
      clone.preferredTags = Set.copyOf(this.preferredTags);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
