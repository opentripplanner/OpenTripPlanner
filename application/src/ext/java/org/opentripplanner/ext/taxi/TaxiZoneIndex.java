package org.opentripplanner.ext.taxi;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.ext.taxi.model.TaxiZone;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Spatial index over car pickup provider zones. Used to look up which provider zone covers a given
 * pickup–dropoff coordinate pair.
 * <p>
 * Also caches a {@link PreparedGeometry} per zone, since prepared geometries are much faster than
 * plain {@link org.locationtech.jts.geom.Geometry} for repeated contains/intersects checks.
 * <p>
 * An {@link IdentityHashMap} is used rather than a plain {@link java.util.HashMap} because
 * {@link TaxiZone#equals}/{@code hashCode} compare full JTS geometries (expensive — iterates all
 * coordinates); zones are always looked up by their exact shared instance here, so reference
 * identity is both correct and cheap.
 *
 * <p>TODO: Multi-provider support. Currently only the first matching zone is used.
 * In the future all matching providers should be available so users can choose.
 */
public class TaxiZoneIndex {

  private final STRtree index = new STRtree();
  private final Map<TaxiZone, PreparedGeometry> preparedGeometries = new IdentityHashMap<>();

  public TaxiZoneIndex(List<TaxiZone> zones) {
    for (TaxiZone zone : zones) {
      index.insert(zone.geometry().getEnvelopeInternal(), zone);
      preparedGeometries.put(zone, PreparedGeometryFactory.prepare(zone.geometry()));
    }
  }

  /**
   * Returns the first zone whose geometry contains both {@code pickup} and
   * {@code dropoff}. Returns an empty optional if no zone covers both endpoints.
   */
  @SuppressWarnings("unchecked")
  public Optional<TaxiZone> findFirstZone(WgsCoordinate pickup, WgsCoordinate dropoff) {
    Envelope envelope = new Envelope(pickup.asJtsCoordinate());
    List<TaxiZone> candidates = index.query(envelope);

    Point pickupPoint = GeometryUtils.getGeometryFactory().createPoint(pickup.asJtsCoordinate());
    Point dropoffPoint = GeometryUtils.getGeometryFactory().createPoint(dropoff.asJtsCoordinate());
    for (TaxiZone zone : candidates) {
      PreparedGeometry preparedGeometry = preparedGeometries.get(zone);
      if (preparedGeometry.contains(pickupPoint) && preparedGeometry.contains(dropoffPoint)) {
        return Optional.of(zone);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns all zones whose geometry contains the given {@code coordinate}.
   * Returns an empty list if no zone covers the coordinate.
   */
  @SuppressWarnings("unchecked")
  public List<TaxiZone> findAllZones(WgsCoordinate coordinate) {
    Envelope envelope = new Envelope(coordinate.asJtsCoordinate());
    List<TaxiZone> candidates = index.query(envelope);

    Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate.asJtsCoordinate());
    List<TaxiZone> result = new ArrayList<>(candidates.size());
    for (TaxiZone zone : candidates) {
      if (preparedGeometries.get(zone).contains(point)) {
        result.add(zone);
      }
    }
    return result;
  }

  /**
   * Returns the cached {@link PreparedGeometry} for {@code zone}.
   */
  public PreparedGeometry getPreparedGeometry(TaxiZone zone) {
    return preparedGeometries.get(zone);
  }
}
