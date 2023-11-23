package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.request.filter.VehicleParkingSelect;
import org.opentripplanner.routing.api.request.request.filter.VehicleParkingFilterRequest;

/**
 * The parking preferences contain preferences for car and bicycle parking. These preferences
 * include filtering, preference and realtime usage.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class VehicleParkingPreferences implements Serializable {

  public static final VehicleParkingPreferences DEFAULT = new VehicleParkingPreferences();
  private final Cost unpreferredVehicleParkingTagCost;
  private final boolean ignoreRealtimeAvailability;
  private final VehicleParkingFilterRequest filter;
  private final VehicleParkingFilterRequest preferred;
  private final Duration parkTime;
  private final Cost parkCost;

  /** Create a new instance with default values. */
  private VehicleParkingPreferences() {
    this.unpreferredVehicleParkingTagCost = Cost.costOfMinutes(5);
    this.ignoreRealtimeAvailability = false;
    this.filter = VehicleParkingFilterRequest.empty();
    this.preferred = VehicleParkingFilterRequest.empty();
    this.parkTime = Duration.ofMinutes(1);
    this.parkCost = Cost.costOfMinutes(2);
  }

  private VehicleParkingPreferences(Builder builder) {
    this.unpreferredVehicleParkingTagCost = builder.unpreferredVehicleParkingTagCost;
    this.ignoreRealtimeAvailability = builder.ignoreRealtimeAvailability;
    this.filter =
      new VehicleParkingFilterRequest(
        builder.bannedVehicleParkingTags,
        builder.requiredVehicleParkingTags
      );
    this.preferred =
      new VehicleParkingFilterRequest(
        builder.notPreferredVehicleParkingTags,
        builder.preferredVehicleParkingTags
      );
    this.parkTime = builder.parkTime;
    this.parkCost = builder.parkCost;
  }

  public static VehicleParkingPreferences.Builder of() {
    return new Builder(DEFAULT);
  }

  /**
   * What cost is applied to using parking that is not preferred.
   */
  public Cost unpreferredVehicleParkingTagCost() {
    return unpreferredVehicleParkingTagCost;
  }

  /**
   * Should availability information be used during routing.
   */
  public boolean ignoreRealtimeAvailability() {
    return ignoreRealtimeAvailability;
  }

  /**
   * Parking containing select filters must only be usable and parking containing with not filters
   * cannot be used.
   */
  public VehicleParkingFilterRequest filter() {
    return filter;
  }

  /**
   * Which vehicle parking tags are preferred. Vehicle parking facilities that don't have one of these
   * tags receive an extra cost.
   * <p>
   * This is useful if you want to use certain kind of facilities, like lockers for expensive e-bikes.
   */
  public VehicleParkingFilterRequest preferred() {
    return preferred;
  }

  /** Time to park a vehicle */
  public Duration parkTime() {
    return parkTime;
  }

  /** Cost of parking a bike. */
  public Cost parkCost() {
    return parkCost;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleParkingPreferences that = (VehicleParkingPreferences) o;
    return (
      Objects.equals(unpreferredVehicleParkingTagCost, that.unpreferredVehicleParkingTagCost) &&
      ignoreRealtimeAvailability == that.ignoreRealtimeAvailability &&
      Objects.equals(filter, that.filter) &&
      Objects.equals(preferred, that.preferred) &&
      Objects.equals(parkCost, that.parkCost) &&
      Objects.equals(parkTime, that.parkTime)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      unpreferredVehicleParkingTagCost,
      ignoreRealtimeAvailability,
      filter,
      preferred,
      parkCost,
      parkTime
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(VehicleParkingPreferences.class)
      .addObj(
        "unpreferredVehicleParkingTagCost",
        unpreferredVehicleParkingTagCost,
        DEFAULT.unpreferredVehicleParkingTagCost
      )
      .addBoolIfTrue("ignoreRealtimeAvailability", ignoreRealtimeAvailability)
      .addObj("filter", filter, DEFAULT.filter)
      .addObj("preferred", preferred, DEFAULT.preferred)
      .addObj("parkCost", parkCost, DEFAULT.parkCost)
      .addObj("parkTime", parkTime, DEFAULT.parkTime)
      .toString();
  }

  public static class Builder {

    private final VehicleParkingPreferences original;
    private Cost unpreferredVehicleParkingTagCost;
    private boolean ignoreRealtimeAvailability;
    private List<VehicleParkingSelect> bannedVehicleParkingTags;
    private List<VehicleParkingSelect> requiredVehicleParkingTags;
    private List<VehicleParkingSelect> preferredVehicleParkingTags;
    private List<VehicleParkingSelect> notPreferredVehicleParkingTags;
    private Cost parkCost;
    private Duration parkTime;

    private Builder(VehicleParkingPreferences original) {
      this.original = original;
      this.unpreferredVehicleParkingTagCost = original.unpreferredVehicleParkingTagCost;
      this.ignoreRealtimeAvailability = original.ignoreRealtimeAvailability;
      this.bannedVehicleParkingTags = original.filter.not();
      this.requiredVehicleParkingTags = original.filter.select();
      this.preferredVehicleParkingTags = original.preferred.select();
      this.notPreferredVehicleParkingTags = original.preferred.not();
      this.parkCost = original.parkCost;
      this.parkTime = original.parkTime;
    }

    public Builder withUnpreferredVehicleParkingTagCost(int cost) {
      this.unpreferredVehicleParkingTagCost = Cost.costOfSeconds(cost);
      return this;
    }

    public Builder withIgnoreRealtimeAvailability(boolean ignoreRealtimeAvailability) {
      this.ignoreRealtimeAvailability = ignoreRealtimeAvailability;
      return this;
    }

    public Builder withBannedVehicleParkingTags(Set<String> bannedVehicleParkingTags) {
      this.bannedVehicleParkingTags =
        List.of(new VehicleParkingSelect.TagsSelect(bannedVehicleParkingTags));
      return this;
    }

    public Builder withRequiredVehicleParkingTags(Set<String> requiredVehicleParkingTags) {
      this.requiredVehicleParkingTags =
        List.of(new VehicleParkingSelect.TagsSelect(requiredVehicleParkingTags));
      return this;
    }

    public Builder withPreferredVehicleParkingTags(Set<String> preferredVehicleParkingTags) {
      this.preferredVehicleParkingTags =
        List.of(new VehicleParkingSelect.TagsSelect(preferredVehicleParkingTags));
      return this;
    }

    public Builder withNotPreferredVehicleParkingTags(Set<String> notPreferredVehicleParkingTags) {
      this.notPreferredVehicleParkingTags =
        List.of(new VehicleParkingSelect.TagsSelect(notPreferredVehicleParkingTags));
      return this;
    }

    public Builder withParkCost(int cost) {
      this.parkCost = Cost.costOfSeconds(cost);
      return this;
    }

    public Builder withParkTime(int seconds) {
      this.parkTime = Duration.ofSeconds(seconds);
      return this;
    }

    public Builder withParkTime(Duration duration) {
      this.parkTime = duration;
      return this;
    }

    public VehicleParkingPreferences build() {
      var newObj = new VehicleParkingPreferences(this);
      return original.equals(newObj) ? original : newObj;
    }
  }
}
