package org.opentripplanner.routing.linking;

import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.model.StreetConstants;

public class VertexLinkerTestFactory {

  public static VertexLinker of(Graph graph) {
    return of(graph, GeofencingZoneService.EMPTY);
  }

  public static VertexLinker of(Graph graph, GeofencingZoneService geofencingZoneService) {
    return new VertexLinker(
      graph,
      geofencingZoneService,
      VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      true
    );
  }
}
