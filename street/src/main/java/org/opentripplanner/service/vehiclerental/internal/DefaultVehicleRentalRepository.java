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
import org.opentripplanner.street.graph.Graph;

/**
 * Default {@link VehicleRentalRepository}. Owns the rental places and the geofencing zone
 * indices, and answers geofencing zone queries via {@link GeofencingZoneService}.
 *
 * <p>Lives only in the serve phase. Zones applied during the graph build are carried on the
 * {@link org.opentripplanner.street.graph.Graph} and indexed here at construction; zones applied
 * by an updater are indexed as they are registered.
 *
 * <p>Indices are keyed by network. A network has exactly one source of zones, so a later
 * registration replaces an earlier one rather than adding a second index alongside it.
 */
@Singleton
public class DefaultVehicleRentalRepository implements VehicleRentalRepository {

  private final Map<FeedScopedId, VehicleRentalPlace> rentalPlaces = new ConcurrentHashMap<>();

  private final Map<String, GeofencingZoneIndex> geofencingZoneIndexes = new ConcurrentHashMap<>();

  /**
   * Seeds the repository with the zones applied during the graph build, which the
   * {@link Graph} carries out of the build phase, and indexes them.
   */
  @Inject
  public DefaultVehicleRentalRepository(Graph graph) {
    this(graph.vehicleRentalGeofencingZones());
  }

  public DefaultVehicleRentalRepository(Map<String, Set<GeofencingZone>> zonesByNetwork) {
    zonesByNetwork.forEach(this::setGeofencingZones);
  }

  /** A repository with no zones, for tests and for a graph built without them. */
  public DefaultVehicleRentalRepository() {
    this(Map.of());
  }

  @Override
  public void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation) {
    rentalPlaces.put(vehicleRentalStation.id(), vehicleRentalStation);
  }

  @Override
  public void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId) {
    rentalPlaces.remove(vehicleRentalStationId);
  }

  @Override
  public void setGeofencingZones(String network, Collection<GeofencingZone> zones) {
    geofencingZoneIndexes.put(network, new GeofencingZoneIndex(zones));
  }

  @Override
  public Collection<VehicleRentalPlace> listRentalPlaces() {
    return rentalPlaces.values();
  }

  @Override
  public Collection<String> listZoneNetworks() {
    return Set.copyOf(geofencingZoneIndexes.keySet());
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
  public Set<GeofencingZone> findZonesContaining(Coordinate coord, String network) {
    var index = geofencingZoneIndexes.get(network);
    return index == null ? Set.of() : index.findZonesContaining(coord);
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
