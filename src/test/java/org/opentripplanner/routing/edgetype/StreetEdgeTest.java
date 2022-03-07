package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;

class StreetEdgeTest extends GraphRoutingTest {

  private StreetVertex V1, V2;

  private StreetEdge streetEdge1, streetEdge2;

  private final Graph graph;

  private TurnRestriction originalTurnRestriction;

  public StreetEdgeTest() {
    graph = graph();
  }

  private Graph graph() {
    return graphOf(new Builder() {
      @Override
      public void build() {
        V1 = intersection("V1", 0.0, 0.0);
        V2 = intersection("V2", 2.0, 0.0);

        streetEdge1 = street(V2, V1, 100, StreetTraversalPermission.ALL);
        streetEdge2 = street(V1, V2, 100, StreetTraversalPermission.ALL);

        originalTurnRestriction = disallowTurn(streetEdge1, streetEdge2);
      }
    });
  }

  @Test
  public void turnRestrictionFromEdgeSplit() {
    var splitVtx = new SplitterVertex(graph, "Split_Vertex", 1.0, 0.0, streetEdge1);

    var splitResult = streetEdge1.splitDestructively(splitVtx);
    assertTrue(splitResult.first.getTurnRestrictions().isEmpty());
    assertFalse(splitResult.second.getTurnRestrictions().isEmpty());
    assertEquals(streetEdge2, addedRestriction(splitResult.second).to);
    // The turn restriction is removed when the edge is removed
    assertOriginalRestrictionExists();

    graph.removeEdge(streetEdge1);
    assertOriginalRestrictionMissing();

    assertTrue(splitResult.first.getTurnRestrictions().isEmpty());
    assertFalse(splitResult.second.getTurnRestrictions().isEmpty());
    assertEquals(streetEdge2, addedRestriction(splitResult.second).to);
  }

  @Test
  public void turnRestrictionToEdgeSplit() {
    var splitVtx = new SplitterVertex(graph, "Split_Vertex", 1.0, 1.0, streetEdge2);

    var splitResult = streetEdge2.splitDestructively(splitVtx);
    assertEquals(splitResult.first, addedRestriction(streetEdge1).to);
    // The turn restriction is removed when the edge is removed
    assertOriginalRestrictionExists();

    graph.removeEdge(streetEdge2);
    assertOriginalRestrictionMissing();

    assertTrue(splitResult.first.getTurnRestrictions().isEmpty());
    assertTrue(splitResult.second.getTurnRestrictions().isEmpty());
    assertEquals(splitResult.first, addedRestriction(streetEdge1).to);
  }

  @Test
  public void turnRestrictionFromEdgeSplitWithTemporary() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 0.0, streetEdge1, true);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge1.splitNonDestructively(splitVtx, disposableEdgeCollection, LinkingDirection.BOTH_WAYS);
    assertTrue(splitResult.first.getTurnRestrictions().isEmpty());
    assertFalse(splitResult.second.getTurnRestrictions().isEmpty());
    assertEquals(streetEdge2, addedRestriction(splitResult.second).to);
    assertOriginalRestrictionExists();

    disposableEdgeCollection.disposeEdges();
    assertTrue(splitResult.first.getTurnRestrictions().isEmpty());
    assertTrue(splitResult.second.getTurnRestrictions().isEmpty());
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionToEdgeSplitTemporary() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 1.0, streetEdge2, false);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge2.splitNonDestructively(splitVtx, disposableEdgeCollection, LinkingDirection.BOTH_WAYS);
    assertEquals(splitResult.first, addedRestriction(streetEdge1).to);
    assertOriginalRestrictionExists();

    disposableEdgeCollection.disposeEdges();
    assertTrue(splitResult.first.getTurnRestrictions().isEmpty());
    assertTrue(splitResult.second.getTurnRestrictions().isEmpty());
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionFromEdgeSplitWithToVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 0.0, streetEdge1, true);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge1.splitNonDestructively(splitVtx, disposableEdgeCollection, LinkingDirection.OUTGOING);

    assertOnlyOriginalRestrictionExists();
    assertTrue(splitResult.first.getTurnRestrictions().isEmpty());
    assertNull(splitResult.second);

    disposableEdgeCollection.disposeEdges();
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionToEdgeSplitWithToVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 1.0, streetEdge2, true);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge2.splitNonDestructively(splitVtx, disposableEdgeCollection, LinkingDirection.OUTGOING);

    var turnRestrictions = streetEdge1.getTurnRestrictions();
    assertEquals(2, turnRestrictions.size());

    assertOriginalRestrictionExists();
    assertEquals(splitResult.first, addedRestriction(streetEdge1).to);

    disposableEdgeCollection.disposeEdges();
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionFromEdgeSplitWithFromVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 0.0, streetEdge1, false);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge1.splitNonDestructively(splitVtx, disposableEdgeCollection, LinkingDirection.INCOMING);

    assertOnlyOriginalRestrictionExists();

    assertEquals(streetEdge2, addedRestriction(splitResult.second).to);

    disposableEdgeCollection.disposeEdges();

    assertOnlyOriginalRestrictionExists();
    assertTrue(splitResult.second.getTurnRestrictions().isEmpty());
  }

  @Test
  public void turnRestrictionToEdgeSplitWithFromVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 1.0, streetEdge2, false);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge2.splitNonDestructively(splitVtx, disposableEdgeCollection, LinkingDirection.INCOMING);

    assertOnlyOriginalRestrictionExists();
    assertTrue(splitResult.second.getTurnRestrictions().isEmpty());

    disposableEdgeCollection.disposeEdges();
    assertOnlyOriginalRestrictionExists();
  }

  private void assertOriginalRestrictionExists() {
    var turnRestrictions = streetEdge1.getTurnRestrictions();
    assertTrue(turnRestrictions.contains(originalTurnRestriction));
  }

  private void assertOnlyOriginalRestrictionExists() {
    var turnRestrictions = streetEdge1.getTurnRestrictions();
    assertEquals(1, turnRestrictions.size());
    assertEquals(originalTurnRestriction, turnRestrictions.get(0));
  }

  private void assertOriginalRestrictionMissing() {
    var turnRestrictions = streetEdge1.getTurnRestrictions();
    assertFalse(turnRestrictions.contains(originalTurnRestriction));
  }

  private TurnRestriction addedRestriction(StreetEdge streetEdge) {
    return streetEdge.getTurnRestrictions()
            .stream()
            .filter(tr -> tr != originalTurnRestriction)
            .findFirst()
            .orElseThrow();
  }

  private TurnRestriction disallowTurn(StreetEdge from, StreetEdge to) {
    TurnRestrictionType restrictionType = TurnRestrictionType.NO_TURN;
    TraverseModeSet restrictedModes = new TraverseModeSet(TraverseMode.CAR);
    TurnRestriction restrict = new TurnRestriction(from, to, restrictionType, restrictedModes, null);
    from.addTurnRestriction(restrict);
    return restrict;
  }
}
