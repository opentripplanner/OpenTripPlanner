package org.opentripplanner.updater.vehicle_rental;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.BusinessAreaBorder;
import org.opentripplanner.service.vehiclerental.street.GeofencingZoneExtension;
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
class GeofencingVertexUpdater {

  private final Function<Envelope, Collection<Edge>> getEdgesForEnvelope;

  public GeofencingVertexUpdater(Function<Envelope, Collection<Edge>> getEdgesForEnvelope) {
    this.getEdgesForEnvelope = getEdgesForEnvelope;
  }

  /**
   * Applies the restrictions described in the geofencing zones to eges by adding
   * {@link RentalRestrictionExtension} to them.
   */
  Map<StreetEdge, RentalRestrictionExtension> applyGeofencingZones(
    Collection<GeofencingZone> geofencingZones
  ) {
    var restrictedZones = geofencingZones.stream().filter(GeofencingZone::hasRestriction).toList();

    // these are the edges inside business area where exceptions like "no pass through"
    // or "no drop-off" are added
    var restrictedEdges = addExtensionToIntersectingStreetEdges(
      restrictedZones,
      GeofencingZoneExtension::new
    );

    var updates = new HashMap<>(restrictedEdges);

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

      var updated = applyExtension(
        unionOfBusinessAreas.getBoundary(),
        new BusinessAreaBorder(network)
      );

      updates.putAll(updated);
    }

    return Map.copyOf(updates);
  }

  private Map<StreetEdge, RentalRestrictionExtension> addExtensionToIntersectingStreetEdges(
    List<GeofencingZone> zones,
    Function<GeofencingZone, RentalRestrictionExtension> createExtension
  ) {
    var edgesUpdated = new HashMap<StreetEdge, RentalRestrictionExtension>();
    for (GeofencingZone zone : zones) {
      var geom = zone.geometry();
      var ext = createExtension.apply(zone);
      edgesUpdated.putAll(applyExtension(geom, ext));
    }
    return edgesUpdated;
  }

  private Map<StreetEdge, RentalRestrictionExtension> applyExtension(
    Geometry geom,
    RentalRestrictionExtension ext
  ) {
    var edgesUpdated = new HashMap<StreetEdge, RentalRestrictionExtension>();
    Set<Edge> candidates;
    // for business areas we only care about the borders so we compute the boundary of the
    // (multi) polygon. this can either be a MultiLineString or a LineString
    if (geom instanceof LineString ring) {
      candidates = getEdgesAlongLineStrings(List.of(ring));
    } else if (geom instanceof MultiLineString mls) {
      var lineStrings = GeometryUtils.getLineStrings(mls);
      candidates = getEdgesAlongLineStrings(lineStrings);
    } else {
      candidates = Set.copyOf(getEdgesForEnvelope.apply(geom.getEnvelopeInternal()));
    }

    PreparedGeometry preparedZone = PreparedGeometryFactory.prepare(geom);

    for (var e : candidates) {
      if (e instanceof StreetEdge streetEdge && preparedZone.intersects(streetEdge.getGeometry())) {
        streetEdge.addRentalRestriction(ext);
        edgesUpdated.put(streetEdge, ext);
      }
    }
    return edgesUpdated;
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
