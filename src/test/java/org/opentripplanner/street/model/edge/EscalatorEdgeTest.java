package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
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
    var edge = new EscalatorEdge(from, to);
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK);
    var res = edge.traverse(new State(from, req.build()))[0];
    assertNotNull(res);
  }

  @Test
  void testCycling() {
    var edge = new EscalatorEdge(from, to);
    var req = StreetSearchRequest.of().withMode(StreetMode.BIKE);
    var res = edge.traverse(new State(from, req.build()));
    assert (res.length == 0);
  }
}
