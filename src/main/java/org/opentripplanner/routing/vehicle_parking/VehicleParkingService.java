package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class VehicleParkingService implements Serializable {
  private static final long serialVersionUID = 1L;

  private final Set<VehicleParking> vehicleParkings = new HashSet<>();

  public void addVehicleParking(VehicleParking vehicleParking) {
    vehicleParkings.add(vehicleParking);
  }

  public void removeVehicleParking(VehicleParking vehicleParking) {
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
}
