package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViaLocationTest {

  private static final int STOP_A = 12;
  private static final int STOP_B = 13;
  private static final Duration DURATION = Duration.ofMinutes(3);
  private static final int C1 = 200;

  @Test
  void testAssessors() {
    var connections = List.of(ViaConnection.stop(STOP_A, STOP_B, DURATION, C1));
    var subject = new ViaLocation("Nnn", connections);

    assertEquals("Nnn", subject.label());
    assertEquals(connections, subject.connections());
  }

  @Test
  void twoNoneParetoOptimalConnectionsAreNotAllowed() {
    var e = assertThrows(
      IllegalArgumentException.class,
      () ->
        new ViaLocation(
          "Via",
          List.of(ViaConnection.passThroughStop(STOP_A), ViaConnection.stop(STOP_A, DURATION))
        )
    );
    assertEquals(
      "All connection need to be pareto-optimal. a: PassThrough(12), b: Via(3m 12)",
      e.getMessage()
    );
  }

  @Test
  void testToString() {
    assertEquals(
      "ViaLocation{label: Nnn, connections: [PassThrough(12), PassThrough(13)]}",
      new ViaLocation(
        "Nnn",
        List.of(ViaConnection.passThroughStop(STOP_A), ViaConnection.passThroughStop(STOP_B))
      )
        .toString()
    );
  }
}
