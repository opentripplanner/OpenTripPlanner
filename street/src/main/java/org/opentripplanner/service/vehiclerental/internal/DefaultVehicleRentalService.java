package org.opentripplanner.service.vehiclerental.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.street.model.RentalFormFactor;

/**
 * Default implementation of {@link VehicleRentalService}, delegating to the
 * {@link VehicleRentalRepository} that holds the data.
 */
@Singleton
public class DefaultVehicleRentalService implements VehicleRentalService {

  private final VehicleRentalRepository repository;

  @Inject
  public DefaultVehicleRentalService(VehicleRentalRepository repository) {
    this.repository = repository;
  }

  @Override
  public Collection<VehicleRentalPlace> getVehicleRentalPlaces() {
    return repository.listRentalPlaces();
  }

  @Override
  public VehicleRentalPlace getVehicleRentalPlace(FeedScopedId id) {
    return repository.getRentalPlace(id);
  }

  @Override
  public List<VehicleRentalVehicle> getVehicleRentalVehicles() {
    return repository
      .listRentalPlaces()
      .stream()
      .filter(VehicleRentalVehicle.class::isInstance)
      .map(VehicleRentalVehicle.class::cast)
      .toList();
  }

  @Override
  public VehicleRentalVehicle getVehicleRentalVehicle(FeedScopedId id) {
    VehicleRentalPlace vehicleRentalPlace = repository.getRentalPlace(id);
    return vehicleRentalPlace instanceof VehicleRentalVehicle vehicleRentalVehicle
      ? vehicleRentalVehicle
      : null;
  }

  @Override
  public List<VehicleRentalStation> getVehicleRentalStations() {
    return getVehicleRentalStationsAsStream().toList();
  }

  @Override
  public VehicleRentalStation getVehicleRentalStation(FeedScopedId id) {
    VehicleRentalPlace vehicleRentalPlace = repository.getRentalPlace(id);
    return vehicleRentalPlace instanceof VehicleRentalStation vehicleRentalStation
      ? vehicleRentalStation
      : null;
  }

  @Override
  public boolean hasRentalBikes() {
    return repository
      .listRentalPlaces()
      .stream()
      .anyMatch(place -> {
        if (place instanceof VehicleRentalVehicle vehicle) {
          return vehicle.vehicleType().formFactor() == RentalFormFactor.BICYCLE;
        } else if (place instanceof VehicleRentalStation station) {
          return station
            .vehicleTypesAvailable()
            .keySet()
            .stream()
            .anyMatch(t -> t.formFactor() == RentalFormFactor.BICYCLE);
        } else {
          return false;
        }
      });
  }

  @Override
  public List<String> listNetworks() {
    return Stream.concat(
      repository.listRentalPlaces().stream().map(VehicleRentalPlace::network),
      repository.listZoneNetworks().stream()
    )
      .filter(Objects::nonNull)
      .distinct()
      .sorted()
      .toList();
  }

  @Override
  public List<VehicleRentalStation> getVehicleRentalStationForEnvelope(
    double minLon,
    double minLat,
    double maxLon,
    double maxLat
  ) {
    Envelope envelope = new Envelope(
      new Coordinate(minLon, minLat),
      new Coordinate(maxLon, maxLat)
    );

    return getVehicleRentalStationsAsStream()
      .filter(b -> envelope.contains(new Coordinate(b.longitude(), b.latitude())))
      .toList();
  }

  @Override
  public List<VehicleRentalPlace> getVehicleRentalPlacesForEnvelope(Envelope envelope) {
    return repository
      .listRentalPlaces()
      .stream()
      .filter(vr -> envelope.contains(new Coordinate(vr.longitude(), vr.latitude())))
      .toList();
  }

  private Stream<VehicleRentalStation> getVehicleRentalStationsAsStream() {
    return repository
      .listRentalPlaces()
      .stream()
      .filter(VehicleRentalStation.class::isInstance)
      .map(VehicleRentalStation.class::cast);
  }

  @Override
  public Set<GeofencingZone> findZonesContaining(Coordinate coord) {
    return repository.findZonesContaining(coord);
  }

  @Override
  public boolean hasIndexedZones() {
    return repository.hasIndexedZones();
  }

  @Override
  public Set<GeofencingZone> listZones() {
    return repository.listZones();
  }
}
