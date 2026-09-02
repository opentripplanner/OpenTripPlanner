package org.opentripplanner.service.vehiclerental;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;

/**
 * The writable data store of vehicle rental information. Also exposes raw read access and
 * geofencing zone queries (via {@link GeofencingZoneService}); the higher-level
 * {@link VehicleRentalService} provides typed views on top of this.
 * <p>
 * This lives only in the serve phase. Zones applied during the graph build travel on the
 * {@link org.opentripplanner.street.graph.Graph} and are indexed here when it is created.
 */
public interface VehicleRentalRepository extends GeofencingZoneService {
  void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation);

  void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId);

  /**
   * Register a network's geofencing zones. Called by the
   * vehicle rental updater and, for networks in the permanent scope, by the geofencing graph
   * builder. A network has exactly one source of zones, so re-registering with the same
   * {@code network} replaces the previous registration.
   *
   * <p>Only the index built from the zones is kept.
   */
  void setGeofencingZones(String network, Collection<GeofencingZone> zones);

  Collection<VehicleRentalPlace> listRentalPlaces();

  /**
   * The networks that have registered a geofencing zone index. A network appears here as soon as
   * its zones are applied, even if no rental place has been reported for it yet.
   */
  Collection<String> listZoneNetworks();

  VehicleRentalPlace getRentalPlace(FeedScopedId id);
}
