package org.opentripplanner.service.vehiclerental.model;

import static java.util.Locale.ROOT;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.SetUtils;

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
  public boolean overloadingAllowed = false;
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
  public boolean overloadingAllowed() {
    return overloadingAllowed;
  }

  @Override
  public boolean isAllowPickup() {
    return isRenting;
  }

  public boolean allowPickupNow() {
    return isRenting && vehiclesAvailable > 0;
  }

  public boolean allowDropoffNow() {
    return isReturning && (spacesAvailable > 0 || overloadingAllowed);
  }

  @Override
  public boolean isFloatingVehicle() {
    return false;
  }

  @Override
  public boolean isCarStation() {
    return Stream.concat(
      vehicleTypesAvailable.keySet().stream(),
      vehicleSpacesAvailable.keySet().stream()
    ).anyMatch(rentalVehicleType -> rentalVehicleType.formFactor.equals(RentalFormFactor.CAR));
  }

  @Override
  public Set<RentalFormFactor> getAvailablePickupFormFactors(boolean includeRealtimeAvailability) {
    return vehicleTypesAvailable
      .entrySet()
      .stream()
      .filter(e -> !includeRealtimeAvailability || e.getValue() > 0)
      .map(e -> e.getKey().formFactor)
      .collect(Collectors.toSet());
  }

  @Override
  public Set<RentalFormFactor> getAvailableDropoffFormFactors(boolean includeRealtimeAvailability) {
    return vehicleSpacesAvailable
      .entrySet()
      .stream()
      .filter(e -> !includeRealtimeAvailability || overloadingAllowed || e.getValue() > 0)
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
  public VehicleRentalSystem getVehicleRentalSystem() {
    return system;
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

  public Set<RentalFormFactor> formFactors() {
    return SetUtils.combine(
      getAvailableDropoffFormFactors(false),
      getAvailablePickupFormFactors(false)
    );
  }

  /**
   * @return Counts of available vehicles by type as well as the total number of available vehicles.
   */
  public RentalVehicleEntityCounts getVehicleTypeCounts() {
    return new RentalVehicleEntityCounts(
      vehiclesAvailable,
      vehicleRentalTypeMapToList(vehicleTypesAvailable)
    );
  }

  /**
   * @return Counts of available vehicle spaces by type as well as the total number of available
   * vehicle spaces.
   */
  public RentalVehicleEntityCounts getVehicleSpaceCounts() {
    return new RentalVehicleEntityCounts(
      spacesAvailable,
      vehicleRentalTypeMapToList(vehicleSpacesAvailable)
    );
  }

  private List<RentalVehicleTypeCount> vehicleRentalTypeMapToList(
    Map<RentalVehicleType, Integer> vehicleTypeMap
  ) {
    return vehicleTypeMap
      .entrySet()
      .stream()
      .map(vtc -> new RentalVehicleTypeCount(vtc.getKey(), vtc.getValue()))
      .toList();
  }
}
