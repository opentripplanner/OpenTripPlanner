package org.opentripplanner.graph_builder.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
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
  final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  final Map<Vertex, Set<SubsidiaryVertex>> subsidiaryVertices;
  final Map<Vertex, IntersectionVertex> mainVertices;
  int addedVertices;
  int addedEdges;

  public TurnRestrictionModule(
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository
  ) {
    this.graph = graph;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.subsidiaryVertices = new HashMap<>();
    this.mainVertices = new HashMap<>();
    initializeMainAndSubsidiaryVertices();
  }

  void initializeMainAndSubsidiaryVertices() {
    for (var vertex : graph.getVerticesOfType(SubsidiaryVertex.class)) {
      Vertex parent = vertex.getParent();
      if (parent instanceof IntersectionVertex intersectionVertex) {
        mainVertices.put(vertex, intersectionVertex);
        subsidiaryVertices.computeIfAbsent(parent, k -> new HashSet<>()).add(vertex);
      }
    }
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

  List<StreetEdge> getFromCorrespondingEdges(
    StreetEdge edge,
    IntersectionVertex intersectionVertex
  ) {
    List<StreetEdge> edges = new ArrayList<>();
    for (var e : intersectionVertex.getIncomingStreetEdges()) {
      if (isCorrespondingVertex(e.getFromVertex(), edge.getFromVertex())) {
        edges.add(e);
      }
    }
    return edges;
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
    var fromEdges = getFromCorrespondingEdges(turnRestriction.from, vertex);
    if (fromEdges.isEmpty()) {
      return;
    }
    var mainVertex = (IntersectionVertex) turnRestriction.from.getToVertex();
    var splitVertex = new SubsidiaryVertex(mainVertex);
    graph.addVertex(splitVertex);
    subsidiaryVertices.get(mainVertex).add(splitVertex);
    mainVertices.put(splitVertex, mainVertex);
    addedVertices++;
    boolean hasRemovedInputEdges = false;
    for (var fromEdge : fromEdges) {
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
        hasRemovedInputEdges = true;
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
    if (hasRemovedInputEdges) {
      if (vertex.getIncoming().isEmpty()) {
        for (var toEdge : vertex.getOutgoing()) {
          toEdge.remove();
          addedEdges--;
        }
        graph.remove(vertex);
        addedVertices--;
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
    for (var turnRestriction : osmInfoGraphBuildRepository.listTurnRestrictions()) {
      processRestriction(turnRestriction);
      turnRestrictionCount++;
    }
    LOG.info(
      "Applied {} turn restrictions, added {} vertices and {} edges",
      turnRestrictionCount,
      addedVertices,
      addedEdges
    );
  }
}
