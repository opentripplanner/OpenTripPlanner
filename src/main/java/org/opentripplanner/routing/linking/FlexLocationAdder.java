package org.opentripplanner.routing.linking;

import java.util.HashSet;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.StopModel;

class FlexLocationAdder {

  static void addFlexLocations(StreetEdge edge, SplitterVertex v0, StopModel stopModel) {
    if (edge.getPermission().allows(StreetTraversalPermission.PEDESTRIAN_AND_CAR)) {
      Point p = GeometryUtils.getGeometryFactory().createPoint(v0.getCoordinate());
      Envelope env = p.getEnvelopeInternal();
      for (AreaStop location : stopModel.queryLocationIndex(env)) {
        if (!location.getGeometry().disjoint(p)) {
          if (v0.areaStops == null) {
            v0.areaStops = new HashSet<>();
          }
          v0.areaStops.add(location);
        }
      }
    }
  }
}
