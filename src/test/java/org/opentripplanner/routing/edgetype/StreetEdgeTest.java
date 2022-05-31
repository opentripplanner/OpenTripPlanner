package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.WheelchairAccessibilityFeature.ofOnlyAccessible;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.test.support.VariableSource;

class StreetEdgeTest extends GraphRoutingTest {

  private final Graph graph;
  private StreetVertex V1, V2;
  private StreetEdge streetEdge1, streetEdge2;
  private TurnRestriction originalTurnRestriction;

  public StreetEdgeTest() {
    graph = graph();
  }

  @Test
  public void turnRestrictionFromEdgeSplit() {
    var splitVtx = new SplitterVertex(graph, "Split_Vertex", 1.0, 0.0);

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
    var splitVtx = new SplitterVertex(graph, "Split_Vertex", 1.0, 1.0);

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

    var splitResult = streetEdge1.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.BOTH_WAYS
    );
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

    var splitResult = streetEdge2.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.BOTH_WAYS
    );
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

    var splitResult = streetEdge1.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.OUTGOING
    );

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

    var splitResult = streetEdge2.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.OUTGOING
    );

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

    var splitResult = streetEdge1.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.INCOMING
    );

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

    var splitResult = streetEdge2.splitNonDestructively(
      splitVtx,
      disposableEdgeCollection,
      LinkingDirection.INCOMING
    );

    assertOnlyOriginalRestrictionExists();
    assertTrue(splitResult.second.getTurnRestrictions().isEmpty());

    disposableEdgeCollection.disposeEdges();
    assertOnlyOriginalRestrictionExists();
  }

  static Stream<Arguments> slopeCases = Stream.of(
    Arguments.of(0.07f, 5081), // no extra cost
    Arguments.of(0.08f, 5945), // no extra cost
    Arguments.of(0.09f, 6908), // no extra cost
    Arguments.of(0.091f, 7708), // 0.1 % above the max slope, tiny extra cost
    Arguments.of(0.0915f, 11645), // 0.15 % above the max slope, will incur larger cost
    Arguments.of(0.11f, 194144) // 2 % above max slope, will incur very large cost
  );

  /**
   * This makes sure that when you exceed the max slope in a wheelchair there isn't a hard cut-off
   * but rather the cost increases proportional to how much you go over the maximum.
   * <p>
   * In other words: 0.1 % over the limit only has a small cost but 2% over increases it
   * dramatically to the point where it's only used as a last resort.
   */
  @ParameterizedTest(name = "slope of {0} should lead to traversal costs of {1}")
  @VariableSource("slopeCases")
  public void shouldScaleCostWithMaxSlope(float slope, long expectedCost) {
    double length = 1000;
    var edge = new StreetEdge(
      V1,
      V2,
      null,
      "edge with elevation",
      length,
      StreetTraversalPermission.ALL,
      false
    );

    Coordinate[] profile = new Coordinate[] {
      new Coordinate(0, 0),
      new Coordinate(length, slope * length),
    };

    PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
    StreetElevationExtension.addToEdge(edge, elev, true);

    assertEquals(slope, edge.getMaxSlope(), 0.0001);

    var req = new RoutingRequest();
    req.wheelchairAccessibility =
      new WheelchairAccessibilityRequest(
        true,
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        25,
        0.09,
        1.1f,
        10
      );
    State result = traverse(edge, req);
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);
  }

  static Stream<Arguments> stairsCases = Stream.of(
    Arguments.of(1f, 22),
    Arguments.of(1.5f, 33),
    Arguments.of(3f, 67)
  );

  @ParameterizedTest(name = "stairs reluctance of of {0} should lead to traversal costs of {1}")
  @VariableSource("stairsCases")
  public void stairsReluctance(float stairsReluctance, long expectedCost) {
    double length = 10;
    var edge = new StreetEdge(V1, V2, null, "stairs", length, StreetTraversalPermission.ALL, false);
    edge.setStairs(true);

    var req = new RoutingRequest();
    req.stairsReluctance = stairsReluctance;

    var result = traverse(edge, req);
    assertEquals(expectedCost, (long) result.weight);

    edge.setStairs(false);
    var notStairsResult = traverse(edge, req);
    assertEquals(15, (long) notStairsResult.weight);
  }

  static Stream<Arguments> wheelchairStairsCases = Stream.of(
    Arguments.of(1f, 22),
    Arguments.of(10f, 225),
    Arguments.of(100f, 2255)
  );

  @ParameterizedTest(
    name = "wheelchair stairs reluctance of {0} should lead to traversal costs of {1}"
  )
  @VariableSource("wheelchairStairsCases")
  public void wheelchairStairsReluctance(float stairsReluctance, long expectedCost) {
    double length = 10;
    var edge = new StreetEdge(V1, V2, null, "stairs", length, StreetTraversalPermission.ALL, false);
    edge.setStairs(true);

    var req = new RoutingRequest();
    req.wheelchairAccessibility =
      new WheelchairAccessibilityRequest(
        true,
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        25,
        0,
        1.1f,
        stairsReluctance
      );

    var result = traverse(edge, req);
    assertEquals(expectedCost, (long) result.weight);

    edge.setStairs(false);
    var notStairsResult = traverse(edge, req);
    assertEquals(15, (long) notStairsResult.weight);
  }

  static Stream<Arguments> inaccessibleStreetCases = Stream.of(
    Arguments.of(1f, 15),
    Arguments.of(10f, 150),
    Arguments.of(100f, 1503)
  );

  @ParameterizedTest(
    name = "an inaccessible street with the reluctance of {0} should lead to traversal costs of {1}"
  )
  @VariableSource("inaccessibleStreetCases")
  public void inaccessibleStreet(float inaccessibleStreetReluctance, long expectedCost) {
    double length = 10;
    var edge = new StreetEdge(V1, V2, null, "stairs", length, StreetTraversalPermission.ALL, false);
    edge.setWheelchairAccessible(false);

    var req = new RoutingRequest();
    req.wheelchairAccessibility =
      new WheelchairAccessibilityRequest(
        true,
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        inaccessibleStreetReluctance,
        0,
        1.1f,
        25
      );

    var result = traverse(edge, req);
    assertEquals(expectedCost, (long) result.weight);

    // reluctance should have no effect when the edge is accessible
    edge.setWheelchairAccessible(true);
    var accessibleResult = traverse(edge, req);
    assertEquals(15, (long) accessibleResult.weight);
  }

  private State traverse(StreetEdge edge, RoutingRequest req) {
    var ctx = new RoutingContext(req, graph, V1, V2);
    var state = new State(ctx);

    assertEquals(0, state.weight);
    return edge.traverse(state);
  }

  private Graph graph() {
    return graphOf(
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
