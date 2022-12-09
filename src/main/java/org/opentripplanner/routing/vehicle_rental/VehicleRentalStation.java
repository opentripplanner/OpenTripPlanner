package org.opentripplanner.routing.vehicle_rental;

import static java.util.Locale.ROOT;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.collection.SetUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Implements the {@link VehicleRentalPlace} class which contains Javadoc.
 */
public class VehicleRentalStation implements VehicleRentalPlace {

  // GBFS  Static information
  public FeedScopedId id;
  public I18NString name;
  public String shortName;
  public double longitude;
  public double latitude;
  public String address;
  public String crossStreet;
  public String regionId;
  public String postCode;
  public Set<String> rentalMethods;
  public boolean isVirtualStation = false;
  public Geometry stationArea;
  public Integer capacity;
  public Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity;
  public Map<RentalVehicleType, Integer> vehicleTypeDockCapacity;
  public boolean isValetStation = false;
  public VehicleRentalSystem system;
  public VehicleRentalStationUris rentalUris;

  // GBFS Dynamic information
  public int vehiclesAvailable = 0;
  public int vehiclesDisabled = 0;
  public Map<RentalVehicleType, Integer> vehicleTypesAvailable = Map.of();
  public int spacesAvailable = 0;
  public int spacesDisabled = 0;
  public Map<RentalVehicleType, Integer> vehicleSpacesAvailable = Map.of();

  public boolean isInstalled = true;
  public boolean isRenting = true;
  public boolean isReturning = true;
  public Instant lastReported;

  // OTP internal data
  public boolean allowOverloading = false;
  public boolean isArrivingInRentalVehicleAtDestinationAllowed = false;
  public boolean realTimeData = true;

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
    return vehiclesAvailable;
  }

  @Override
  public int getSpacesAvailable() {
    return spacesAvailable;
  }

  @Override
  public Integer getCapacity() {
    return capacity;
  }

  @Override
  public boolean isAllowDropoff() {
    return isReturning;
  }

  @Override
  public boolean isAllowOverloading() {
    return allowOverloading;
  }

  @Override
  public boolean isAllowPickup() {
    return isRenting;
  }

  public boolean allowPickupNow() {
    return isRenting && vehiclesAvailable > 0;
  }

  public boolean allowDropoffNow() {
    return isReturning && (spacesAvailable > 0 || allowOverloading);
  }

  @Override
  public boolean isFloatingVehicle() {
    return false;
  }

  @Override
  public boolean isCarStation() {
    return Stream
      .concat(vehicleTypesAvailable.keySet().stream(), vehicleSpacesAvailable.keySet().stream())
      .anyMatch(rentalVehicleType ->
        rentalVehicleType.formFactor.equals(RentalVehicleType.FormFactor.CAR)
      );
  }

  @Override
  public Set<FormFactor> getAvailablePickupFormFactors(boolean includeRealtimeAvailability) {
    return vehicleTypesAvailable
      .entrySet()
      .stream()
      .filter(e -> !includeRealtimeAvailability || e.getValue() > 0)
      .map(e -> e.getKey().formFactor)
      .collect(Collectors.toSet());
  }

  @Override
  public Set<FormFactor> getAvailableDropoffFormFactors(boolean includeRealtimeAvailability) {
    return vehicleSpacesAvailable
      .entrySet()
      .stream()
      .filter(e -> !includeRealtimeAvailability || e.getValue() > 0)
      .map(e -> e.getKey().formFactor)
      .collect(Collectors.toSet());
  }

  @Override
  public boolean isArrivingInRentalVehicleAtDestinationAllowed() {
    return isArrivingInRentalVehicleAtDestinationAllowed;
  }

  @Override
  public boolean isRealTimeData() {
    return realTimeData;
  }

  @Override
  public VehicleRentalStationUris getRentalUris() {
    return rentalUris;
  }

  @Override
  public String toString() {
    return String.format(
      ROOT,
      "Vehicle rental station %s at %.6f, %.6f",
      name,
      latitude,
      longitude
    );
  }

  public Set<FormFactor> formFactors() {
    return SetUtils.combine(
      getAvailableDropoffFormFactors(false),
      getAvailablePickupFormFactors(false)
    );
  }
}
