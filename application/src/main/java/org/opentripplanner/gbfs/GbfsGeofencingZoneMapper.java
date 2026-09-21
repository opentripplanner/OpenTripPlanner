package org.opentripplanner.gbfs;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.geojson.MultiPolygon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.UnsupportedGeometryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract mapper for GBFS geofencing zones. Subclasses implement version-specific access to GBFS
 * feature properties and rules.
 * <p>
 * Per the GBFS spec, a zone can contain multiple rules. Rules within the same zone are resolved by
 * vehicle type scope: rules with the same {@code vehicle_type_ids} are grouped, and only the first
 * (highest-precedence) rule per group is kept. Each distinct vehicle type scope produces one
 * {@link GeofencingZone}.
 *
 * @param <F> The GBFS feature type (zone)
 * @param <R> The GBFS rule type
 */
public abstract class GbfsGeofencingZoneMapper<F, R> {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsGeofencingZoneMapper.class);

  private final String systemId;

  public GbfsGeofencingZoneMapper(String systemId) {
    this.systemId = systemId;
  }

  protected abstract MultiPolygon featureGeometry(F feature);

  protected abstract @Nullable I18NString featureName(F feature);

  protected abstract List<R> featureRules(F feature);

  protected abstract @Nullable Boolean ruleBansDropOff(R rule);

  protected abstract @Nullable Boolean ruleBansPassThrough(R rule);

  protected abstract @Nullable Boolean ruleBansRideStart(R rule);

  protected abstract @Nullable List<String> ruleVehicleTypeIds(R rule);

  protected abstract @Nullable Integer ruleMaximumSpeedKph(R rule);

  /**
   * Convert a GBFS feature to internal model(s). Rules within the zone are grouped by
   * {@code vehicle_type_ids} scope — the first rule per scope wins (GBFS precedence semantic).
   * Each distinct scope produces one {@link GeofencingZone}.
   *
   * @param feature The GBFS feature (zone)
   * @param zoneIndex Position in GBFS feature array (used as inter-zone priority)
   * @return List of GeofencingZone objects, one per distinct vehicle type scope
   */
  protected List<GeofencingZone> toInternalModel(F feature, int zoneIndex) {
    Geometry geometry;
    try {
      geometry = GeometryUtils.convertGeoJsonToJtsGeometry(featureGeometry(feature));
      TopologyValidationError validation = new IsValidOp(geometry).getValidationError();
      if (validation != null) {
        LOG.warn(
          "Invalid JTS geometry name={}, zoneIndex={} validation={}",
          featureName(feature),
          zoneIndex,
          validation
        );
      }
    } catch (UnsupportedGeometryException e) {
      LOG.error("Could not convert geofencing zone", e);
      return Collections.emptyList();
    }

    var rules = featureRules(feature);
    if (rules == null || rules.isEmpty()) {
      return Collections.emptyList();
    }

    // Group by vehicle type scope, keeping only the first rule per scope (GBFS first-wins).
    // LinkedHashMap preserves insertion order so the output is deterministic.
    var firstRuleByScope = new LinkedHashMap<VehicleTypeScope, R>();
    for (R rule : rules) {
      var ids = ruleVehicleTypeIds(rule);
      var scope = new VehicleTypeScope(ids == null ? null : new LinkedHashSet<>(ids));
      firstRuleByScope.putIfAbsent(scope, rule);
    }

    var name = featureName(feature);
    var baseId = fallbackId(geometry);
    var scopes = new ArrayList<>(firstRuleByScope.entrySet());
    var zones = new ArrayList<GeofencingZone>(scopes.size());

    for (int i = 0; i < scopes.size(); i++) {
      var rule = scopes.get(i).getValue();
      String id = scopes.size() > 1 ? baseId + "-scope" + i : baseId;

      Boolean dropOffBanned = ruleBansDropOff(rule);
      Boolean traversalBanned = ruleBansPassThrough(rule);
      Boolean rideStartBanned = ruleBansRideStart(rule);

      // A zone is a business area when all ride/traversal booleans are permissive
      // (null or false). Fields like maximum_speed_kph, station_parking are orthogonal.
      boolean businessArea =
        !Boolean.TRUE.equals(dropOffBanned) &&
        !Boolean.TRUE.equals(traversalBanned) &&
        !Boolean.TRUE.equals(rideStartBanned);

      zones.add(
        new GeofencingZone(
          new FeedScopedId(systemId, id),
          name,
          geometry,
          dropOffBanned,
          traversalBanned,
          rideStartBanned,
          businessArea,
          ruleVehicleTypeIds(rule),
          ruleMaximumSpeedKph(rule),
          zoneIndex
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

  /**
   * Wrapper for vehicle type ID sets that provides value-based equality for use as a map key. Two
   * scopes are equal if they have the same vehicle type IDs (or both are null = all types).
   */
  private record VehicleTypeScope(@Nullable Set<String> vehicleTypeIds) {}
}
