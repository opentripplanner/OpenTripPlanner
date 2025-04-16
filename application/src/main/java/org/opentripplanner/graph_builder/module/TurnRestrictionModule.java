package org.opentripplanner.graph_builder.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.TurnRestrictionType;
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
  int addedVertices;
  int addedEdges;

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

  StreetEdge getFromCorrespondingEdge(StreetEdge edge, IntersectionVertex intersectionVertex) {
    for (var e : intersectionVertex.getIncomingStreetEdges()) {
      if (isCorrespondingVertex(e.getFromVertex(), edge.getFromVertex())) {
        return e;
      }
    }
    // adjacent turn restrictions might mean that there are no longer any corresponding edges
    // left, because the original ones weren't traversable by the modes for which this
    // vertex is accessible
    return null;
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
    var fromEdge = getFromCorrespondingEdge(turnRestriction.from, vertex);
    if (fromEdge == null) {
      return;
    }
    var mainVertex = (IntersectionVertex) turnRestriction.from.getToVertex();
    var splitVertex = new SubsidiaryVertex(mainVertex);
    graph.addVertex(splitVertex);
    subsidiaryVertices.get(mainVertex).add(splitVertex);
    mainVertices.put(splitVertex, mainVertex);
    addedVertices++;
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
      addedEdges++;
    }
    if (oldPermission.allowsNothing()) {
      fromEdge.remove();
      addedEdges--;
    } else {
      fromEdge.setPermission(oldPermission);
    }
    for (var toEdge : vertex.getOutgoingStreetEdges()) {
      if (turnRestriction.type == TurnRestrictionType.NO_TURN) {
        if (!isCorrespondingVertex(turnRestriction.to.getToVertex(), toEdge.getToVertex())) {
          toEdge.toBuilder().withFromVertex(splitVertex).buildAndConnect();
          addedEdges++;
        }
      } else {
        if (isCorrespondingVertex(turnRestriction.to.getToVertex(), toEdge.getToVertex())) {
          toEdge.toBuilder().withFromVertex(splitVertex).buildAndConnect();
          addedEdges++;
        }
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
      throw new IllegalStateException(
        String.format("Vertex %s is not an IntersectionVertex", vertex)
      );
    }
  }

  @Override
  public void buildGraph() {
    LOG.info("Applying turn restrictions to graph");

    int turnRestrictionCount = 0;
    addedVertices = 0;
    addedEdges = 0;
    for (var streetEdge : graph.getEdgesOfType(StreetEdge.class)) {
      for (var turnRestriction : streetEdge.getTurnRestrictions()) {
        processRestriction(turnRestriction);
        turnRestrictionCount++;
      }
    }
    LOG.info("Applied {} turn restrictions, added {} vertices and {} edges", turnRestrictionCount, addedVertices, addedEdges);
  }
}
