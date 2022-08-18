package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class StateEditorTest {

  @Test
  public final void testIncrementTimeInSeconds() {
    Graph graph = new Graph();
    RoutingRequest routingRequest = new RoutingRequest();
    RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
    StateEditor stateEditor = new StateEditor(routingContext, null);

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementTimeInSeconds(999999999);

    assertEquals(999999999, stateEditor.child.getTimeSeconds());
  }

  @Test
  public final void testWeightIncrement() {
    Graph graph = new Graph();
    RoutingRequest routingRequest = new RoutingRequest();
    RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
    StateEditor stateEditor = new StateEditor(routingContext, null);

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(10);

    assertNotNull(stateEditor.makeState());
  }

  @Test
  public final void testNanWeightIncrement() {
    Graph graph = new Graph();
    RoutingRequest routingRequest = new RoutingRequest();
    RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
    StateEditor stateEditor = new StateEditor(routingContext, null);

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(Double.NaN);

    assertNull(stateEditor.makeState());
  }

  @Test
  public final void testInfinityWeightIncrement() {
    Graph graph = new Graph();
    RoutingRequest routingRequest = new RoutingRequest();
    RoutingContext routingContext = new RoutingContext(routingRequest, graph, (Vertex) null, null);
    StateEditor stateEditor = new StateEditor(routingContext, null);

    stateEditor.setTimeSeconds(0);
    stateEditor.incrementWeight(Double.NEGATIVE_INFINITY);

    assertNull(stateEditor.makeState(), "Infinity weight increment");
  }
}
