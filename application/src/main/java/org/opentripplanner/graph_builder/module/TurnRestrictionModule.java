package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.SubsidiaryOsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnRestrictionModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TurnRestrictionModule.class);

  final Graph graph;

  public TurnRestrictionModule(Graph graph) {
    this.graph = graph;
  }

  boolean isCorrespondingEdge(StreetEdge a, StreetEdge b) {
    if (a == b) return true;
    Vertex aTo = a.getToVertex();
    Vertex bTo = b.getToVertex();
    if (aTo == bTo) return true;
    if (aTo.getLat() == bTo.getLat() && aTo.getLon() == bTo.getLon()) return true;
    return false;
  }

  void processRestriction(TurnRestriction turnRestriction) {
    var fromEdge = turnRestriction.from;
    var vertex = fromEdge.getToVertex();
    var toEdge = turnRestriction.to;
    if (vertex instanceof OsmVertex osmVertex) {
      var splitVertex = new SubsidiaryOsmVertex(osmVertex);
      graph.addVertex(splitVertex);
      fromEdge.toBuilder().withToVertex(splitVertex).buildAndConnect();
      for (Edge edge : vertex.getOutgoing()) {
        if (edge instanceof StreetEdge streetEdge) {
          if (!isCorrespondingEdge(streetEdge, toEdge)) {
            streetEdge.toBuilder().withFromVertex(splitVertex);
          }
        }
      }
    } else {
      throw new IllegalStateException(String.format("Vertex %s is not an OsmVertex", vertex));
    }
  }

  @Override
  public void buildGraph() {
    LOG.info("Applying turn restrictions to graph");

    int turnRestrictionCount = 0;
    for (var streetEdge : graph.getEdgesOfType(StreetEdge.class)) {
      for (var turnRestriction : streetEdge.getTurnRestrictions()) {
        processRestriction(turnRestriction);
        turnRestrictionCount++;
      }
    }
    LOG.info("Applied {} turn restrictions", turnRestrictionCount);
  }
}
