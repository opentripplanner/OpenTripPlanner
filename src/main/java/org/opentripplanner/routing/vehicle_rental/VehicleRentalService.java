package org.opentripplanner.routing.vehicle_rental;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleRentalService implements Serializable {

  private final Map<FeedScopedId, VehicleRentalPlace> rentalPlaces = new HashMap<>();

  public Collection<VehicleRentalPlace> getVehicleRentalPlaces() {
    return rentalPlaces.values();
  }

  public VehicleRentalPlace getVehicleRentalPlace(FeedScopedId id) {
    return rentalPlaces.get(id);
  }

  public List<VehicleRentalVehicle> getVehicleRentalVehicles() {
    return rentalPlaces
      .values()
      .stream()
      .filter(vehicleRentalPlace -> vehicleRentalPlace instanceof VehicleRentalVehicle)
      .map(VehicleRentalVehicle.class::cast)
      .toList();
  }

  public VehicleRentalVehicle getVehicleRentalVehicle(FeedScopedId id) {
    VehicleRentalPlace vehicleRentalPlace = rentalPlaces.get(id);
    return vehicleRentalPlace instanceof VehicleRentalVehicle
      ? (VehicleRentalVehicle) vehicleRentalPlace
      : null;
  }

  public List<VehicleRentalStation> getVehicleRentalStations() {
    return rentalPlaces
      .values()
      .stream()
      .filter(vehicleRentalPlace -> vehicleRentalPlace instanceof VehicleRentalStation)
      .map(VehicleRentalStation.class::cast)
      .toList();
  }

  public VehicleRentalStation getVehicleRentalStation(FeedScopedId id) {
    VehicleRentalPlace vehicleRentalPlace = rentalPlaces.get(id);
    return vehicleRentalPlace instanceof VehicleRentalStation
      ? (VehicleRentalStation) vehicleRentalPlace
      : null;
  }

  public void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation) {
    // Remove old reference first, as adding will be a no-op if already present
    rentalPlaces.remove(vehicleRentalStation.getId());
    rentalPlaces.put(vehicleRentalStation.getId(), vehicleRentalStation);
  }

  public void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId) {
    rentalPlaces.remove(vehicleRentalStationId);
  }

  public boolean hasRentalBikes() {
    return rentalPlaces
      .values()
      .stream()
      .anyMatch(place -> {
        if (place instanceof VehicleRentalVehicle vehicle) {
          return vehicle.vehicleType.formFactor == FormFactor.BICYCLE;
        } else if (place instanceof VehicleRentalStation station) {
          return station.vehicleTypesAvailable
            .keySet()
            .stream()
            .anyMatch(t -> t.formFactor == FormFactor.BICYCLE);
        } else {
          return false;
        }
      });
  }

  /**
   * Gets all the vehicle rental stations inside the envelope. This is currently done by iterating
   * over a set, but we could use a spatial index if the number of vehicle rental stations is high
   * enough for performance to be a concern.
   */
  public List<VehicleRentalPlace> getVehicleRentalStationForEnvelope(
    double minLon,
    double minLat,
    double maxLon,
    double maxLat
  ) {
    Envelope envelope = new Envelope(
      new Coordinate(minLon, minLat),
      new Coordinate(maxLon, maxLat)
    );

    return rentalPlaces
      .values()
      .stream()
      .filter(b -> envelope.contains(new Coordinate(b.getLongitude(), b.getLatitude())))
      .toList();
  }
}
