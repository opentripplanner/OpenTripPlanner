package org.opentripplanner.service.vehiclerental.street;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * Spatial index for efficient geofencing zone containment queries.
 * Used at vehicle pickup to determine which zones the vehicle starts in,
 * enabling boundary-only geofencing where restrictions are tracked in routing state
 * rather than applied to all interior edges.
 * <p>
 * Uses a two-level optimization for fast containment tests:
 * <ol>
 *   <li>Inner envelope check: A rectangle guaranteed to be fully inside the zone.
 *       Points inside this envelope are definitely inside - no further check needed.</li>
 *   <li>PreparedGeometry check: For points outside the inner envelope but inside
 *       the bounding box, use JTS PreparedGeometry for fast polygon containment.</li>
 * </ol>
 */
public class GeofencingZoneIndex implements Serializable {

  private static final GeometryFactory GF = new GeometryFactory();
  private static final PreparedGeometryFactory PGF = new PreparedGeometryFactory();
  private final STRtree spatialIndex;
  private final Map<GeofencingZone, PreparedGeometry> preparedGeometries;
  private final Map<GeofencingZone, Envelope> innerEnvelopes;

  public GeofencingZoneIndex(Collection<GeofencingZone> zones) {
    this.spatialIndex = new STRtree();
    this.preparedGeometries = new HashMap<>();
    this.innerEnvelopes = new HashMap<>();
    for (var zone : zones) {
      spatialIndex.insert(zone.geometry().getEnvelopeInternal(), zone);
      preparedGeometries.put(zone, PGF.create(zone.geometry()));
      innerEnvelopes.put(zone, computeInnerEnvelope(zone.geometry()));
    }
    spatialIndex.build();
  }

  /**
   * Compute an inner envelope (rectangle) that is guaranteed to be fully inside the geometry.
   * Uses the interior point and its distance to the boundary to create a square that
   * any point inside is definitely within the polygon.
   */
  private static Envelope computeInnerEnvelope(Geometry geometry) {
    Point interiorPoint = geometry.getInteriorPoint();
    double distanceToBoundary = interiorPoint.distance(geometry.getBoundary());

    if (distanceToBoundary <= 0) {
      return new Envelope();
    }

    double x = interiorPoint.getX();
    double y = interiorPoint.getY();
    return new Envelope(
      x - distanceToBoundary,
      x + distanceToBoundary,
      y - distanceToBoundary,
      y + distanceToBoundary
    );
  }

  /**
   * Check if a point is contained in a zone using the two-level optimization.
   */
  private boolean containsPoint(GeofencingZone zone, Point point) {
    Envelope innerEnvelope = innerEnvelopes.get(zone);
    if (innerEnvelope.contains(point.getCoordinate())) {
      return true;
    }
    return preparedGeometries.get(zone).contains(point);
  }

  /**
   * Find all zones containing the given point.
   * Called at vehicle pickup to initialize routing state with the zones
   * the vehicle is currently inside.
   *
   * @param point The coordinate to check for zone containment
   * @return Set of GeofencingZone objects that contain the point
   */
  public Set<GeofencingZone> getZonesContaining(Coordinate point) {
    var pointGeom = GF.createPoint(point);
    @SuppressWarnings("unchecked")
    var candidates = (List<GeofencingZone>) spatialIndex.query(new Envelope(point));
    return candidates
      .stream()
      .filter(zone -> containsPoint(zone, pointGeom))
      .collect(Collectors.toSet());
  }

  /**
   * Find all zones containing the given point that have restrictions
   * (either drop-off banned or traversal banned).
   *
   * @param point The coordinate to check for zone containment
   * @return Set of GeofencingZone objects with restrictions that contain the point
   */
  public Set<GeofencingZone> getRestrictedZonesContaining(Coordinate point) {
    return getZonesContaining(point)
      .stream()
      .filter(GeofencingZone::hasRestriction)
      .collect(Collectors.toSet());
  }

  /**
   * Check if the index is empty (no zones indexed).
   */
  public boolean isEmpty() {
    return spatialIndex.isEmpty();
  }

  /**
   * Get the total number of zones in the index.
   */
  public int size() {
    return spatialIndex.size();
  }
}
