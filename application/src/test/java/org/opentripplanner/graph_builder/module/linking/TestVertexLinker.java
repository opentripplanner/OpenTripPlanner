package org.opentripplanner.graph_builder.module.linking;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.linking.VisibilityMode;
import org.opentripplanner.street.model.StreetConstants;

public class TestVertexLinker {

  public static VertexLinker of(Graph graph) {
    return new VertexLinker(
      graph,
      VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES
    );
  }
}
