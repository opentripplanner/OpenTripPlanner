package org.opentripplanner.updater.vehicle_rental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.BusinessAreaBorder;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.GeofencingZoneExtension;

class GeofencingZoneEdgeUpdater {

  private final Function<Envelope, Collection<Edge>> getEdgesForEnvelope;

  public GeofencingZoneEdgeUpdater(Function<Envelope, Collection<Edge>> getEdgesForEnvelope) {
    this.getEdgesForEnvelope = getEdgesForEnvelope;
  }

  Set<StreetEdge> applyGeofencingZones(List<GeofencingZone> geofencingZones) {

    var restrictedZones = geofencingZones.stream().filter(GeofencingZone::hasRestriction).toList();

    var restrictedEdges = addExtensionToIntersectingStreetEdges(
      restrictedZones,
      GeofencingZone::geometry,
      GeofencingZoneExtension::new
    );

    var generalBusinessAreas = geofencingZones
      .stream()
      .filter(GeofencingZone::isBusinessArea)
      .toList();

    var businessAreaBorders = addExtensionToIntersectingStreetEdges(
      generalBusinessAreas,
      zone -> zone.geometry().getBoundary(),
      zone -> new BusinessAreaBorder(zone.id().getFeedId())
    );

    var updatedEdges = new HashSet<>(businessAreaBorders);
    updatedEdges.addAll(restrictedEdges);

    return updatedEdges;
  }

  private Collection<StreetEdge> addExtensionToIntersectingStreetEdges(
    List<GeofencingZone> restrictedZones,
    Function<GeofencingZone, Geometry> extractGeometry,
    Function<GeofencingZone, StreetEdgeRentalExtension> createExtension
  ) {
    var edgesUpdated = new ArrayList<StreetEdge>();
    for (GeofencingZone zone : restrictedZones) {
      var geom = extractGeometry.apply(zone);
      var candidates = getEdgesForEnvelope.apply(geom.getEnvelopeInternal());
      for (var e : candidates) {
        if (e instanceof StreetEdge streetEdge && streetEdge.getGeometry().intersects(geom)) {
          var ext = createExtension.apply(zone);
          streetEdge.addRentalExtension(ext);
          edgesUpdated.add(streetEdge);
        }
      }
    }
    return edgesUpdated;
  }
}
