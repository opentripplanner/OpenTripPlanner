package org.opentripplanner.graph_builder.module;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
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
    TraverseModeSet traverseModeSet,
    TurnRestrictionType turnRestrictionType
  ) {
    TurnRestriction restriction = new TurnRestriction(
      from,
      to,
      turnRestrictionType,
      traverseModeSet
    );
    return restriction;
  }

  private void turnRestriction(
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    StreetEdge from,
    StreetEdge to,
    TraverseModeSet traverseModeSet,
    TurnRestrictionType turnRestrictionType
  ) {
    osmInfoGraphBuildRepository.addTurnRestriction(
      turnRestriction(from, to, traverseModeSet, turnRestrictionType)
    );
  }

  private TurnRestriction turnRestriction(
    StreetEdge from,
    StreetEdge to,
    TraverseModeSet traverseModeSet
  ) {
    return turnRestriction(from, to, traverseModeSet, TurnRestrictionType.NO_TURN);
  }

  private TurnRestriction turnRestriction(
    StreetEdge from,
    StreetEdge to,
    TurnRestrictionType turnRestrictionType
  ) {
    return turnRestriction(from, to, TraverseModeSet.allModes(), turnRestrictionType);
  }

  private TurnRestriction turnRestriction(StreetEdge from, StreetEdge to) {
    return turnRestriction(from, to, TraverseModeSet.allModes(), TurnRestrictionType.NO_TURN);
  }

  @Test
  public void singleTurnRestriction() {
    var graph = new Graph();
    var osmInfoGraphBuildRepository = new DefaultOsmInfoGraphBuildRepository();
    var A = vertex(graph, 1, -1, 0);
    var B = vertex(graph, 2, 0, 0);
    var C = vertex(graph, 3, 0, 1);
    var D = vertex(graph, 4, 1, 0);
    var E = vertex(graph, 5, 0, -1);
    var AB = edges(A, B, 1.0);
    edges(B, C, 1.0);
    edges(B, D, 1.0);
    var BE = edges(B, E, 1.0);
    osmInfoGraphBuildRepository.addTurnRestriction(
      new TurnRestriction(AB[0], BE[0], TurnRestrictionType.NO_TURN, TraverseModeSet.allModes())
    );
    var module = new TurnRestrictionModule(graph, osmInfoGraphBuildRepository);
    module.buildGraph();

    var newB = graph
      .getVertices()
      .stream()
      .filter(v -> v.sameLocation(B) && v != B)
      .findFirst()
      .get();
    assertThat(graph.getVertices()).containsExactly(A, B, C, D, E, newB);
    var newOutB = newB.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(newOutB).containsExactly(A, C, D);
    var outB = B.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(outB).containsExactly(A, C, D, E);
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
    var osmInfoGraphBuildRepository = new DefaultOsmInfoGraphBuildRepository();
    var A = vertex(graph, 1, -1, 0);
    var B = vertex(graph, 2, 0, 0);
    var C = vertex(graph, 3, 0, 1);
    var D = vertex(graph, 4, 1, 0);
    var E = vertex(graph, 5, 0, -1);
    var F = vertex(graph, 6, 1, -1);
    var G = vertex(graph, 7, 0, -2);
    var H = vertex(graph, 8, -1, -1);
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
    var module = new TurnRestrictionModule(graph, osmInfoGraphBuildRepository);
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
    var newOutB = newB.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(newOutB).containsExactly(A, C, D);
    var outB = B.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(outB).containsExactly(A, C, D, newE);
    var newOutE = newE.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(newOutE).containsExactly(B, F, G);
    var outE = E.getOutgoing().stream().map(Edge::getToVertex).toList();
    assertThat(outE).containsExactly(B, F, G, H);

    var newInB = newB.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(newInB).containsExactly(A);
    var inB = B.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(inB).containsExactly(C, D, E, newE);
    var newInE = newE.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(newInE).containsExactly(B);
    var inE = E.getIncoming().stream().map(Edge::getFromVertex).toList();
    assertThat(inE).containsExactly(F, G, H);
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, 2, 3, 4, 5 })
  public void tripleTurnRestriction(int order) {
    //   F D
    //   | |
    // G-E-B-C
    //     |
    //     A
    // only turn: A-B-C, D-B-E, B-E-F
    var graph = new Graph();
    var osmInfoGraphBuildRepository = new DefaultOsmInfoGraphBuildRepository();
    var A = vertex(graph, 1, -1, 0);
    var B = vertex(graph, 2, 0, 0);
    var C = vertex(graph, 3, 0, 1);
    var D = vertex(graph, 4, 1, 0);
    var E = vertex(graph, 5, 0, -1);
    var F = vertex(graph, 6, 1, -1);
    var G = vertex(graph, 7, 0, -2);
    var AB = edges(A, B, 1.0);
    var BC = edges(B, C, 1.0);
    var BD = edges(B, D, 1.0);
    var BE = edges(B, E, 1.0);
    var EF = edges(E, F, 1.0);
    edges(E, G, 1.0);
    List<TurnRestriction> turnRestrictions = new ArrayList<>();
    turnRestrictions.add(turnRestriction(AB[0], BC[0], TurnRestrictionType.ONLY_TURN));
    turnRestrictions.add(turnRestriction(BD[1], BE[0], TurnRestrictionType.ONLY_TURN));
    turnRestrictions.add(turnRestriction(BE[0], EF[0], TurnRestrictionType.ONLY_TURN));
    assertEquals(4, B.getOutgoing().size());
    var module = new TurnRestrictionModule(graph, osmInfoGraphBuildRepository);
    // Test all orders in which the turn restrictions can be applied.
    //module.buildGraph();
    List<TurnRestriction> useTurnRestrictions = new ArrayList<>();
    useTurnRestrictions.add(turnRestrictions.get(order % 3));
    turnRestrictions.remove(order % 3);
    order /= 3;
    useTurnRestrictions.add(turnRestrictions.get(order % 2));
    turnRestrictions.remove(order % 2);
    order /= 2;
    useTurnRestrictions.add(turnRestrictions.get(0));
    for (var turnRestriction : useTurnRestrictions) {
      module.processRestriction(turnRestriction);
    }

    var Bs = graph.getVertices().stream().filter(v -> v.sameLocation(B) && v != B).toList();
    var newE = graph
      .getVertices()
      .stream()
      .filter(v -> v.sameLocation(E) && v != E)
      .findFirst()
      .get();
    var B1 = Bs.get(0);
    var B2 = Bs.get(1);
    if (B1.getIncoming().stream().map(Edge::getFromVertex).toList().contains(D)) {
      var swap = B1;
      B1 = B2;
      B2 = swap;
    }
    assertThat(graph.getVertices()).containsExactly(A, B, C, D, E, F, G, B1, B2, newE);
    assertThat(B1.getIncoming().stream().map(Edge::getFromVertex)).containsExactly(A);
    assertThat(B2.getIncoming().stream().map(Edge::getFromVertex)).containsExactly(D);
    assertThat(B.getIncoming().stream().map(Edge::getFromVertex)).containsExactly(C, E);
    assertThat(newE.getIncoming().stream().map(Edge::getFromVertex)).containsExactly(B, B2);
    assertThat(E.getIncoming().stream().map(Edge::getFromVertex)).containsExactly(F, G);
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
    var osmInfoGraphBuildRepository = new DefaultOsmInfoGraphBuildRepository();
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
    osmInfoGraphBuildRepository.addTurnRestriction(
      turnRestriction(BD[0], DF[0], new TraverseModeSet(TraverseMode.CAR))
    );

    var module = new TurnRestrictionModule(graph, osmInfoGraphBuildRepository);
    module.buildGraph();

    var streetRequest = new StreetRequest(StreetMode.CAR);

    var request = RouteRequest.of().withJourney(j -> j.withDirect(streetRequest)).buildDefault();

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
