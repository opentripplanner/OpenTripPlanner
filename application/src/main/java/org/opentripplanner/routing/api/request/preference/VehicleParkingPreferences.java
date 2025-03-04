package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingFilter;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingSelect;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The parking preferences contain preferences for car and bicycle parking. These preferences
 * include filtering, preference and realtime usage.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class VehicleParkingPreferences implements Serializable {

  public static final VehicleParkingPreferences DEFAULT = new VehicleParkingPreferences();
  private final Cost unpreferredVehicleParkingTagCost;
  private final VehicleParkingFilter filter;
  private final VehicleParkingFilter preferred;
  private final Duration time;
  private final Cost cost;

  /** Create a new instance with default values. */
  private VehicleParkingPreferences() {
    this.unpreferredVehicleParkingTagCost = Cost.costOfMinutes(5);
    this.filter = VehicleParkingFilter.empty();
    this.preferred = VehicleParkingFilter.empty();
    this.time = Duration.ofMinutes(1);
    this.cost = Cost.costOfMinutes(2);
  }

  private VehicleParkingPreferences(Builder builder) {
    this.unpreferredVehicleParkingTagCost = builder.unpreferredVehicleParkingTagCost;
    this.filter = new VehicleParkingFilter(
      builder.bannedVehicleParkingTags,
      builder.requiredVehicleParkingTags
    );
    this.preferred = new VehicleParkingFilter(
      builder.notPreferredVehicleParkingTags,
      builder.preferredVehicleParkingTags
    );
    this.time = builder.time;
    this.cost = builder.cost;
  }

  public static VehicleParkingPreferences.Builder of() {
    return new Builder(DEFAULT);
  }

  public VehicleParkingPreferences.Builder copyOf() {
    return new Builder(this);
  }

  /**
   * What cost is applied to using parking that is not preferred.
   */
  public Cost unpreferredVehicleParkingTagCost() {
    return unpreferredVehicleParkingTagCost;
  }

  /**
   * Parking containing select filters must only be usable and parking containing with not filters
   * cannot be used.
   */
  public VehicleParkingFilter filter() {
    return filter;
  }

  /**
   * Which vehicle parking tags are preferred. Vehicle parking facilities that don't have one of these
   * tags receive an extra cost.
   * <p>
   * This is useful if you want to use certain kind of facilities, like lockers for expensive e-bikes.
   */
  public VehicleParkingFilter preferred() {
    return preferred;
  }

  /** Time to park a vehicle */
  public Duration time() {
    return time;
  }

  /** Cost of parking a bike. */
  public Cost cost() {
    return cost;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleParkingPreferences that = (VehicleParkingPreferences) o;
    return (
      Objects.equals(unpreferredVehicleParkingTagCost, that.unpreferredVehicleParkingTagCost) &&
      Objects.equals(filter, that.filter) &&
      Objects.equals(preferred, that.preferred) &&
      Objects.equals(cost, that.cost) &&
      Objects.equals(time, that.time)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(unpreferredVehicleParkingTagCost, filter, preferred, cost, time);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleParkingPreferences.class)
      .addObj(
        "unpreferredVehicleParkingTagCost",
        unpreferredVehicleParkingTagCost,
        DEFAULT.unpreferredVehicleParkingTagCost
      )
      .addObj("filter", filter, DEFAULT.filter)
      .addObj("preferred", preferred, DEFAULT.preferred)
      .addObj("cost", cost, DEFAULT.cost)
      .addObj("time", time, DEFAULT.time)
      .toString();
  }

  public static class Builder {

    private final VehicleParkingPreferences original;
    private Cost unpreferredVehicleParkingTagCost;
    private List<VehicleParkingSelect> bannedVehicleParkingTags;
    private List<VehicleParkingSelect> requiredVehicleParkingTags;
    private List<VehicleParkingSelect> preferredVehicleParkingTags;
    private List<VehicleParkingSelect> notPreferredVehicleParkingTags;
    private Cost cost;
    private Duration time;

    private Builder(VehicleParkingPreferences original) {
      this.original = original;
      this.unpreferredVehicleParkingTagCost = original.unpreferredVehicleParkingTagCost;
      this.bannedVehicleParkingTags = original.filter.not();
      this.requiredVehicleParkingTags = original.filter.select();
      this.preferredVehicleParkingTags = original.preferred.select();
      this.notPreferredVehicleParkingTags = original.preferred.not();
      this.cost = original.cost;
      this.time = original.time;
    }

    public Builder withUnpreferredVehicleParkingTagCost(int cost) {
      this.unpreferredVehicleParkingTagCost = Cost.costOfSeconds(cost);
      return this;
    }

    public Builder withBannedVehicleParkingTags(Set<String> bannedVehicleParkingTags) {
      this.bannedVehicleParkingTags = List.of(
        new VehicleParkingSelect.TagsSelect(bannedVehicleParkingTags)
      );
      return this;
    }

    public Builder withRequiredVehicleParkingTags(Set<String> requiredVehicleParkingTags) {
      this.requiredVehicleParkingTags = List.of(
        new VehicleParkingSelect.TagsSelect(requiredVehicleParkingTags)
      );
      return this;
    }

    public Builder withPreferredVehicleParkingTags(Set<String> preferredVehicleParkingTags) {
      this.preferredVehicleParkingTags = List.of(
        new VehicleParkingSelect.TagsSelect(preferredVehicleParkingTags)
      );
      return this;
    }

    public Builder withNotPreferredVehicleParkingTags(Set<String> notPreferredVehicleParkingTags) {
      this.notPreferredVehicleParkingTags = List.of(
        new VehicleParkingSelect.TagsSelect(notPreferredVehicleParkingTags)
      );
      return this;
    }

    public Builder withCost(int cost) {
      this.cost = Cost.costOfSeconds(cost);
      return this;
    }

    public Builder withTime(int seconds) {
      this.time = Duration.ofSeconds(seconds);
      return this;
    }

    public Builder withTime(Duration duration) {
      this.time = duration;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public VehicleParkingPreferences build() {
      var newObj = new VehicleParkingPreferences(this);
      return original.equals(newObj) ? original : newObj;
    }
  }
}
