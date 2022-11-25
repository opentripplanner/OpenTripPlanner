package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.framework.Units;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;

/**
 * Preferences for renting a Bike, Car or other type of vehicle.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class VehicleRentalPreferences implements Serializable {

  public static final VehicleRentalPreferences DEFAULT = new VehicleRentalPreferences();
  private final int pickupTime;
  private final int pickupCost;
  private final int dropoffTime;
  private final int dropoffCost;

  private final boolean useAvailabilityInformation;
  private final double arrivingInRentalVehicleAtDestinationCost;

  private VehicleRentalPreferences() {
    this.pickupTime = 60;
    this.pickupCost = 120;
    this.dropoffTime = 30;
    this.dropoffCost = 30;
    this.useAvailabilityInformation = false;
    this.arrivingInRentalVehicleAtDestinationCost = 0;
  }

  private VehicleRentalPreferences(Builder builder) {
    this.pickupTime = builder.pickupTime;
    this.pickupCost = builder.pickupCost;
    this.dropoffTime = builder.dropoffTime;
    this.dropoffCost = builder.dropoffCost;
    this.useAvailabilityInformation = builder.useAvailabilityInformation;
    this.arrivingInRentalVehicleAtDestinationCost =
      DoubleUtils.roundTo1Decimal(builder.arrivingInRentalVehicleAtDestinationCost);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /** Time to rent a vehicle */
  public int pickupTime() {
    return pickupTime;
  }

  /**
   * Cost of renting a vehicle. The cost is a bit more than actual time to model the associated cost
   * and trouble.
   */
  public int pickupCost() {
    return pickupCost;
  }

  /** Time to drop-off a rented vehicle */
  public int dropoffTime() {
    return dropoffTime;
  }

  /** Cost of dropping-off a rented vehicle */
  public int dropoffCost() {
    return dropoffCost;
  }

  /**
   * Whether or not vehicle rental availability information will be used to plan vehicle rental
   * trips
   *
   * TODO: This belong in the request?
   */
  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  /**
   * The cost of arriving at the destination with the rented bicycle, to discourage doing so.
   *
   * @see VehicleRentalRequest#allowArrivingInRentedVehicleAtDestination()
   */
  public double arrivingInRentalVehicleAtDestinationCost() {
    return arrivingInRentalVehicleAtDestinationCost;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalPreferences that = (VehicleRentalPreferences) o;
    return (
      pickupTime == that.pickupTime &&
      pickupCost == that.pickupCost &&
      dropoffTime == that.dropoffTime &&
      dropoffCost == that.dropoffCost &&
      useAvailabilityInformation == that.useAvailabilityInformation &&
      Double.compare(
        that.arrivingInRentalVehicleAtDestinationCost,
        arrivingInRentalVehicleAtDestinationCost
      ) ==
      0
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      pickupTime,
      pickupCost,
      dropoffTime,
      dropoffCost,
      useAvailabilityInformation,
      arrivingInRentalVehicleAtDestinationCost
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(VehicleRentalPreferences.class)
      .addDurationSec("pickupTime", pickupTime, DEFAULT.pickupTime)
      .addCost("pickupCost", pickupCost, DEFAULT.pickupCost)
      .addDurationSec("dropoffTime", dropoffTime, DEFAULT.dropoffTime)
      .addCost("dropoffCost", dropoffCost, DEFAULT.dropoffCost)
      .addBoolIfTrue("useAvailabilityInformation", useAvailabilityInformation)
      .addNum(
        "arrivingInRentalVehicleAtDestinationCost",
        arrivingInRentalVehicleAtDestinationCost,
        DEFAULT.arrivingInRentalVehicleAtDestinationCost
      )
      .toString();
  }

  public static class Builder {

    private final VehicleRentalPreferences original;
    private int pickupTime;
    private int pickupCost;
    private int dropoffTime;
    private int dropoffCost;
    private boolean useAvailabilityInformation;
    private double arrivingInRentalVehicleAtDestinationCost;

    private Builder(VehicleRentalPreferences original) {
      this.original = original;
      this.pickupTime = Units.duration(original.pickupTime);
      this.pickupCost = Units.cost(original.pickupCost);
      this.dropoffTime = Units.duration(original.dropoffTime);
      this.dropoffCost = Units.cost(original.dropoffCost);
      this.useAvailabilityInformation = original.useAvailabilityInformation;
      this.arrivingInRentalVehicleAtDestinationCost =
        original.arrivingInRentalVehicleAtDestinationCost;
    }

    public VehicleRentalPreferences original() {
      return original;
    }

    public Builder withPickupTime(int pickupTime) {
      this.pickupTime = pickupTime;
      return this;
    }

    public Builder withPickupCost(int pickupCost) {
      this.pickupCost = pickupCost;
      return this;
    }

    public Builder withDropoffTime(int dropoffTime) {
      this.dropoffTime = dropoffTime;
      return this;
    }

    public Builder withDropoffCost(int dropoffCost) {
      this.dropoffCost = dropoffCost;
      return this;
    }

    public Builder withUseAvailabilityInformation(boolean useAvailabilityInformation) {
      this.useAvailabilityInformation = useAvailabilityInformation;
      return this;
    }

    public Builder withArrivingInRentalVehicleAtDestinationCost(
      double arrivingInRentalVehicleAtDestinationCost
    ) {
      this.arrivingInRentalVehicleAtDestinationCost = arrivingInRentalVehicleAtDestinationCost;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public VehicleRentalPreferences build() {
      var value = new VehicleRentalPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
