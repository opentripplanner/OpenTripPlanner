package org.opentripplanner.graph_builder.module;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.test.support.GeoJsonIo;

public class TurnRestrictionModuleTest {

  private StreetVertex vertex(Graph graph, long nodeId, double lat, double lon) {
    var v = new OsmVertex(lon, lat, nodeId);
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

  private TurnRestriction turnRestriction(
    StreetEdge from,
    StreetEdge to,
    TraverseModeSet traverseModeSet
  ) {
    TurnRestriction restriction = new TurnRestriction(
      from,
      to,
      TurnRestrictionType.NO_TURN,
      traverseModeSet,
      null
    );
    from.addTurnRestriction(restriction);
    return restriction;
  }

  private TurnRestriction turnRestriction(StreetEdge from, StreetEdge to) {
    return turnRestriction(from, to, TraverseModeSet.allModes());
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
          TraverseModeSet.allModes(),
          null
        )
      );
    var module = new TurnRestrictionModule(graph);
    module.buildGraph();

    var newB = graph
      .getVertices()
      .stream()
      .filter(v -> v.sameLocation(B) && v != B)
      .findFirst()
      .get();
    assertThat(graph.getVertices()).containsExactly(A, B, C, D, E, newB);
    var newBout = newB.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(newBout).containsExactly(A, C, D);
    var Bout = B.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(Bout).containsExactly(A, C, D, E);
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1 })
  public void doubleTurnRestriction(int order) {
    //   F D
    //   | |
    // G-E-B-C
    //   | |
    //   H A
    var graph = new Graph();
    StreetVertex A = vertex(graph, 1, -1, 0);
    StreetVertex B = vertex(graph, 2, 0, 0);
    StreetVertex C = vertex(graph, 3, 0, 1);
    StreetVertex D = vertex(graph, 4, 1, 0);
    StreetVertex E = vertex(graph, 5, 0, -1);
    StreetVertex F = vertex(graph, 6, 1, -1);
    StreetVertex G = vertex(graph, 7, 0, -2);
    StreetVertex H = vertex(graph, 8, -1, -1);
    var AB = edges(A, B, 1.0);
    edges(B, C, 1.0);
    edges(B, D, 1.0);
    var BE = edges(B, E, 1.0);
    edges(E, F, 1.0);
    edges(E, G, 1.0);
    var EH = edges(E, H, 1.0);
    List<TurnRestriction> turnRestrictions = new ArrayList<>();
    turnRestrictions.add(turnRestriction(AB[0], BE[0]));
    turnRestrictions.add(turnRestriction(BE[0], EH[0]));
    assertEquals(4, B.getOutgoing().size());
    var module = new TurnRestrictionModule(graph);
    // Test all orders in which the turn restrictions can be applied.
    //module.buildGraph();
    List<TurnRestriction> useTurnRestrictions = new ArrayList<>();
    switch (order) {
      case 0:
        useTurnRestrictions.add(turnRestrictions.get(0));
        useTurnRestrictions.add(turnRestrictions.get(1));
        break;
      case 1:
        useTurnRestrictions.add(turnRestrictions.get(1));
        useTurnRestrictions.add(turnRestrictions.get(0));
        break;
    }
    for (var turnRestriction : useTurnRestrictions) {
      module.processRestriction(turnRestriction);
    }

    var newB = graph
      .getVertices()
      .stream()
      .filter(v -> v.sameLocation(B) && v != B)
      .findFirst()
      .get();
    var newE = graph
      .getVertices()
      .stream()
      .filter(v -> v.sameLocation(E) && v != E)
      .findFirst()
      .get();
    assertThat(graph.getVertices()).containsExactly(A, B, C, D, E, F, G, H, newB, newE);
    var newBout = newB.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(newBout).containsExactly(A, C, D);
    var Bout = B.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(Bout).containsExactly(A, C, D, newE);
    var newEout = newE.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(newEout).containsExactly(B, F, G);
    var Eout = E.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(Eout).containsExactly(B, F, G, H);

    var newBin = newB.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(newBin).containsExactly(A);
    var Bin = B.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(Bin).containsExactly(C, D, E, newE);
    var newEin = newE.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(newEin).containsExactly(B);
    var Ein = E.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(Ein).containsExactly(F, G, H);
  }

  @Test
  public void turnRestrictedVisitDoesNotBlockSearch() {
    // The costs of the edges are set up so that the search first goes A->B->D before trying
    // A->B->C->D. The test tests that the previous visit of D does not block the proper
    // path A->B->C->D->F.
    //      E
    //      |
    //  F - D -\
    //      |   C
    //      B -/
    //      |
    //      A
    // B-D-F is forbidden by a turn restriction
    var graph = new Graph();
    var A = vertex(graph, 1, 0.0, 0.0);
    var B = vertex(graph, 2, 1.0, 0.0);
    var C = vertex(graph, 3, 1.5, 1.0);
    var D = vertex(graph, 4, 2.0, 0.0);
    var E = vertex(graph, 5, 3.0, 0.0);
    var F = vertex(graph, 6, 2.0, -1.0);
    edges(A, B, 1.0);
    edges(B, C, 1.0);
    var BD = edges(B, D, 1.0);
    edges(C, D, 1.0);
    edges(D, E, 1.0);
    var DF = edges(D, F, 1.0);
    turnRestriction(BD[0], DF[0], new TraverseModeSet(TraverseMode.CAR));

    var module = new TurnRestrictionModule(graph);
    module.buildGraph();

    var request = new RouteRequest();
    var dateTime = Instant.now();
    request.setDateTime(dateTime);
    request.journey().direct().setMode(StreetMode.CAR);

    var streetRequest = new StreetRequest(StreetMode.CAR);

    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
      .setRequest(request)
      .setStreetRequest(streetRequest)
      .setFrom(A)
      .setTo(F)
      .getShortestPathTree();
    GraphPath<State, Edge, Vertex> path = spt.getPath(F);
    List<State> states = path.states;
    assertEquals(5, states.size());
    assertEquals(states.get(0).getVertex(), A);
    assertEquals(states.get(1).getVertex(), B);
    assertEquals(states.get(2).getVertex(), C);
    assertEquals(states.get(3).getVertex(), D);
    assertEquals(states.get(4).getVertex(), F);
  }
}
