package org.opentripplanner.service.vehiclerental.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@Singleton
public class DefaultVehicleRentalService implements VehicleRentalService, VehicleRentalRepository {

  @Inject
  public DefaultVehicleRentalService() {}

  private final Map<FeedScopedId, VehicleRentalPlace> rentalPlaces = new HashMap<>();

  @Override
  public Collection<VehicleRentalPlace> getVehicleRentalPlaces() {
    return rentalPlaces.values();
  }

  @Override
  public VehicleRentalPlace getVehicleRentalPlace(FeedScopedId id) {
    return rentalPlaces.get(id);
  }

  @Override
  public List<VehicleRentalVehicle> getVehicleRentalVehicles() {
    return rentalPlaces
      .values()
      .stream()
      .filter(vehicleRentalPlace -> vehicleRentalPlace instanceof VehicleRentalVehicle)
      .map(VehicleRentalVehicle.class::cast)
      .toList();
  }

  @Override
  public VehicleRentalVehicle getVehicleRentalVehicle(FeedScopedId id) {
    VehicleRentalPlace vehicleRentalPlace = rentalPlaces.get(id);
    return vehicleRentalPlace instanceof VehicleRentalVehicle
      ? (VehicleRentalVehicle) vehicleRentalPlace
      : null;
  }

  @Override
  public List<VehicleRentalStation> getVehicleRentalStations() {
    return rentalPlaces
      .values()
      .stream()
      .filter(vehicleRentalPlace -> vehicleRentalPlace instanceof VehicleRentalStation)
      .map(VehicleRentalStation.class::cast)
      .toList();
  }

  @Override
  public VehicleRentalStation getVehicleRentalStation(FeedScopedId id) {
    VehicleRentalPlace vehicleRentalPlace = rentalPlaces.get(id);
    return vehicleRentalPlace instanceof VehicleRentalStation
      ? (VehicleRentalStation) vehicleRentalPlace
      : null;
  }

  @Override
  public void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation) {
    // Remove old reference first, as adding will be a no-op if already present
    rentalPlaces.remove(vehicleRentalStation.getId());
    rentalPlaces.put(vehicleRentalStation.getId(), vehicleRentalStation);
  }

  @Override
  public void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId) {
    rentalPlaces.remove(vehicleRentalStationId);
  }

  @Override
  public boolean hasRentalBikes() {
    return rentalPlaces
      .values()
      .stream()
      .anyMatch(place -> {
        if (place instanceof VehicleRentalVehicle vehicle) {
          return vehicle.vehicleType.formFactor == RentalFormFactor.BICYCLE;
        } else if (place instanceof VehicleRentalStation station) {
          return station.vehicleTypesAvailable
            .keySet()
            .stream()
            .anyMatch(t -> t.formFactor == RentalFormFactor.BICYCLE);
        } else {
          return false;
        }
      });
  }

  @Override
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
