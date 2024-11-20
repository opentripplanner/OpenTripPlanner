package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporarySplitterVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

class StreetEdgeSplittingTest extends GraphRoutingTest {

  private final Graph graph;
  private StreetVertex V1, V2;
  private StreetEdge streetEdge1, streetEdge2;
  private TurnRestriction originalTurnRestriction;

  public StreetEdgeSplittingTest() {
    graph = graph();
  }

  @Test
  public void turnRestrictionFromEdgeSplit() {
    var splitVtx = new SplitterVertex("Split_Vertex", 1.0, 0.0, new NonLocalizedString("a name"));

    var splitResult = streetEdge1.splitDestructively(splitVtx);
    assertTrue(splitResult.head().getTurnRestrictions().isEmpty());
    assertFalse(splitResult.tail().getTurnRestrictions().isEmpty());
    assertEquals(streetEdge2, addedRestriction(splitResult.tail()).to);
    // The turn restriction is removed when the edge is removed
    assertOriginalRestrictionExists();

    graph.removeEdge(streetEdge1);
    assertOriginalRestrictionMissing();

    assertTrue(splitResult.head().getTurnRestrictions().isEmpty());
    assertFalse(splitResult.tail().getTurnRestrictions().isEmpty());
    assertEquals(streetEdge2, addedRestriction(splitResult.tail()).to);
  }

  @Test
  public void turnRestrictionToEdgeSplit() {
    var splitVtx = new SplitterVertex("Split_Vertex", 1.0, 1.0, new NonLocalizedString("a name"));

    var splitResult = streetEdge2.splitDestructively(splitVtx);
    assertEquals(splitResult.head(), addedRestriction(streetEdge1).to);
    // The turn restriction is removed when the edge is removed
    assertOriginalRestrictionExists();

    graph.removeEdge(streetEdge2);
    assertOriginalRestrictionMissing();

    assertTrue(splitResult.head().getTurnRestrictions().isEmpty());
    assertTrue(splitResult.tail().getTurnRestrictions().isEmpty());
    assertEquals(splitResult.head(), addedRestriction(streetEdge1).to);
  }

  @Test
  public void turnRestrictionFromEdgeSplitWithTemporary() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 0.0, streetEdge1);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge1.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.BOTH_WAYS
    );
    assertTrue(splitResult.head().getTurnRestrictions().isEmpty());
    assertFalse(splitResult.tail().getTurnRestrictions().isEmpty());
    assertEquals(streetEdge2, addedRestriction(splitResult.tail()).to);
    assertOriginalRestrictionExists();

    disposableEdgeCollection.disposeEdges();
    assertTrue(splitResult.head().getTurnRestrictions().isEmpty());
    assertTrue(splitResult.tail().getTurnRestrictions().isEmpty());
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionToEdgeSplitTemporary() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 1.0, streetEdge2);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge2.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.BOTH_WAYS
    );
    assertEquals(splitResult.head(), addedRestriction(streetEdge1).to);
    assertOriginalRestrictionExists();

    disposableEdgeCollection.disposeEdges();
    assertTrue(splitResult.head().getTurnRestrictions().isEmpty());
    assertTrue(splitResult.tail().getTurnRestrictions().isEmpty());
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionFromEdgeSplitWithToVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 0.0, streetEdge1);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge1.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.OUTGOING
    );

    assertOnlyOriginalRestrictionExists();
    assertTrue(splitResult.head().getTurnRestrictions().isEmpty());
    assertNull(splitResult.tail());

    disposableEdgeCollection.disposeEdges();
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionToEdgeSplitWithToVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 1.0, streetEdge2);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge2.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.OUTGOING
    );

    var turnRestrictions = streetEdge1.getTurnRestrictions();
    assertEquals(2, turnRestrictions.size());

    assertOriginalRestrictionExists();
    assertEquals(splitResult.head(), addedRestriction(streetEdge1).to);

    disposableEdgeCollection.disposeEdges();
    assertOnlyOriginalRestrictionExists();
  }

  @Test
  public void turnRestrictionFromEdgeSplitWithFromVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 0.0, streetEdge1);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge1.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.INCOMING
    );

    assertOnlyOriginalRestrictionExists();

    assertEquals(streetEdge2, addedRestriction(splitResult.tail()).to);

    disposableEdgeCollection.disposeEdges();

    assertOnlyOriginalRestrictionExists();
    assertTrue(splitResult.tail().getTurnRestrictions().isEmpty());
  }

  @Test
  public void turnRestrictionToEdgeSplitWithFromVertex() {
    var splitVtx = new TemporarySplitterVertex("Split_Vertex", 1.0, 1.0, streetEdge2);
    var disposableEdgeCollection = new DisposableEdgeCollection(graph);

    var splitResult = streetEdge2.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.INCOMING
    );

    assertOnlyOriginalRestrictionExists();
    assertTrue(splitResult.tail().getTurnRestrictions().isEmpty());

    disposableEdgeCollection.disposeEdges();
    assertOnlyOriginalRestrictionExists();
  }

  private Graph graph() {
    TestOtpModel model = modelOf(
      new Builder() {
        @Override
        public void build() {
          V1 = intersection("V1", 0.0, 0.0);
          V2 = intersection("V2", 2.0, 0.0);

          streetEdge1 = street(V2, V1, 100, StreetTraversalPermission.ALL);
          streetEdge2 = street(V1, V2, 100, StreetTraversalPermission.ALL);

          originalTurnRestriction = disallowTurn(streetEdge1, streetEdge2);
        }
      }
    );
    return model.graph();
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
    return streetEdge
      .getTurnRestrictions()
      .stream()
      .filter(tr -> tr != originalTurnRestriction)
      .findFirst()
      .orElseThrow();
  }

  private TurnRestriction disallowTurn(StreetEdge from, StreetEdge to) {
    TurnRestrictionType restrictionType = TurnRestrictionType.NO_TURN;
    TraverseModeSet restrictedModes = new TraverseModeSet(TraverseMode.CAR);
    TurnRestriction restrict = new TurnRestriction(
      from,
      to,
      restrictionType,
      restrictedModes,
      null
    );
    from.addTurnRestriction(restrict);
    return restrict;
  }
}
