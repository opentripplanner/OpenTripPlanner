package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Preferences for renting a Bike, Car or other type of vehicle.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class VehicleRentalPreferences implements Serializable {

  public static final VehicleRentalPreferences DEFAULT = new VehicleRentalPreferences();
  private final Duration pickupTime;
  private final Cost pickupCost;
  private final Duration dropOffTime;
  private final Cost dropOffCost;

  private final boolean useAvailabilityInformation;
  private final Cost arrivingInRentalVehicleAtDestinationCost;
  private final boolean allowArrivingInRentedVehicleAtDestination;

  private final Set<String> allowedNetworks;
  private final Set<String> bannedNetworks;

  private VehicleRentalPreferences() {
    this.pickupTime = Duration.ofMinutes(1);
    this.pickupCost = Cost.costOfMinutes(2);
    this.dropOffTime = Duration.ofSeconds(30);
    this.dropOffCost = Cost.costOfSeconds(30);
    this.useAvailabilityInformation = false;
    this.arrivingInRentalVehicleAtDestinationCost = Cost.costOfSeconds(0);
    this.allowArrivingInRentedVehicleAtDestination = false;
    this.allowedNetworks = Set.of();
    this.bannedNetworks = Set.of();
  }

  private VehicleRentalPreferences(Builder builder) {
    this.pickupTime = Duration.ofSeconds(Units.duration(builder.pickupTime));
    this.pickupCost = builder.pickupCost;
    this.dropOffTime = Duration.ofSeconds(Units.duration(builder.dropOffTime));
    this.dropOffCost = builder.dropOffCost;
    this.useAvailabilityInformation = builder.useAvailabilityInformation;
    this.arrivingInRentalVehicleAtDestinationCost =
      builder.arrivingInRentalVehicleAtDestinationCost;
    this.allowArrivingInRentedVehicleAtDestination =
      builder.allowArrivingInRentedVehicleAtDestination;
    this.allowedNetworks = builder.allowedNetworks;
    this.bannedNetworks = builder.bannedNetworks;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /** Time to rent a vehicle */
  public Duration pickupTime() {
    return pickupTime;
  }

  /**
   * Cost of renting a vehicle. The cost is a bit more than actual time to model the associated cost
   * and trouble.
   */
  public Cost pickupCost() {
    return pickupCost;
  }

  /** Time to drop-off a rented vehicle */
  public Duration dropOffTime() {
    return dropOffTime;
  }

  /** Cost of dropping-off a rented vehicle */
  public Cost dropOffCost() {
    return dropOffCost;
  }

  /**
   * Whether or not vehicle rental availability information will be used to plan vehicle rental
   * trips
   */
  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  /**
   * The cost of arriving at the destination with the rented vehicle, to discourage doing so.
   */
  public Cost arrivingInRentalVehicleAtDestinationCost() {
    return arrivingInRentalVehicleAtDestinationCost;
  }

  /**
   * Whether arriving at the destination with a rented (station) vehicle is allowed without dropping
   * it off.
   *
   * @see VehicleRentalPreferences#arrivingInRentalVehicleAtDestinationCost()
   */
  public boolean allowArrivingInRentedVehicleAtDestination() {
    return allowArrivingInRentedVehicleAtDestination;
  }

  /** The vehicle rental networks which may be used. If empty all networks may be used. */
  public Set<String> allowedNetworks() {
    return allowedNetworks;
  }

  /** The vehicle rental networks which may not be used. If empty, no networks are banned. */
  public Set<String> bannedNetworks() {
    return bannedNetworks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalPreferences that = (VehicleRentalPreferences) o;
    return (
      Objects.equals(pickupTime, that.pickupTime) &&
      Objects.equals(pickupCost, that.pickupCost) &&
      Objects.equals(dropOffTime, that.dropOffTime) &&
      Objects.equals(dropOffCost, that.dropOffCost) &&
      useAvailabilityInformation == that.useAvailabilityInformation &&
      Objects.equals(
        that.arrivingInRentalVehicleAtDestinationCost,
        arrivingInRentalVehicleAtDestinationCost
      ) &&
      allowArrivingInRentedVehicleAtDestination == that.allowArrivingInRentedVehicleAtDestination &&
      allowedNetworks.equals(that.allowedNetworks) &&
      bannedNetworks.equals(that.bannedNetworks)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      pickupTime,
      pickupCost,
      dropOffTime,
      dropOffCost,
      useAvailabilityInformation,
      arrivingInRentalVehicleAtDestinationCost,
      allowArrivingInRentedVehicleAtDestination,
      allowedNetworks,
      bannedNetworks
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalPreferences.class)
      .addDuration("pickupTime", pickupTime, DEFAULT.pickupTime)
      .addObj("pickupCost", pickupCost, DEFAULT.pickupCost)
      .addDuration("dropOffTime", dropOffTime, DEFAULT.dropOffTime)
      .addObj("dropOffCost", dropOffCost, DEFAULT.dropOffCost)
      .addBoolIfTrue("useAvailabilityInformation", useAvailabilityInformation)
      .addObj(
        "arrivingInRentalVehicleAtDestinationCost",
        arrivingInRentalVehicleAtDestinationCost,
        DEFAULT.arrivingInRentalVehicleAtDestinationCost
      )
      .addBoolIfTrue(
        "allowArrivingInRentedVehicleAtDestination",
        allowArrivingInRentedVehicleAtDestination
      )
      .addCol("allowedNetworks", allowedNetworks, DEFAULT.allowedNetworks)
      .addCol("bannedNetworks", bannedNetworks, DEFAULT.bannedNetworks)
      .toString();
  }

  public static class Builder {

    private final VehicleRentalPreferences original;
    private int pickupTime;
    private Cost pickupCost;
    private int dropOffTime;
    private Cost dropOffCost;
    private boolean useAvailabilityInformation;
    private Cost arrivingInRentalVehicleAtDestinationCost;
    private boolean allowArrivingInRentedVehicleAtDestination;
    private Set<String> allowedNetworks;
    private Set<String> bannedNetworks;

    private Builder(VehicleRentalPreferences original) {
      this.original = original;
      this.pickupTime = (int) original.pickupTime.toSeconds();
      this.pickupCost = original.pickupCost;
      this.dropOffTime = (int) original.dropOffTime.toSeconds();
      this.dropOffCost = original.dropOffCost;
      this.useAvailabilityInformation = original.useAvailabilityInformation;
      this.arrivingInRentalVehicleAtDestinationCost =
        original.arrivingInRentalVehicleAtDestinationCost;
      this.allowArrivingInRentedVehicleAtDestination =
        original.allowArrivingInRentedVehicleAtDestination;
      this.allowedNetworks = original.allowedNetworks;
      this.bannedNetworks = original.bannedNetworks;
    }

    public VehicleRentalPreferences original() {
      return original;
    }

    public Builder withPickupTime(int pickupTime) {
      this.pickupTime = pickupTime;
      return this;
    }

    public Builder withPickupTime(Duration pickupTime) {
      this.pickupTime = (int) pickupTime.toSeconds();
      return this;
    }

    public Builder withPickupCost(int pickupCost) {
      this.pickupCost = Cost.costOfSeconds(pickupCost);
      return this;
    }

    public Builder withDropOffTime(int dropOffTime) {
      this.dropOffTime = dropOffTime;
      return this;
    }

    public Builder withDropOffTime(Duration dropOffTime) {
      this.dropOffTime = (int) dropOffTime.toSeconds();
      return this;
    }

    public Builder withDropOffCost(int dropOffCost) {
      this.dropOffCost = Cost.costOfSeconds(dropOffCost);
      return this;
    }

    public Builder withUseAvailabilityInformation(boolean useAvailabilityInformation) {
      this.useAvailabilityInformation = useAvailabilityInformation;
      return this;
    }

    public Builder withArrivingInRentalVehicleAtDestinationCost(
      int arrivingInRentalVehicleAtDestinationCost
    ) {
      this.arrivingInRentalVehicleAtDestinationCost = Cost.costOfSeconds(
        arrivingInRentalVehicleAtDestinationCost
      );
      return this;
    }

    public Builder withAllowArrivingInRentedVehicleAtDestination(
      boolean allowArrivingInRentedVehicleAtDestination
    ) {
      this.allowArrivingInRentedVehicleAtDestination = allowArrivingInRentedVehicleAtDestination;
      return this;
    }

    public Builder withAllowedNetworks(Set<String> allowedNetworks) {
      this.allowedNetworks = allowedNetworks;
      return this;
    }

    public Builder withBannedNetworks(Set<String> bannedNetworks) {
      this.bannedNetworks = bannedNetworks;
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
