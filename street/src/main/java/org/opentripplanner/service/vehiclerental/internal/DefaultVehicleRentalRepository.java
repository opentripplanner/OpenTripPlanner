package org.opentripplanner.service.vehiclerental.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingZoneIndex;

/**
 * Default {@link VehicleRentalRepository}. Owns the rental places and the geofencing zone indices,
 * and answers geofencing zone queries via {@link GeofencingZoneService}.
 * <p>
 * The indices are keyed by network. A network has exactly one source of zones, so a later
 * registration replaces an earlier one rather than adding a second index alongside it.
 */
@Singleton
public class DefaultVehicleRentalRepository implements VehicleRentalRepository {

  private final Map<FeedScopedId, VehicleRentalPlace> rentalPlaces = new ConcurrentHashMap<>();

  private final Map<String, GeofencingZoneIndex> geofencingZoneIndexes = new ConcurrentHashMap<>();

  @Inject
  public DefaultVehicleRentalRepository() {}

  @Override
  public void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation) {
    rentalPlaces.put(vehicleRentalStation.id(), vehicleRentalStation);
  }

  @Override
  public void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId) {
    rentalPlaces.remove(vehicleRentalStationId);
  }

  @Override
  public void setGeofencingZoneIndex(String network, GeofencingZoneIndex index) {
    geofencingZoneIndexes.put(network, index);
  }

  @Override
  public Collection<VehicleRentalPlace> listRentalPlaces() {
    return rentalPlaces.values();
  }

  @Override
  public VehicleRentalPlace getRentalPlace(FeedScopedId id) {
    return rentalPlaces.get(id);
  }

  @Override
  public Set<GeofencingZone> findZonesContaining(Coordinate coord) {
    return geofencingZoneIndexes
      .values()
      .stream()
      .flatMap(idx -> idx.findZonesContaining(coord).stream())
      .collect(Collectors.toSet());
  }

  @Override
  public boolean hasIndexedZones() {
    return !geofencingZoneIndexes.isEmpty();
  }

  @Override
  public Set<GeofencingZone> listZones() {
    var zones = new HashSet<GeofencingZone>();
    for (var idx : geofencingZoneIndexes.values()) {
      zones.addAll(idx.listZones());
    }
    return zones;
  }
}
