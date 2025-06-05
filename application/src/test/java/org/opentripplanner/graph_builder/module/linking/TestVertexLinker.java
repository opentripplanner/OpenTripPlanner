package org.opentripplanner.graph_builder.module.linking;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetConstants;

public class TestVertexLinker {

  public static VertexLinker of(Graph graph) {
    return new VertexLinker(
      graph,
      VertexLinker.VisibilityMode.COMPUTE_AREA_VISIBILITY,
      StreetConstants.DEFAULT_MAX_AREA_NODES
    );
  }
}
