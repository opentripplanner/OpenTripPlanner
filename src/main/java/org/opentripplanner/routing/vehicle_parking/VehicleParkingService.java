package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Service that holds all the {@link VehicleParking} instances and an index for fetching parking
 * locations within a {@link VehicleParkingGroup}.
 */
public class VehicleParkingService implements Serializable {

  private static final long serialVersionUID = 1L;

  private final Set<VehicleParking> vehicleParkings = new HashSet<>();
  private final Map<VehicleParkingGroup, Set<VehicleParking>> vehicleParkingGroups = new ConcurrentHashMap<>();

  /**
   * Adds {@link VehicleParking} to this service and attaches it to an index for
   * {@link VehicleParkingGroup} if the parking is part of a group.
   */
  public void addVehicleParking(VehicleParking vehicleParking) {
    var vehicleParkingGroup = vehicleParking.getVehicleParkingGroup();
    if (vehicleParkingGroup != null) {
      var parkingForGroup = vehicleParkingGroups.get(vehicleParkingGroup);
      if (parkingForGroup != null) {
        parkingForGroup.add(vehicleParking);
      } else {
        HashSet<VehicleParking> newParkingForGroup = new HashSet<>();
        newParkingForGroup.add(vehicleParking);
        vehicleParkingGroups.put(vehicleParkingGroup, newParkingForGroup);
      }
    }
    vehicleParkings.add(vehicleParking);
  }

  /**
   * Removes {@link VehicleParking} to this service and from an index for
   * {@link VehicleParkingGroup} if the parking is part of a group.
   */
  public void removeVehicleParking(VehicleParking vehicleParking) {
    var vehicleParkingGroup = vehicleParking.getVehicleParkingGroup();
    if (vehicleParkingGroup != null) {
      var parkingForGroup = vehicleParkingGroups.get(vehicleParkingGroup);
      if (parkingForGroup != null) {
        if (parkingForGroup.size() == 1) {
          vehicleParkingGroups.remove(vehicleParkingGroup);
        } else {
          parkingForGroup.remove(vehicleParking);
        }
      }
    }
    vehicleParkings.remove(vehicleParking);
  }

  public Stream<VehicleParking> getBikeParks() {
    return vehicleParkings.stream().filter(VehicleParking::hasBicyclePlaces);
  }

  public Stream<VehicleParking> getCarParks() {
    return vehicleParkings.stream().filter(VehicleParking::hasAnyCarPlaces);
  }

  public Stream<VehicleParking> getVehicleParkings() {
    return vehicleParkings.stream();
  }

  public Map<VehicleParkingGroup, Set<VehicleParking>> getVehicleParkingGroups() {
    return vehicleParkingGroups;
  }
}
