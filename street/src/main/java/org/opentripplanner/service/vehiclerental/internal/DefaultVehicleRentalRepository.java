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
 * Default {@link VehicleRentalRepository}. Owns the rental places and the geofencing zone
 * indices, and answers geofencing zone queries via {@link GeofencingZoneService}.
 *
 * <p>The spatial indices are {@code transient} — JTS {@code STRtree} / {@code PreparedGeometry}
 * caches don't survive Kryo — so the raw zones are kept and the indices are rebuilt lazily on
 * first access after deserialization.
 *
 * <p>Both maps are keyed by network. A network has exactly one source of zones, so a later
 * registration replaces an earlier one rather than adding a second index alongside it.
 */
@Singleton
public class DefaultVehicleRentalRepository implements VehicleRentalRepository {

  private final Map<FeedScopedId, VehicleRentalPlace> rentalPlaces = new ConcurrentHashMap<>();

  /** Raw zones, by network. Only these survive serialization; the indices are rebuilt from them. */
  private final Map<String, Set<GeofencingZone>> zonesByNetwork = new ConcurrentHashMap<>();

  /** Rebuilt lazily from {@link #zonesByNetwork} via {@link #indexes()} after deserialization. */
  private transient volatile Map<String, GeofencingZoneIndex> geofencingZoneIndexes;

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
  public void setGeofencingZoneIndex(
    String network,
    GeofencingZoneIndex index,
    Collection<GeofencingZone> zones
  ) {
    indexes().put(network, index);
    zonesByNetwork.put(network, Set.copyOf(zones));
  }

  @Override
  public Collection<VehicleRentalPlace> listRentalPlaces() {
    return rentalPlaces.values();
  }

  @Override
  public Collection<String> listZoneNetworks() {
    return Set.copyOf(indexes().keySet());
  }

  @Override
  public VehicleRentalPlace getRentalPlace(FeedScopedId id) {
    return rentalPlaces.get(id);
  }

  @Override
  public Set<GeofencingZone> findZonesContaining(Coordinate coord) {
    return indexes()
      .values()
      .stream()
      .flatMap(idx -> idx.findZonesContaining(coord).stream())
      .collect(Collectors.toSet());
  }

  @Override
  public Set<GeofencingZone> findZonesContaining(Coordinate coord, String network) {
    var index = indexes().get(network);
    return index == null ? Set.of() : index.findZonesContaining(coord);
  }

  @Override
  public boolean hasIndexedZones() {
    return !indexes().isEmpty();
  }

  @Override
  public Set<GeofencingZone> listZones() {
    var zones = new HashSet<GeofencingZone>();
    for (var idx : indexes().values()) {
      zones.addAll(idx.listZones());
    }
    return zones;
  }

  private Map<String, GeofencingZoneIndex> indexes() {
    var indexes = this.geofencingZoneIndexes;
    if (indexes != null) {
      return indexes;
    }
    synchronized (this) {
      if (geofencingZoneIndexes == null) {
        var rebuilt = new ConcurrentHashMap<String, GeofencingZoneIndex>();
        zonesByNetwork.forEach((name, zones) -> rebuilt.put(name, new GeofencingZoneIndex(zones)));
        geofencingZoneIndexes = rebuilt;
      }
      return geofencingZoneIndexes;
    }
  }
}
