package org.opentripplanner.street.model.edge;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class EscalatorEdgeTest {

  Vertex from = new SimpleVertex("A", 10, 10);
  Vertex to = new SimpleVertex("B", 10.001, 10.001);

  static Stream<Arguments> args() {
    return Stream.of(Arguments.of(1.5, 150), Arguments.of(3.0, 300));
  }

  @ParameterizedTest(name = "escalatorReluctance of {0} should lead to traversal costs of {1}")
  @MethodSource("args")
  void testWalking(double escalatorReluctance, double expectedWeight) {
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 45, null);
    var req = StreetSearchRequest
      .of()
      .withPreferences(p ->
        p.withWalk(w -> w.withEscalator(escalator -> escalator.withReluctance(escalatorReluctance)))
      )
      .withMode(StreetMode.WALK);

    var res = edge.traverse(new State(from, req.build()))[0];
    assertEquals(expectedWeight, res.weight);
    assertEquals(100_000, res.getTimeDeltaMilliseconds());
  }

  @Test
  void testDuration() {
    // If duration is given, length does not affect timeDeltaSeconds, only duration does.
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 45, Duration.ofSeconds(60));
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK);
    var res = edge.traverse(new State(from, req.build()))[0];
    assertEquals(60_000, res.getTimeDeltaMilliseconds());
  }

  @Test
  void testCycling() {
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 10, null);
    var req = StreetSearchRequest.of().withMode(StreetMode.BIKE);
    var res = edge.traverse(new State(from, req.build()));
    assertThat(res).isEmpty();
  }

  @Test
  void testWheelchair() {
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 10, null);
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK).withWheelchair(true);
    var res = edge.traverse(new State(from, req.build()));
    assertThat(res).isEmpty();
  }

  @Test
  void name() {
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 10, null);
    assertEquals("Rolltreppe", edge.getName().toString(Locale.GERMANY));
    assertEquals("escalator", edge.getName().toString(Locale.ENGLISH));
  }

  @Test
  void geometry() {
    var edge = EscalatorEdge.createEscalatorEdge(from, to, 10, null);
    assertThat(edge.getGeometry().getCoordinates()).isNotEmpty();
  }
}
