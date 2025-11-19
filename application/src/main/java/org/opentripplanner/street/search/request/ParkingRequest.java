package org.opentripplanner.street.search.request;

import static java.util.Objects.requireNonNullElse;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.street.search.request.filter.ParkingFilter;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The parking preferences contain preferences for car and bicycle parking. These preferences
 * include filtering, preference and realtime usage.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ParkingRequest {

  public static final ParkingRequest DEFAULT = new ParkingRequest();
  private final Cost unpreferredVehicleParkingTagCost;
  private final ParkingFilter filter;
  private final ParkingFilter preferred;
  private final Duration time;
  private final Cost cost;

  /** Create a new instance with default values. */
  private ParkingRequest() {
    this.unpreferredVehicleParkingTagCost = Cost.costOfMinutes(5);
    this.filter = ParkingFilter.empty();
    this.preferred = ParkingFilter.empty();
    this.time = Duration.ofMinutes(1);
    this.cost = Cost.costOfMinutes(2);
  }

  private ParkingRequest(Builder builder) {
    this.unpreferredVehicleParkingTagCost = builder.unpreferredVehicleParkingTagCost;
    this.filter = requireNonNullElse(builder.filter, ParkingFilter.empty());
    this.preferred = requireNonNullElse(builder.preferred, ParkingFilter.empty());
    this.time = builder.time;
    this.cost = builder.cost;
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
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
  public ParkingFilter filter() {
    return filter;
  }

  /**
   * Which vehicle parking tags are preferred. Vehicle parking facilities that don't have one of these
   * tags receive an extra cost.
   * <p>
   * This is useful if you want to use certain kind of facilities, like lockers for expensive e-bikes.
   */
  public ParkingFilter preferred() {
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
    ParkingRequest that = (ParkingRequest) o;
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
    return ToStringBuilder.of(ParkingRequest.class)
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

    private final ParkingRequest original;
    private Cost unpreferredVehicleParkingTagCost;
    private Cost cost;
    private Duration time;
    private ParkingFilter filter;
    private ParkingFilter preferred;

    private Builder(ParkingRequest original) {
      this.original = original;
      this.unpreferredVehicleParkingTagCost = original.unpreferredVehicleParkingTagCost;
      this.cost = original.cost;
      this.time = original.time;
      this.filter = original.filter;
      this.preferred = original.preferred;
    }

    public Builder withUnpreferredTagCost(Cost cost) {
      this.unpreferredVehicleParkingTagCost = cost;
      return this;
    }

    public Builder withCost(Cost cost) {
      this.cost = cost;
      return this;
    }

    public Builder withTime(Duration duration) {
      this.time = duration;
      return this;
    }

    public Builder withFilter(ParkingFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder withPreferred(ParkingFilter preferred) {
      this.preferred = preferred;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public ParkingRequest build() {
      var newObj = new ParkingRequest(this);
      return original.equals(newObj) ? original : newObj;
    }
  }
}
