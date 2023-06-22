package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class EscalatorEdgeTest {

  Graph graph = new Graph();
  Vertex from = new SimpleVertex(graph, "A", 10, 10);
  Vertex to = new SimpleVertex(graph, "B", 10.001, 10.001);

  @Test
  void testWalking() {
    var edge = new EscalatorEdge(from, to, 45);
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK);
    var res = edge.traverse(new State(from, req.build()))[0];
    assertEquals(res.weight, 150);
    assertEquals(res.getTimeDeltaSeconds(), 100);
  }

  @Test
  void testCycling() {
    var edge = new EscalatorEdge(from, to, 10);
    var req = StreetSearchRequest.of().withMode(StreetMode.BIKE);
    var res = edge.traverse(new State(from, req.build()));
    assertEquals(res.length, 0);
  }

  @Test
  void testWheelchair() {
    var edge = new EscalatorEdge(from, to, 10);
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK).withWheelchair(true);
    var res = edge.traverse(new State(from, req.build()));
    assertEquals(res.length, 0);
  }
}
