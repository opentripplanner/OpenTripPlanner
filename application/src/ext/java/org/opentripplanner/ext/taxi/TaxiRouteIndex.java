package org.opentripplanner.ext.taxi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.ext.taxi.model.TaxiRoute;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Spatial index over taxi routes that also caches a {@link PreparedGeometry} per route, since
 * prepared geometries are much faster than plain {@link org.locationtech.jts.geom.Geometry} for
 * repeated contains/intersects checks.
 * <p>
 * TODO: Multi-provider support. Currently only the first matching route is used.
 * In the future all matching providers should be available so users can choose.
 */
public class TaxiRouteIndex {

  private final STRtree index;
  private final Map<TaxiRoute, PreparedGeometry> preparedGeometries;

  private TaxiRouteIndex(STRtree index, Map<TaxiRoute, PreparedGeometry> preparedGeometries) {
    this.index = index;
    this.preparedGeometries = preparedGeometries;
  }

  /**
   * Builds a spatial index over {@code routes}, pre-computing and caching a
   * {@link PreparedGeometry} for each one.
   */
  public static TaxiRouteIndex createAndIndex(List<TaxiRoute> routes) {
    STRtree index = new STRtree();
    Map<TaxiRoute, PreparedGeometry> preparedGeometries = new HashMap<>();
    for (TaxiRoute route : routes) {
      index.insert(route.geometry().getEnvelopeInternal(), route);
      preparedGeometries.put(route, PreparedGeometryFactory.prepare(route.geometry()));
    }
    return new TaxiRouteIndex(index, preparedGeometries);
  }

  /**
   * Returns the first route whose geometry contains both {@code pickup} and
   * {@code dropoff}. Returns an empty optional if no route covers both endpoints.
   */
  @SuppressWarnings("unchecked")
  public Optional<TaxiRoute> findFirstRoute(WgsCoordinate pickup, WgsCoordinate dropoff) {
    Envelope envelope = new Envelope(pickup.asJtsCoordinate());
    List<TaxiRoute> candidates = index.query(envelope);

    Point pickupPoint = GeometryUtils.getGeometryFactory().createPoint(pickup.asJtsCoordinate());
    Point dropoffPoint = GeometryUtils.getGeometryFactory().createPoint(dropoff.asJtsCoordinate());
    for (TaxiRoute route : candidates) {
      PreparedGeometry preparedGeometry = preparedGeometries.get(route);
      if (preparedGeometry.contains(pickupPoint) && preparedGeometry.contains(dropoffPoint)) {
        return Optional.of(route);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns all routes whose geometry contains the given {@code coordinate}.
   * Returns an empty list if no route covers the coordinate.
   */
  @SuppressWarnings("unchecked")
  public List<TaxiRoute> findAllRoutes(WgsCoordinate coordinate) {
    Envelope envelope = new Envelope(coordinate.asJtsCoordinate());
    List<TaxiRoute> candidates = index.query(envelope);

    Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate.asJtsCoordinate());
    List<TaxiRoute> result = new ArrayList<>(candidates.size());
    for (TaxiRoute route : candidates) {
      if (preparedGeometries.get(route).contains(point)) {
        result.add(route);
      }
    }
    return result;
  }

  /**
   * Returns the cached {@link PreparedGeometry} for {@code route}.
   */
  public PreparedGeometry getPreparedGeometry(TaxiRoute route) {
    return preparedGeometries.get(route);
  }
}
