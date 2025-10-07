package org.opentripplanner.routing.linking;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetConstants;

public class VertexLinkerTestFactory {

  public static VertexLinker of(Graph graph) {
    return new VertexLinker(
      graph,
      VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES
    );
  }
}
