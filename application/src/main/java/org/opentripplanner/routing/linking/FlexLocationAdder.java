package org.opentripplanner.routing.linking;

import java.util.Set;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.SiteRepository;

class FlexLocationAdder {

  static void addFlexLocations(StreetEdge edge, Vertex v0, SiteRepository siteRepository) {
    if (edge.getPermission().allows(StreetTraversalPermission.PEDESTRIAN_AND_CAR)) {
      Point p = GeometryUtils.getGeometryFactory().createPoint(v0.getCoordinate());
      Envelope env = p.getEnvelopeInternal();
      for (AreaStop areaStop : siteRepository.findAreaStops(env)) {
        if (!areaStop.getGeometry().disjoint(p)) {
          v0.addAreaStops(Set.of(areaStop));
        }
      }
    }
  }
}
