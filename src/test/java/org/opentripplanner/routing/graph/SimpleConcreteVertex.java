package org.opentripplanner.routing.graph;

import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Seems to be used only in tests. As far as I know this is not used in normal routing (abyrd).
 */
public class SimpleConcreteVertex extends Vertex {

  public SimpleConcreteVertex(Graph g, String label, double lat, double lon) {
    super(g, label, lon, lat);
  }
}
