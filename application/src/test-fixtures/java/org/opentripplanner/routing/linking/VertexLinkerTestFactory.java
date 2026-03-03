package org.opentripplanner.routing.linking;

import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetConstants;

public class VertexLinkerTestFactory {

  public static org.opentripplanner.street.linking.VertexLinker of(Graph graph) {
    return new org.opentripplanner.street.linking.VertexLinker(
      graph,
      org.opentripplanner.street.linking.VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      true
    );
  }
}
