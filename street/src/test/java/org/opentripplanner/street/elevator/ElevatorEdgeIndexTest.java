package org.opentripplanner.street.elevator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.Vertex;

class ElevatorEdgeIndexTest {

  private static final String ELEVATOR_A = "Lutz:1";
  private static final String ELEVATOR_B = "Koch:2";

  private final Graph graph = new Graph();
  private final Vertex a0 = vertex(0, 0);
  private final Vertex a1 = vertex(0, 1);
  private final Vertex a2 = vertex(0, 2);
  private final Vertex b0 = vertex(1, 0);
  private final Vertex b1 = vertex(1, 1);
  private final Vertex c0 = vertex(2, 0);
  private final Vertex c1 = vertex(2, 1);

  {
    // Elevator A has three levels, B two, and C has no id
    elevator(a0, a1, ELEVATOR_A);
    elevator(a1, a2, ELEVATOR_A);
    elevator(b0, b1, ELEVATOR_B);
    elevator(c0, c1, null);
  }

  private final ElevatorEdgeIndex subject = ElevatorEdgeIndex.of(graph);

  @Test
  void indexesTheElevatorsWithAnId() {
    assertEquals(Set.of(ELEVATOR_A, ELEVATOR_B), subject.elevatorIds());
    assertTrue(subject.contains(ELEVATOR_A));
    assertFalse(subject.contains("unknown"));
  }

  @Test
  void setOutOfService() {
    var unknown = subject.setOutOfService(List.of(ELEVATOR_A, "unknown"));

    assertEquals(Set.of("unknown"), unknown);
    assertEquals(4, countOutOfService(ELEVATOR_A));
    assertEquals(0, countOutOfService(ELEVATOR_B));
    assertEquals(0, countOutOfService(null));
  }

  @Test
  void setOutOfServiceReplacesTheSetOfThePreviousCall() {
    subject.setOutOfService(List.of(ELEVATOR_A));
    subject.setOutOfService(List.of(ELEVATOR_B));

    assertEquals(0, countOutOfService(ELEVATOR_A));
    assertEquals(2, countOutOfService(ELEVATOR_B));

    subject.setOutOfService(List.of());

    assertEquals(0, countOutOfService(ELEVATOR_B));
  }

  private long countOutOfService(String id) {
    long count = 0;
    for (var edge : graph.findEdges(ElevatorHopEdge.class)) {
      if (Objects.equals(edge.id().orElse(null), id) && edge.isOutOfService()) {
        count++;
      }
    }
    return count;
  }

  private Vertex vertex(double x, double y) {
    var vertex = intersectionVertex(x, y);
    graph.addVertex(vertex);
    return vertex;
  }

  private static void elevator(Vertex from, Vertex to, String id) {
    ElevatorHopEdge.bidirectional(
      from,
      to,
      StreetTraversalPermission.ALL,
      Accessibility.POSSIBLE,
      1,
      -1,
      id
    );
  }
}
