package org.opentripplanner.graph_builder.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SubsidiaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.utils.logging.ProgressTracker;
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
    if (a == b) {
      return true;
    }
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
    var restrictionPermission = streetTraversalPermission(turnRestriction.modes);
    var mainVertex = (IntersectionVertex) turnRestriction.from.getToVertex();
    var splitVertex = new SubsidiaryVertex(mainVertex);
    graph.addVertex(splitVertex);
    subsidiaryVertices.get(mainVertex).add(splitVertex);
    mainVertices.put(splitVertex, mainVertex);
    addedVertices++;
    boolean vertexHasRemovedInputEdges = false;
    for (var fromEdge : fromEdges) {
      var fromPermission = fromEdge.getPermission();
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
        vertexHasRemovedInputEdges = true;
      } else {
        fromEdge.setPermission(oldPermission);
      }
    }
    if (!removeVertexWithoutIncomingEdges(mainVertex, splitVertex)) {
      for (var toEdge : vertex.getOutgoingStreetEdges()) {
        var toPermission = toEdge.getPermission();
        var newPermission = toPermission.intersection(restrictionPermission);
        if (newPermission.allowsAnything()) {
          if (turnRestriction.type == TurnRestrictionType.NO_TURN) {
            if (!isCorrespondingVertex(turnRestriction.to.getToVertex(), toEdge.getToVertex())) {
              toEdge
                .toBuilder()
                .withFromVertex(splitVertex)
                .withPermission(newPermission)
                .buildAndConnect();
              addedEdges++;
            }
          } else {
            if (isCorrespondingVertex(turnRestriction.to.getToVertex(), toEdge.getToVertex())) {
              toEdge
                .toBuilder()
                .withFromVertex(splitVertex)
                .withPermission(newPermission)
                .buildAndConnect();
              addedEdges++;
            }
          }
        }
      }
      if (splitVertex.getOutgoing().isEmpty()) {
        removeVertex(mainVertex, splitVertex);
      }
    }
    if (vertexHasRemovedInputEdges) {
      removeVertexWithoutIncomingEdges(mainVertex, vertex);
    }
  }

  private boolean removeVertexWithoutIncomingEdges(
    IntersectionVertex mainVertex,
    IntersectionVertex vertex
  ) {
    if (!vertex.getIncoming().isEmpty()) {
      return false;
    }
    removeVertex(mainVertex, vertex);
    return true;
  }

  private void removeVertex(IntersectionVertex mainVertex, IntersectionVertex vertex) {
    for (var incomingEdge : vertex.getIncoming()) {
      incomingEdge.remove();
      addedEdges--;
    }
    for (var outgoingEdge : vertex.getOutgoing()) {
      outgoingEdge.remove();
      addedEdges--;
    }
    graph.remove(vertex);
    addedVertices--;
    if (vertex instanceof SubsidiaryVertex) {
      subsidiaryVertices.get(mainVertex).remove(vertex);
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
    var turnRestrictions = osmInfoGraphBuildRepository.listTurnRestrictions();
    var progressTracker = ProgressTracker.track(
      "Applying turn restrictions to graph",
      1_000,
      turnRestrictions.size()
    );
    LOG.info(progressTracker.startMessage());

    addedVertices = 0;
    addedEdges = 0;
    for (var turnRestriction : turnRestrictions) {
      processRestriction(turnRestriction);
      //noinspection Convert2MethodRef
      progressTracker.step(msg -> LOG.info(msg));
    }

    LOG.info(
      "{} Added {} vertices and {} edges.",
      progressTracker.completeMessage(),
      addedVertices,
      addedEdges
    );
  }
}
