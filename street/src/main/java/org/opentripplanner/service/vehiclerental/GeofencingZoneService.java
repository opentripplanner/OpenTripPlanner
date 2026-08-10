package org.opentripplanner.service.vehiclerental;

import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * Consumer-side abstraction over the registered geofencing zone indices. Defined by the needs of
 * the routing layer (and the debug map layer) so they can query zone membership without depending
 * on the rental service implementation.
 *
 * <p>{@link org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService} is
 * the production implementation; tests can use {@link #EMPTY}.
 */
public interface GeofencingZoneService {
  /**
   * Empty implementation for tests and graph-build phase wiring (before any updater has
   * registered zones).
   */
  GeofencingZoneService EMPTY = new GeofencingZoneService() {
    @Override
    public Set<GeofencingZone> zonesContaining(Coordinate coord) {
      return Set.of();
    }

    @Override
    public boolean hasIndexedZones() {
      return false;
    }

    @Override
    public Set<GeofencingZone> listZones() {
      return Set.of();
    }
  };

  /** All registered zones (across all data sources) that contain the given coordinate. */
  Set<GeofencingZone> zonesContaining(Coordinate coord);

  /**
   * Whether any data source has registered a zone index. Cheap short-circuit for callers that
   * want to avoid per-vertex lookups when no zones exist.
   */
  boolean hasIndexedZones();

  /** All zones across all registered data sources. Used by the debug map layer. */
  Set<GeofencingZone> listZones();
}
