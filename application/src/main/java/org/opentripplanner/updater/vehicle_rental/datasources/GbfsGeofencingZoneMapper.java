package org.opentripplanner.updater.vehicle_rental.datasources;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSFeature;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.UnsupportedGeometryException;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mapper from the raw GBFS type into the internal model of the geofencing zones.
 */
class GbfsGeofencingZoneMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GeofencingZone.class);

  private final String systemId;

  public GbfsGeofencingZoneMapper(String systemId) {
    this.systemId = systemId;
  }

  public List<GeofencingZone> mapGeofencingZone(GBFSGeofencingZones input) {
    return input
      .getData()
      .getGeofencingZones()
      .getFeatures()
      .stream()
      .map(this::toInternalModel)
      .filter(Objects::nonNull)
      .toList();
  }

  /**
   * Convert the GBFS type to the internal model.
   */
  @Nullable
  private GeofencingZone toInternalModel(GBFSFeature f) {
    Geometry g;
    try {
      g = GeometryUtils.convertGeoJsonToJtsGeometry(f.getGeometry());
    } catch (UnsupportedGeometryException e) {
      LOG.error("Could not convert geofencing zone", e);
      return null;
    }
    var name = Objects.requireNonNullElseGet(f.getProperties().getName(), () -> fallbackId(g));
    if (!StringUtils.hasValue(name)) {
      name = fallbackId(g);
    }
    var dropOffBanned = !f.getProperties().getRules().get(0).getRideAllowed();
    var passThroughBanned = !f.getProperties().getRules().get(0).getRideThroughAllowed();
    return new GeofencingZone(
      new FeedScopedId(systemId, name),
      I18NString.of(name),
      g,
      dropOffBanned,
      passThroughBanned
    );
  }

  /**
   * Some zones don't have a name, so we use the hash of the geometry as a fallback.
   */
  private static String fallbackId(Geometry geom) {
    return Hashing.murmur3_32_fixed()
      .hashBytes(geom.toString().getBytes(StandardCharsets.UTF_8))
      .toString();
  }
}
