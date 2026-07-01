package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.geometry.GeometryUtils;

/**
 * Spatial index for efficient geofencing zone containment queries. Uses an STRtree for
 * envelope-based candidate filtering, then precise geometry containment checks via
 * PreparedGeometry for performance.
 */
public class GeofencingZoneIndex {

  private final STRtree index;
  private final Map<GeofencingZone, PreparedGeometry> preparedGeometries;

  public GeofencingZoneIndex(Collection<GeofencingZone> zones) {
    this.index = new STRtree();
    this.preparedGeometries = new HashMap<>(zones.size());
    for (GeofencingZone zone : zones) {
      index.insert(zone.geometry().getEnvelopeInternal(), zone);
      preparedGeometries.put(zone, PreparedGeometryFactory.prepare(zone.geometry()));
    }
    index.build();
  }

  /**
   * Returns all zones in this index.
   */
  public Set<GeofencingZone> listZones() {
    return Set.copyOf(preparedGeometries.keySet());
  }

  /**
   * Returns all zones whose geometry covers the given coordinate. Uses {@code covers()} (not
   * {@code contains()}) to include points on the zone boundary, matching the semantics of
   * {@link GeofencingZoneApplier} which uses {@code covers()} for boundary detection.
   */
  @SuppressWarnings("unchecked")
  public Set<GeofencingZone> findZonesContaining(Coordinate coord) {
    var point = GeometryUtils.getGeometryFactory().createPoint(coord);
    List<GeofencingZone> candidates = index.query(new Envelope(coord));
    return candidates
      .stream()
      .filter(z -> preparedGeometries.get(z).covers(point))
      .collect(Collectors.toSet());
  }
}
