package org.opentripplanner.updater.vehicle_rental.datasources.gbfs;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.geojson.MultiPolygon;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.UnsupportedGeometryException;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GbfsGeofencingZoneMapper<T> {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsGeofencingZoneMapper.class);

  private final String systemId;

  public GbfsGeofencingZoneMapper(String systemId) {
    this.systemId = systemId;
  }

  protected abstract boolean featureBansDropOff(T feature);

  protected abstract boolean featureBansPassThrough(T feature);

  protected abstract MultiPolygon featureGeometry(T feature);

  protected abstract @Nullable I18NString featureName(T feature);

  /**
   * Convert the GBFS type to the internal model.
   */
  @Nullable
  protected GeofencingZone toInternalModel(T feature) {
    Geometry g;
    try {
      g = GeometryUtils.convertGeoJsonToJtsGeometry(featureGeometry(feature));
    } catch (UnsupportedGeometryException e) {
      LOG.error("Could not convert geofencing zone", e);
      return null;
    }

    var id = fallbackId(g);
    return new GeofencingZone(
      new FeedScopedId(systemId, id),
      featureName(feature),
      g,
      featureBansDropOff(feature),
      featureBansPassThrough(feature)
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
