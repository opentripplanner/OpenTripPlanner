package org.opentripplanner.street.graph;

import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.utils.collection.StreamUtils;

public class GraphFetcher {

  private final Graph graph;

  public GraphFetcher(Graph graph) {
    this.graph = graph;
  }

  public OsmVertex getVertexForOsmNode(OsmNode node) {
    var vertices = graph.getVerticesOfType(OsmVertex.class);
    return StreamUtils.ofIterable(vertices)
      .filter(v -> v.nodeId() == node.getId())
      .findFirst()
      .orElseThrow();
  }

  public Graph graph() {
    return graph;
  }
}
