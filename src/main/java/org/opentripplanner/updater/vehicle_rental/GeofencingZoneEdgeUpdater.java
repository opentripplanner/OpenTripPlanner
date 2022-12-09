package org.opentripplanner.updater.vehicle_rental;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.BusinessAreaBorder;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.GeofencingZoneExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GeofencingZoneEdgeUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(GeofencingZoneEdgeUpdater.class);
  private final Function<Envelope, Collection<Edge>> getEdgesForEnvelope;

  public GeofencingZoneEdgeUpdater(Function<Envelope, Collection<Edge>> getEdgesForEnvelope) {
    this.getEdgesForEnvelope = getEdgesForEnvelope;
  }

  Set<StreetEdge> applyGeofencingZones(List<GeofencingZone> geofencingZones) {
    LOG.info("Computing geofencing zones");
    var start = System.currentTimeMillis();

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

    var end = System.currentTimeMillis();
    var millis = Duration.ofMillis(end - start);
    var updatedEdges = new HashSet<>(businessAreaBorders);
    updatedEdges.addAll(restrictedEdges);

    LOG.info(
      "Geofencing zones computation took {}. Added extension to {} edges.",
      TimeUtils.durationToStrCompact(millis),
      updatedEdges.size()
    );
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
