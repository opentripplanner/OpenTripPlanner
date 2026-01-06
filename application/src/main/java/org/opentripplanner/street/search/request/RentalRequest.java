package org.opentripplanner.street.search.request;

import static org.opentripplanner.utils.lang.DoubleUtils.doubleEquals;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Preferences for renting a Bike, Car or other type of vehicle.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class RentalRequest {

  public static final RentalRequest DEFAULT = new RentalRequest();
  private final Duration pickupTime;
  private final Cost pickupCost;
  private final Duration dropOffTime;
  private final Cost dropOffCost;

  private final boolean useAvailabilityInformation;
  private final Cost arrivingInRentalVehicleAtDestinationCost;
  private final boolean allowArrivingInRentedVehicleAtDestination;

  private final Set<String> allowedNetworks;
  private final Set<String> bannedNetworks;
  private final double electricAssistSlopeSensitivity;

  private RentalRequest() {
    this.pickupTime = Duration.ofMinutes(1);
    this.pickupCost = Cost.costOfMinutes(2);
    this.dropOffTime = Duration.ofSeconds(30);
    this.dropOffCost = Cost.costOfSeconds(30);
    this.useAvailabilityInformation = false;
    this.arrivingInRentalVehicleAtDestinationCost = Cost.costOfSeconds(0);
    this.allowArrivingInRentedVehicleAtDestination = false;
    this.allowedNetworks = Set.of();
    this.bannedNetworks = Set.of();
    this.electricAssistSlopeSensitivity =
      VehicleRentalPreferences.DEFAULT_ELECTRIC_ASSIST_SLOPE_SENSITIVITY;
  }

  private RentalRequest(Builder builder) {
    this.pickupTime = builder.pickupTime;
    this.pickupCost = builder.pickupCost;
    this.dropOffTime = builder.dropOffTime;
    this.dropOffCost = builder.dropOffCost;
    this.useAvailabilityInformation = builder.useAvailabilityInformation;
    this.arrivingInRentalVehicleAtDestinationCost =
      builder.arrivingInRentalVehicleAtDestinationCost;
    this.allowArrivingInRentedVehicleAtDestination =
      builder.allowArrivingInRentedVehicleAtDestination;
    this.allowedNetworks = builder.allowedNetworks;
    this.bannedNetworks = builder.bannedNetworks;
    this.electricAssistSlopeSensitivity = builder.electricAssistSlopeSensitivity;
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
   * @see RentalRequest#arrivingInRentalVehicleAtDestinationCost()
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

  /**
   * Slope sensitivity for electric-assist rental vehicles (0-1).
   * @see VehicleRentalPreferences#electricAssistSlopeSensitivity()
   */
  public double electricAssistSlopeSensitivity() {
    return electricAssistSlopeSensitivity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RentalRequest that = (RentalRequest) o;
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
      bannedNetworks.equals(that.bannedNetworks) &&
      doubleEquals(electricAssistSlopeSensitivity, that.electricAssistSlopeSensitivity)
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
      bannedNetworks,
      electricAssistSlopeSensitivity
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RentalRequest.class)
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
      .addNum(
        "electricAssistSlopeSensitivity",
        electricAssistSlopeSensitivity,
        DEFAULT.electricAssistSlopeSensitivity
      )
      .toString();
  }

  public static class Builder {

    private final RentalRequest original;
    private Duration pickupTime;
    private Cost pickupCost;
    private Duration dropOffTime;
    private Cost dropOffCost;
    private boolean useAvailabilityInformation;
    private Cost arrivingInRentalVehicleAtDestinationCost;
    private boolean allowArrivingInRentedVehicleAtDestination;
    private Set<String> allowedNetworks;
    private Set<String> bannedNetworks;
    private double electricAssistSlopeSensitivity;

    private Builder(RentalRequest original) {
      this.original = original;
      this.pickupTime = original.pickupTime;
      this.pickupCost = original.pickupCost;
      this.dropOffTime = original.dropOffTime;
      this.dropOffCost = original.dropOffCost;
      this.useAvailabilityInformation = original.useAvailabilityInformation;
      this.arrivingInRentalVehicleAtDestinationCost =
        original.arrivingInRentalVehicleAtDestinationCost;
      this.allowArrivingInRentedVehicleAtDestination =
        original.allowArrivingInRentedVehicleAtDestination;
      this.allowedNetworks = original.allowedNetworks;
      this.bannedNetworks = original.bannedNetworks;
      this.electricAssistSlopeSensitivity = original.electricAssistSlopeSensitivity;
    }

    public Builder withPickupTime(Duration pickupTime) {
      this.pickupTime = pickupTime;
      return this;
    }

    public Builder withPickupCost(Cost pickupCost) {
      this.pickupCost = pickupCost;
      return this;
    }

    public Builder withDropOffTime(Duration dropOffTime) {
      this.dropOffTime = dropOffTime;
      return this;
    }

    public Builder withDropOffCost(Cost dropOffCost) {
      this.dropOffCost = dropOffCost;
      return this;
    }

    public Builder withUseAvailabilityInformation(boolean useAvailabilityInformation) {
      this.useAvailabilityInformation = useAvailabilityInformation;
      return this;
    }

    public Builder withArrivingInRentalVehicleAtDestinationCost(
      Cost arrivingInRentalVehicleAtDestinationCost
    ) {
      this.arrivingInRentalVehicleAtDestinationCost = arrivingInRentalVehicleAtDestinationCost;
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

    public Builder withElectricAssistSlopeSensitivity(double electricAssistSlopeSensitivity) {
      this.electricAssistSlopeSensitivity = electricAssistSlopeSensitivity;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public RentalRequest build() {
      var value = new RentalRequest(this);
      return original.equals(value) ? original : value;
    }
  }
}
