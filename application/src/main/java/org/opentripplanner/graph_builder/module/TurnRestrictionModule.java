package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SubsidiaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseModeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnRestrictionModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TurnRestrictionModule.class);

  final Graph graph;
  final Map<Vertex, Set<SubsidiaryVertex>> subsidiaryVertices;
  final Map<Vertex, IntersectionVertex> mainVertices;

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

  boolean isCorrespondingVertex(Vertex a, Vertex b) {
    if (a == b) return true;
    return getMainVertex(a) == getMainVertex(b);
  }

  boolean isCorrespondingEdge(StreetEdge a, StreetEdge b) {
    if (a == b) return true;
    Vertex aTo = getMainVertex(a.getToVertex());
    Vertex bTo = getMainVertex(b.getToVertex());
    Vertex aFrom = getMainVertex(a.getFromVertex());
    Vertex bFrom = getMainVertex(b.getFromVertex());
    return aTo == bTo || aFrom == bFrom;
  }

  StreetEdge getFromCorrespondingEdge(StreetEdge edge, Collection<StreetEdge> edges) {
    for (var e : edges) {
      if (isCorrespondingVertex(e.getFromVertex(), edge.getFromVertex())) {
        return e;
      }
    }
    throw new IllegalStateException(
      String.format("corresponding edge for %s not found in %s", edge, edges)
    );
  }

  StreetTraversalPermission streetTraversalPermission(TraverseModeSet traverseModeSet) {
    var permission = StreetTraversalPermission.NONE;
    if (traverseModeSet.getBicycle()) {
      permission = permission.add(StreetTraversalPermission.BICYCLE);
    }
    if (traverseModeSet.getWalk()) {
      permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
    }
    if (traverseModeSet.getCar()) {
      permission = permission.add(StreetTraversalPermission.CAR);
    }
    return permission;
  }

  void processVertex(IntersectionVertex vertex, TurnRestriction turnRestriction) {
    var mainVertex = (IntersectionVertex) turnRestriction.from.getToVertex();
    var splitVertex = new SubsidiaryVertex(mainVertex);
    graph.addVertex(splitVertex);
    subsidiaryVertices.get(mainVertex).add(splitVertex);
    mainVertices.put(splitVertex, mainVertex);
    var fromEdge = getFromCorrespondingEdge(turnRestriction.from, vertex.getIncomingStreetEdges());
    var fromPermission = fromEdge.getPermission();
    var restrictionPermission = streetTraversalPermission(turnRestriction.modes);
    var oldPermission = fromPermission.remove(restrictionPermission);
    var newPermission = fromPermission.intersection(restrictionPermission);
    if (newPermission.allowsAnything()) {
      fromEdge
        .toBuilder()
        .withToVertex(splitVertex)
        .withPermission(newPermission)
        .buildAndConnect();
    }
    if (oldPermission.allowsNothing()) {
      fromEdge.remove();
    } else {
      fromEdge.setPermission(oldPermission);
    }
    for (var toEdge : vertex.getOutgoingStreetEdges()) {
      if (!isCorrespondingVertex(turnRestriction.to.getToVertex(), toEdge.getToVertex())) {
        toEdge.toBuilder().withFromVertex(splitVertex).buildAndConnect();
      }
    }
  }

  void processRestriction(TurnRestriction turnRestriction) {
    var vertex = turnRestriction.from.getToVertex();
    if (vertex instanceof IntersectionVertex intersectionVertex) {
      if (subsidiaryVertices.containsKey(vertex)) {
        var vertices = subsidiaryVertices.get(vertex);
        for (var subVertex : vertices.toArray(new SubsidiaryVertex[0])) {
          processVertex(subVertex, turnRestriction);
        }
      } else {
        subsidiaryVertices.put(vertex, new HashSet<>());
      }
      processVertex(intersectionVertex, turnRestriction);
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
