package org.opentripplanner.service.vehiclerental.model;

import java.util.Set;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Represents a place where a rental vehicle can be rented from, or dropped off at. Currently, there
 * are two implementing classes, VehicleRentalStation which represents a physical station, and
 * VehicleRentalVehicle, which represents a free floating vehicle, which is not bound to a station.
 */
public interface VehicleRentalPlace {
  /** Get the id for the place, which is globally unique */
  FeedScopedId id();

  /** Get the system-internal id for the place */
  String stationId();

  /** Get the id of the vehicle rental system */
  String network();

  /** Get the name of the place */
  I18NString name();

  double longitude();

  double latitude();

  /** How many vehicles are currently available for rental at the station */
  int vehiclesAvailable();

  /**
   * How many parking spaces are currently available for dropping off a vehicle at the station, 0
   * for floating vehicles
   */
  int spacesAvailable();

  /** Number of total docking points installed at this station, both available and unavailable. */
  Integer capacity();

  /** Does the place allow dropping off vehicles */
  boolean isAllowDropoff();

  /** Does the place allow overloading (ignore available spaces) */
  boolean overloadingAllowed();

  /** Can a vehicle be rented here */
  boolean isAllowPickup();

  /** Can a vehicle be currently rented here */
  boolean allowPickupNow();

  /** Can a vehicle currently be dropped off here */
  boolean allowDropoffNow();

  /** Is the vehicle to be rented free-floating */
  boolean isFloatingVehicle();

  /** Should the search be continued with CAR mode after renting a vehicle */
  boolean isCarStation();

  /** What form factors are currently available for pick up */
  Set<RentalFormFactor> availablePickupFormFactors(boolean includeRealtimeAvailability);

  /** What form factors are currently available for drop off */
  Set<RentalFormFactor> availableDropoffFormFactors(boolean includeRealtimeAvailability);

  default boolean canDropOffFormFactor(
    RentalFormFactor formFactor,
    boolean includeRealtimeAvailability
  ) {
    return false;
  }

  /** Is it possible to arrive at the destination with a rented bicycle, without dropping it off */
  boolean isArrivingInRentalVehicleAtDestinationAllowed();

  /**
   * Whether this station has real-time data available currently. If no real-time data, users should
   * take bikesAvailable/spacesAvailable with a pinch of salt, as they are always the total capacity
   * divided by two.
   */
  boolean isRealTimeData();

  /** Deep links for this rental station or individual vehicle */
  VehicleRentalStationUris rentalUris();

  /** System information for the vehicle rental provider */
  VehicleRentalSystem vehicleRentalSystem();

  default boolean networkIsNotAllowed(VehicleRentalPreferences preferences) {
    if (
      network() == null &&
      (!preferences.allowedNetworks().isEmpty() || !preferences.bannedNetworks().isEmpty())
    ) {
      return false;
    }

    if (preferences.bannedNetworks().contains(network())) {
      return true;
    }

    if (preferences.allowedNetworks().isEmpty()) {
      return false;
    }

    return !preferences.allowedNetworks().contains(network());
  }
}
