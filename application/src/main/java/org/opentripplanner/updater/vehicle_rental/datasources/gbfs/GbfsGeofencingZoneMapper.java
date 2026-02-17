package org.opentripplanner.updater.vehicle_rental.datasources.gbfs;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.geojson.MultiPolygon;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.UnsupportedGeometryException;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract mapper for GBFS geofencing zones. Subclasses implement version-specific
 * access to GBFS feature properties and rules.
 *
 * @param <F> The GBFS feature type (zone)
 * @param <R> The GBFS rule type
 */
public abstract class GbfsGeofencingZoneMapper<F, R> {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsGeofencingZoneMapper.class);

  /**
   * Priority multiplier for zone index. Each zone's rules get priorities in the range
   * [zoneIndex * 1000, zoneIndex * 1000 + 999], allowing up to 1000 rules per zone.
   */
  private static final int ZONE_PRIORITY_MULTIPLIER = 1000;

  private final String systemId;

  public GbfsGeofencingZoneMapper(String systemId) {
    this.systemId = systemId;
  }

  protected abstract MultiPolygon featureGeometry(F feature);

  protected abstract @Nullable I18NString featureName(F feature);

  protected abstract List<R> featureRules(F feature);

  protected abstract boolean ruleBansDropOff(R rule);

  protected abstract boolean ruleBansPassThrough(R rule);

  /**
   * Convert a GBFS feature to internal model(s). Each rule in the feature becomes
   * a separate GeofencingZone with its own priority.
   *
   * @param feature The GBFS feature (zone)
   * @param zoneIndex Position in GBFS feature array (for priority calculation)
   * @return List of GeofencingZone objects, one per rule
   */
  protected List<GeofencingZone> toInternalModel(F feature, int zoneIndex) {
    Geometry geometry;
    try {
      geometry = GeometryUtils.convertGeoJsonToJtsGeometry(featureGeometry(feature));
    } catch (UnsupportedGeometryException e) {
      LOG.error("Could not convert geofencing zone", e);
      return Collections.emptyList();
    }

    var rules = featureRules(feature);
    if (rules == null || rules.isEmpty()) {
      return Collections.emptyList();
    }

    var name = featureName(feature);
    var baseId = fallbackId(geometry);
    var zones = new ArrayList<GeofencingZone>(rules.size());

    for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
      var rule = rules.get(ruleIndex);
      int priority = zoneIndex * ZONE_PRIORITY_MULTIPLIER + ruleIndex;

      // Create unique ID for each rule within a zone
      String id = rules.size() > 1 ? baseId + "-rule" + ruleIndex : baseId;

      zones.add(
        new GeofencingZone(
          new FeedScopedId(systemId, id),
          name,
          geometry,
          ruleBansDropOff(rule),
          ruleBansPassThrough(rule),
          priority
        )
      );
    }

    return zones;
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
