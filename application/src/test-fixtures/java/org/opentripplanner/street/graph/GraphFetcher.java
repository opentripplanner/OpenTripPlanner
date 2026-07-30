package org.opentripplanner.street.graph;

import java.util.stream.StreamSupport;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.street.model.vertex.OsmVertex;

public class GraphFetcher {

  private final Graph graph;

  public GraphFetcher(Graph graph) {
    this.graph = graph;
  }

  public OsmVertex getVertexForOsmNode(OsmNode node) {
    var vertices = graph.getVerticesOfType(OsmVertex.class);
    return StreamSupport.stream(vertices.spliterator(), false)
      .filter(v -> v.nodeId() == node.getId())
      .findFirst()
      .orElseThrow();
  }

  public Graph graph() {
    return graph;
  }
}
