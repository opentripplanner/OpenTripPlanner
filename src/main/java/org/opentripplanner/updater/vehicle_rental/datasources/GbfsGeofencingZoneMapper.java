package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.List;
import java.util.Objects;
import org.entur.gbfs.v2_2.geofencing_zones.GBFSGeofencingZones;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.UnsupportedGeometryException;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbfsGeofencingZoneMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GeofencingZone.class);

  private final String systemId;

  public GbfsGeofencingZoneMapper(String systemId) {
    this.systemId = systemId;
  }

  public List<GeofencingZone> mapRentalVehicleType(GBFSGeofencingZones input) {
    return input
      .getData()
      .getGeofencingZones()
      .getFeatures()
      .stream()
      .map(f -> {
        try {
          var name = f.getProperties().getName();
          var dropOffBanned = !f.getProperties().getRules().get(0).getRideAllowed();
          var passThroughBanned = !f.getProperties().getRules().get(0).getRideThroughAllowed();
          var g = GeometryUtils.convertGeoJsonToJtsGeometry(f.getGeometry());
          return new GeofencingZone(new FeedScopedId(systemId, name), g, dropOffBanned, passThroughBanned);
        } catch (UnsupportedGeometryException e) {
          LOG.error("Could not convert geofencing zone", e);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();
  }
}
