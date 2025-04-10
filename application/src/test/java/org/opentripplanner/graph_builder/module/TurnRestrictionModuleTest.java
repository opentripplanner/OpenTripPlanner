package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

public class TurnRestrictionModuleTest {
  private StreetVertex vertex(Graph graph, long nodeId, double lat, double lon) {
    var v = new OsmVertex(lat, lon, nodeId);
    graph.addVertex(v);
    return v;
  }

  private StreetEdge streetEdge(StreetVertex a, StreetVertex b, double length) {
    return new StreetEdgeBuilder<>()
      .withFromVertex(a)
      .withToVertex(b)
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();
  }

  private StreetEdge[] edges(StreetVertex a, StreetVertex b, double length) {
    return new StreetEdge[] { streetEdge(a, b, length), streetEdge(b, a, length) };
  }

  @Test
  public void singleTurnRestriction() {
    var graph = new Graph();
    StreetVertex A = vertex(graph, 1, -1, 0);
    StreetVertex B = vertex(graph, 2, 0, 0);
    StreetVertex C = vertex(graph, 3, 0, 1);
    StreetVertex D = vertex(graph, 4, 1, 0);
    StreetVertex E = vertex(graph, 5, 0, -1);
    var AB = edges(A, B, 1.0);
    edges(B, C, 1.0);
    edges(B, D, 1.0);
    var BE = edges(B, E, 1.0);
    AB[0].addTurnRestriction(
      new TurnRestriction(
        AB[0],
        BE[0],
        TurnRestrictionType.NO_TURN,
        new TraverseModeSet(TraverseMode.CAR),
        null
      )
    );
    var module = new TurnRestrictionModule(graph);
    module.buildGraph();

    assertEquals(2, A.getOutgoing().size());
    var newOutgoing = A.getOutgoing().stream().filter(e -> e != AB[0]).findFirst().get();
    var newB = newOutgoing.getToVertex();
    assertEquals(3, newB.getOutgoing().size());
  }
}
