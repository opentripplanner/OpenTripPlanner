package org.opentripplanner.routing.graph;

import javax.annotation.Nonnull;

/**
 * A model holding the graph object. This allows OTP to read the graph from a file and
 * set it in the application context.
 */
public class GraphModel {

  private Graph graph;

  public GraphModel(@Nonnull Graph graph) {
    this.graph = graph;
  }

  public Graph graph() {
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
  }
}
