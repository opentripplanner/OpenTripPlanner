package org.opentripplanner.service.vehiclerental.street;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * Even though the data is kept on the vertex this updater operates mostly on edges which then
 * delegate to the vertices.
 * <p>
 * This is because we want to drop the vehicle outside the geofencing zone rather than on the first
 * vertex inside of it. To make this work we need to know which are the edges that cross the
 * border.
 * <p>
 * Perhaps this logic will be replaced with edge splitting where a new vertex is insert right on
 * the border of the zone.
 */
public class GeofencingVertexUpdater {

  private static final GeometryFactory GF = GeometryUtils.getGeometryFactory();
  private final Function<Envelope, Collection<Edge>> getEdgesForEnvelope;

  public GeofencingVertexUpdater(Function<Envelope, Collection<Edge>> getEdgesForEnvelope) {
    this.getEdgesForEnvelope = getEdgesForEnvelope;
  }

  /**
   * Result of applying geofencing zones, containing both the spatial index
   * and the map of modified edges (for cleanup during zone updates).
   */
  public record GeofencingResult(
    GeofencingZoneIndex index,
    Map<StreetEdge, RentalRestrictionExtension> modifiedEdges
  ) {}

  /**
   * Applies the restrictions described in the geofencing zones to edges by adding
   * {@link RentalRestrictionExtension} to them.
   * <p>
   * For zones with restrictions (no drop-off, no traversal), uses boundary-only processing
   * where restrictions are tracked in routing state and only boundary-crossing edges are marked.
   * For business areas (no restrictions), adds traversal ban to boundary edges.
   *
   * @return Result containing a spatial index for zone containment queries and modified edges map
   */
  public GeofencingResult applyGeofencingZones(Collection<GeofencingZone> geofencingZones) {
    var modifiedEdges = new HashMap<StreetEdge, RentalRestrictionExtension>();
    var restrictedZones = geofencingZones.stream().filter(GeofencingZone::hasRestriction).toList();

    // Apply boundary-only restrictions for zones with drop-off/traversal bans
    modifiedEdges.putAll(applyBoundaryRestrictions(restrictedZones));

    var generalBusinessAreas = geofencingZones
      .stream()
      .filter(GeofencingZone::isBusinessArea)
      .toList();

    if (!generalBusinessAreas.isEmpty()) {
      // if the geofencing zones don't have any restrictions then they describe a general business
      // area which you can traverse freely but are not allowed to leave
      // here we just take the boundary of the geometry since we want to add a "no pass through"
      // restriction to any edge intersecting it

      var network = generalBusinessAreas.get(0).id().getFeedId();
      var polygons = generalBusinessAreas
        .stream()
        .map(GeofencingZone::geometry)
        .toArray(Geometry[]::new);

      var unionOfBusinessAreas = GeometryUtils.getGeometryFactory()
        .createGeometryCollection(polygons)
        .union();

      modifiedEdges.putAll(
        applyExtension(unionOfBusinessAreas.getBoundary(), new BusinessAreaBorder(network))
      );
    }

    // Build and return spatial index for pickup zone queries
    var index = new GeofencingZoneIndex(geofencingZones);
    return new GeofencingResult(index, Map.copyOf(modifiedEdges));
  }

  /**
   * Apply boundary-only restrictions for zones with restrictions.
   * Only marks edges that cross zone boundaries with entry/exit information.
   * The actual restrictions are enforced via state-based zone tracking.
   *
   * @return Map of modified edges for cleanup tracking
   */
  private Map<StreetEdge, RentalRestrictionExtension> applyBoundaryRestrictions(
    List<GeofencingZone> zones
  ) {
    var modifiedEdges = new HashMap<StreetEdge, RentalRestrictionExtension>();
    for (GeofencingZone zone : zones) {
      var boundary = zone.geometry().getBoundary();
      var preparedZone = PreparedGeometryFactory.prepare(zone.geometry());
      var preparedBoundary = PreparedGeometryFactory.prepare(boundary);

      Set<Edge> candidates = getBoundaryEdgeCandidates(boundary);

      for (var e : candidates) {
        if (
          e instanceof StreetEdge streetEdge &&
          preparedBoundary.intersects(streetEdge.getGeometry())
        ) {
          // Determine if traversing this edge enters or exits the zone
          // by checking if the "to" vertex is inside the zone
          var toPoint = GF.createPoint(streetEdge.getToVertex().getCoordinate());
          boolean entering = preparedZone.contains(toPoint);

          var boundaryExt = new GeofencingBoundaryExtension(zone, entering);
          streetEdge.addRentalRestrictionToDestination(boundaryExt);
          modifiedEdges.put(streetEdge, boundaryExt);
        }
      }
    }
    return modifiedEdges;
  }

  /**
   * Get candidate edges that might cross a boundary geometry.
   */
  private Set<Edge> getBoundaryEdgeCandidates(Geometry boundary) {
    if (boundary instanceof LineString ring) {
      return getEdgesAlongLineStrings(List.of(ring));
    } else if (boundary instanceof MultiLineString mls) {
      var lineStrings = GeometryUtils.getLineStrings(mls);
      return getEdgesAlongLineStrings(lineStrings);
    } else {
      return Set.copyOf(getEdgesForEnvelope.apply(boundary.getEnvelopeInternal()));
    }
  }

  /**
   * Apply an extension to all edges that intersect a geometry (used for business area borders).
   *
   * @return Map of modified edges for cleanup tracking
   */
  private Map<StreetEdge, RentalRestrictionExtension> applyExtension(
    Geometry geom,
    RentalRestrictionExtension ext
  ) {
    var modifiedEdges = new HashMap<StreetEdge, RentalRestrictionExtension>();
    Set<Edge> candidates = getBoundaryEdgeCandidates(geom);
    PreparedGeometry preparedZone = PreparedGeometryFactory.prepare(geom);

    for (var e : candidates) {
      if (e instanceof StreetEdge streetEdge && preparedZone.intersects(streetEdge.getGeometry())) {
        streetEdge.addRentalRestriction(ext);
        modifiedEdges.put(streetEdge, ext);
      }
    }
    return modifiedEdges;
  }

  /**
   * This method optimizes finding all the candidate edges which could cross the business zone
   * border.
   * <p>
   * If you put the entire zone into an envelope you get lots and lots of edges in the middle of it
   * that are nowhere near the border. Since checking if they intersect with the border is an
   * expensive operation we apply the following optimization:
   * <li>we split the line string into segments for each pair of coordinates
   * <li>for each segment we compute the envelope
   * <li>we only get the edges for that envelope and check if they intersect
   * <p>
   * When finding the edges near the business area border in Oslo this speeds up the computation
   * from ~25 seconds to ~3 seconds (on 2021 hardware).
   */
  private Set<Edge> getEdgesAlongLineStrings(Collection<LineString> lineStrings) {
    return lineStrings
      .stream()
      .flatMap(GeometryUtils::toEnvelopes)
      .map(getEdgesForEnvelope)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }
}
