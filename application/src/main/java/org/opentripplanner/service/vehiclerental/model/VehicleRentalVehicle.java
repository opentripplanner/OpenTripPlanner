package org.opentripplanner.service.vehiclerental.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Implements the {@link VehicleRentalPlace} class which contains Javadoc.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class VehicleRentalVehicle implements VehicleRentalPlace {

  public static final VehicleRentalVehicle DEFAULT = new VehicleRentalVehicle();

  private final FeedScopedId id;
  private final I18NString name;
  private final double longitude;
  private final double latitude;

  private final VehicleRentalSystem system;
  private final RentalVehicleType vehicleType;
  private final VehicleRentalStationUris rentalUris;
  private final boolean isReserved;
  private final boolean isDisabled;
  private final Instant lastReported;
  private final VehicleRentalStation station;
  private final String pricingPlanId;
  private final RentalVehicleFuel fuel;
  private final OffsetDateTime availableUntil;

  public VehicleRentalVehicle() {
    this.id = null;
    this.name = null;
    this.longitude = 0.0;
    this.latitude = 0.0;
    this.system = null;
    this.vehicleType = null;
    this.rentalUris = null;
    this.isReserved = false;
    this.isDisabled = false;
    this.lastReported = null;
    this.station = null;
    this.pricingPlanId = null;
    this.fuel = null;
    this.availableUntil = null;
  }

  private VehicleRentalVehicle(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.longitude = builder.longitude;
    this.latitude = builder.latitude;
    this.system = builder.system;
    this.vehicleType = builder.vehicleType;
    this.rentalUris = builder.rentalUris;
    this.isReserved = builder.isReserved;
    this.isDisabled = builder.isDisabled;
    this.lastReported = builder.lastReported;
    this.station = builder.station;
    this.pricingPlanId = builder.pricingPlanId;
    this.fuel = builder.fuel;
    this.availableUntil = builder.availableUntil;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public FeedScopedId id() {
    return id;
  }

  @Nullable
  public I18NString name() {
    return name;
  }

  public double longitude() {
    return longitude;
  }

  public double latitude() {
    return latitude;
  }

  @Nullable
  public VehicleRentalSystem system() {
    return system;
  }

  @Nullable
  public RentalVehicleType vehicleType() {
    return vehicleType;
  }

  @Nullable
  public VehicleRentalStationUris rentalUris() {
    return rentalUris;
  }

  public boolean isReserved() {
    return isReserved;
  }

  public boolean isDisabled() {
    return isDisabled;
  }

  @Nullable
  public Instant lastReported() {
    return lastReported;
  }

  @Nullable
  public VehicleRentalStation station() {
    return station;
  }

  @Nullable
  public String pricingPlanId() {
    return pricingPlanId;
  }

  @Nullable
  public RentalVehicleFuel fuel() {
    return fuel;
  }

  @Nullable
  public OffsetDateTime availableUntil() {
    return availableUntil;
  }

  @Override
  public String stationId() {
    return this.id().getId();
  }

  @Override
  public String network() {
    return this.id().getFeedId();
  }

  @Override
  public int vehiclesAvailable() {
    return 1;
  }

  @Override
  public int spacesAvailable() {
    return 0;
  }

  @Override
  public Integer capacity() {
    return 0;
  }

  @Override
  public boolean isAllowDropoff() {
    return false;
  }

  @Override
  public boolean overloadingAllowed() {
    return false;
  }

  @Override
  public boolean isAllowPickup() {
    return !isDisabled;
  }

  public boolean allowPickupNow() {
    return !isReserved && !isDisabled;
  }

  public boolean allowDropoffNow() {
    return false;
  }

  @Override
  public boolean isFloatingVehicle() {
    return true;
  }

  @Override
  public boolean isCarStation() {
    return vehicleType.formFactor().equals(RentalFormFactor.CAR);
  }

  @Override
  public Set<RentalFormFactor> availablePickupFormFactors(boolean includeRealtimeAvailability) {
    return Set.of(vehicleType.formFactor());
  }

  @Override
  public Set<RentalFormFactor> availableDropoffFormFactors(boolean includeRealtimeAvailability) {
    return Set.of();
  }

  @Override
  public boolean isArrivingInRentalVehicleAtDestinationAllowed() {
    return false;
  }

  @Override
  public boolean isRealTimeData() {
    return true;
  }

  @Override
  public VehicleRentalSystem vehicleRentalSystem() {
    return system;
  }

  public RentalVehicleFuel getFuel() {
    return fuel;
  }

  public OffsetDateTime getAvailableUntil() {
    return availableUntil;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalVehicle that = (VehicleRentalVehicle) o;
    return (
      Double.compare(that.longitude, longitude) == 0 &&
      Double.compare(that.latitude, latitude) == 0 &&
      isReserved == that.isReserved &&
      isDisabled == that.isDisabled &&
      Objects.equals(id, that.id) &&
      Objects.equals(name, that.name) &&
      Objects.equals(system, that.system) &&
      Objects.equals(vehicleType, that.vehicleType) &&
      Objects.equals(rentalUris, that.rentalUris) &&
      Objects.equals(lastReported, that.lastReported) &&
      Objects.equals(station, that.station) &&
      Objects.equals(pricingPlanId, that.pricingPlanId) &&
      Objects.equals(fuel, that.fuel) &&
      Objects.equals(availableUntil, that.availableUntil)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      id,
      name,
      longitude,
      latitude,
      system,
      vehicleType,
      rentalUris,
      isReserved,
      isDisabled,
      lastReported,
      station,
      pricingPlanId,
      fuel,
      availableUntil
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalVehicle.class)
      .addObj("id", id, DEFAULT.id)
      .addObj("name", name, DEFAULT.name)
      .addNum("longitude", longitude, DEFAULT.longitude)
      .addNum("latitude", latitude, DEFAULT.latitude)
      .addObj("system", system, DEFAULT.system)
      .addObj("vehicleType", vehicleType, DEFAULT.vehicleType)
      .addObj("rentalUris", rentalUris, DEFAULT.rentalUris)
      .addBoolIfTrue("isReserved", isReserved)
      .addBoolIfTrue("isDisabled", isDisabled)
      .addObj("lastReported", lastReported, DEFAULT.lastReported)
      .addObj("station", station, DEFAULT.station)
      .addStr("pricingPlanId", pricingPlanId, DEFAULT.pricingPlanId)
      .addObj("fuel", fuel, DEFAULT.fuel)
      .addObj("availableUntil", availableUntil, DEFAULT.availableUntil)
      .toString();
  }

  public static class Builder {

    private final VehicleRentalVehicle original;
    private FeedScopedId id;
    private I18NString name;
    private double longitude;
    private double latitude;
    private VehicleRentalSystem system;
    private RentalVehicleType vehicleType;
    private VehicleRentalStationUris rentalUris;
    private boolean isReserved;
    private boolean isDisabled;
    private Instant lastReported;
    private VehicleRentalStation station;
    private String pricingPlanId;
    private RentalVehicleFuel fuel;
    private OffsetDateTime availableUntil;

    private Builder(VehicleRentalVehicle original) {
      this.original = original;
      this.id = original.id;
      this.name = original.name;
      this.longitude = original.longitude;
      this.latitude = original.latitude;
      this.system = original.system;
      this.vehicleType = original.vehicleType;
      this.rentalUris = original.rentalUris;
      this.isReserved = original.isReserved;
      this.isDisabled = original.isDisabled;
      this.lastReported = original.lastReported;
      this.station = original.station;
      this.pricingPlanId = original.pricingPlanId;
      this.fuel = original.fuel;
      this.availableUntil = original.availableUntil;
    }

    public Builder withId(FeedScopedId id) {
      this.id = id;
      return this;
    }

    public Builder withName(@Nullable I18NString name) {
      this.name = name;
      return this;
    }

    public Builder withLongitude(double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder withLatitude(double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder withSystem(@Nullable VehicleRentalSystem system) {
      this.system = system;
      return this;
    }

    public Builder withVehicleType(@Nullable RentalVehicleType vehicleType) {
      this.vehicleType = vehicleType;
      return this;
    }

    public Builder withRentalUris(@Nullable VehicleRentalStationUris rentalUris) {
      this.rentalUris = rentalUris;
      return this;
    }

    public Builder withIsReserved(boolean isReserved) {
      this.isReserved = isReserved;
      return this;
    }

    public Builder withIsDisabled(boolean isDisabled) {
      this.isDisabled = isDisabled;
      return this;
    }

    public Builder withLastReported(@Nullable Instant lastReported) {
      this.lastReported = lastReported;
      return this;
    }

    public Builder withStation(@Nullable VehicleRentalStation station) {
      this.station = station;
      return this;
    }

    public Builder withPricingPlanId(@Nullable String pricingPlanId) {
      this.pricingPlanId = pricingPlanId;
      return this;
    }

    public Builder withFuel(@Nullable RentalVehicleFuel fuel) {
      this.fuel = fuel;
      return this;
    }

    public Builder withAvailableUntil(@Nullable OffsetDateTime availableUntil) {
      this.availableUntil = availableUntil;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public VehicleRentalVehicle build() {
      var value = new VehicleRentalVehicle(this);
      return original.equals(value) ? original : value;
    }
  }
}
