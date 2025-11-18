package org.opentripplanner.routing.linking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.linking.EdgeDisposable;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This class is used to keep track of temporary edges added to the graph, so that they can be
 * removed from the graph when no longer needed.
 */
public class DisposableEdgeCollection implements EdgeDisposable {

  private final Graph graph;

  private final Scope scope;

  private final Set<Edge> edges = new HashSet<>();

  public DisposableEdgeCollection(Graph graph) {
    this(graph, null);
  }

  public DisposableEdgeCollection(Graph graph, Scope scope) {
    this.graph = graph;
    this.scope = scope;
  }

  public void addEdge(Edge edge) {
    this.edges.add(edge);
  }

  /**
   * This collection is empty and there is no need to dispose edges.
   */
  public boolean isEmpty() {
    return edges.isEmpty();
  }

  /**
   * Removes all the edges in this collection from the graph.
   */
  public void disposeEdges() {
    if (scope == Scope.REALTIME) {
      for (Edge e : edges) {
        graph.removeEdge(e, scope);
      }
    }
    Collection<Vertex> vertices = new ArrayList<>();
    for (Edge e : edges) {
      vertices.add(e.getFromVertex());
      vertices.add(e.getToVertex());
      graph.removeEdge(e);
    }
    for (Vertex v : vertices) {
      graph.removeIfUnconnected(v);
    }
    edges.clear();
  }
}
