package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
  final Map<Vertex, Set<SubsidiaryOsmVertex>> subsidiaryVertices;
  final Map<Vertex, OsmVertex> mainVertices;

  public TurnRestrictionModule(Graph graph) {
    this.graph = graph;
    this.subsidiaryVertices = new HashMap<>();
    this.mainVertices = new HashMap<>();
  }

  Vertex getMainVertex(Vertex vertex) {
    if (mainVertices.containsKey(vertex)) {
      return mainVertices.get(vertex);
    } else {
      return vertex;
    }
  }

  boolean isCorrespondingEdge(StreetEdge a, StreetEdge b) {
    if (a == b) return true;
    Vertex aTo = getMainVertex(a.getToVertex());
    Vertex bTo = getMainVertex(b.getToVertex());
    Vertex aFrom = getMainVertex(a.getFromVertex());
    Vertex bFrom = getMainVertex(b.getFromVertex());
    return aTo == bTo || aFrom == bFrom;
  }

  StreetEdge getCorrespondingEdge(StreetEdge edge, Collection<Edge> edges) {
    for (Edge e : edges) {
      if (e instanceof StreetEdge streetEdge) {
        if (isCorrespondingEdge(streetEdge, edge)) {
          return streetEdge;
        }
      }
    }
    throw new IllegalStateException(
      String.format("corresponding edge for %s not found in %s", edge, edges)
    );
  }

  void processVertex(OsmVertex vertex, TurnRestriction turnRestriction) {
    var mainVertex = (OsmVertex) turnRestriction.from.getToVertex();
    var splitVertex = new SubsidiaryOsmVertex(mainVertex);
    graph.addVertex(splitVertex);
    subsidiaryVertices.get(mainVertex).add(splitVertex);
    mainVertices.put(splitVertex, mainVertex);
    var fromEdge = getCorrespondingEdge(turnRestriction.from, vertex.getIncoming());
    var toEdge = getCorrespondingEdge(turnRestriction.to, vertex.getOutgoing());
    fromEdge.toBuilder().withToVertex(splitVertex).buildAndConnect();
    toEdge.toBuilder().withFromVertex(splitVertex).buildAndConnect();
  }

  void processRestriction(TurnRestriction turnRestriction) {
    var vertex = turnRestriction.from.getToVertex();
    if (vertex instanceof OsmVertex osmVertex) {
      if (subsidiaryVertices.containsKey(vertex)) {
        var vertices = subsidiaryVertices.get(vertex);
        for (var subVertex : vertices.toArray(new SubsidiaryOsmVertex[0])) {
          processVertex(subVertex, turnRestriction);
        }
      } else {
        subsidiaryVertices.put(vertex, new HashSet<>());
      }
      processVertex(osmVertex, turnRestriction);
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
