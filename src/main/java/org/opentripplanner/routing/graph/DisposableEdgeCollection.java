package org.opentripplanner.routing.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to keep track of temporary edges added to the graph, so that they can be
 * removed from the graph when no longer needed.
 */
public class DisposableEdgeCollection {

  private final Graph graph;

  private final Set<Edge> edges = new HashSet<>();

  public DisposableEdgeCollection(Graph graph) {
    this.graph = graph;
  }

  public void addEdge(Edge edge) {
    this.edges.add(edge);
  }

  /**
   * Removes all the edges in this collection from the graph.
   */
  public void disposeEdges() {
    Collection<Vertex> vertices = new ArrayList<>();
    for (Edge e : edges) {
      vertices.add(e.fromv);
      vertices.add(e.tov);
      graph.removeEdge(e);
    }
    for (Vertex v : vertices) {
      graph.removeIfUnconnected(v);
    }
    edges.clear();
  }
}
