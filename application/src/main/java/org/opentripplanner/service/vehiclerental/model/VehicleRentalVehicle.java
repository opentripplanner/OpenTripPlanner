package org.opentripplanner.service.vehiclerental.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Implements the {@link VehicleRentalPlace} class which contains Javadoc.
 */
public class VehicleRentalVehicle implements VehicleRentalPlace {

  public FeedScopedId id;
  public I18NString name;
  public double longitude;
  public double latitude;

  public VehicleRentalSystem system;
  public RentalVehicleType vehicleType;
  public VehicleRentalStationUris rentalUris;
  public boolean isReserved = false;
  public boolean isDisabled = false;
  public Instant lastReported;
  public VehicleRentalStation station;
  public String pricingPlanId;
  public RentalVehicleFuel fuel;
  public OffsetDateTime availableUntil;

  @Override
  public FeedScopedId getId() {
    return id;
  }

  @Override
  public String getStationId() {
    return getId().getId();
  }

  @Override
  public String getNetwork() {
    return getId().getFeedId();
  }

  @Override
  public I18NString getName() {
    return name;
  }

  @Override
  public double getLongitude() {
    return longitude;
  }

  @Override
  public double getLatitude() {
    return latitude;
  }

  @Override
  public int getVehiclesAvailable() {
    return 1;
  }

  @Override
  public int getSpacesAvailable() {
    return 0;
  }

  @Override
  public Integer getCapacity() {
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
    return vehicleType.formFactor.equals(RentalFormFactor.CAR);
  }

  @Override
  public Set<RentalFormFactor> getAvailablePickupFormFactors(boolean includeRealtimeAvailability) {
    return Set.of(vehicleType.formFactor);
  }

  @Override
  public Set<RentalFormFactor> getAvailableDropoffFormFactors(boolean includeRealtimeAvailability) {
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
  public VehicleRentalStationUris getRentalUris() {
    return rentalUris;
  }

  @Override
  public VehicleRentalSystem getVehicleRentalSystem() {
    return system;
  }

  public RentalVehicleFuel getFuel() {
    return fuel;
  }

  public OffsetDateTime getAvailableUntil() {
    return availableUntil;
  }
}
