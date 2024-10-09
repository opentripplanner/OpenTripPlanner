package org.opentripplanner.graph_builder.module;

import jakarta.inject.Inject;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check the every vertex and edge in the graph to make sure the edge lists and from/to members are
 * coherent, and that there are no edgeless vertices. Primarily intended for debugging.
 */
public class GraphCoherencyCheckerModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(GraphCoherencyCheckerModule.class);

  private final Graph graph;
  private final DataImportIssueStore issueStore;

  @Inject
  public GraphCoherencyCheckerModule(Graph graph, DataImportIssueStore issueStore) {
    this.graph = graph;
    this.issueStore = issueStore;
  }

  @Override
  public void buildGraph() {
    boolean coherent = true;
    LOG.info("checking graph coherency...");
    for (Vertex v : graph.getVertices()) {
      if (v.getOutgoing().isEmpty() && v.getIncoming().isEmpty()) {
        // This is ok for island transit stops etc. Log so that the type can be checked
        issueStore.add("VertexWithoutEdges", "vertex %s has no edges", v);
      }
      for (Edge e : v.getOutgoing()) {
        if (e.getFromVertex() != v) {
          issueStore.add("InvalidEdge", "outgoing edge of %s: from vertex %s does not match", v, e);
          coherent = false;
        }
      }
      for (Edge e : v.getIncoming()) {
        if (e.getToVertex() != v) {
          issueStore.add("InvalidEdge", "incoming edge of %s: to vertex %s does not match", v, e);
          coherent = false;
        }
      }
    }
    LOG.info("edge lists and from/to members are {}coherent.", coherent ? "" : "not ");
  }
}
