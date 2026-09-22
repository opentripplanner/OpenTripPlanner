package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class CarpoolCorridorTest {

  private static final WgsCoordinate A = new WgsCoordinate(63.43, 10.39);
  private static final WgsCoordinate B = A.moveEastMeters(20_000);
  private static final List<Vertex> WAYPOINTS = List.of(vertex("a", A), vertex("b", B));

  @Test
  void oneLimitPerLegIsRequired() {
    assertThrows(IllegalArgumentException.class, () ->
      new CarpoolCorridor(List.of(Duration.ofMinutes(15)), List.of(), List.of(), WAYPOINTS, 40.0)
    );
  }

  private static Vertex vertex(String label, WgsCoordinate coordinate) {
    return new SimpleVertex(label, coordinate.latitude(), coordinate.longitude());
  }
}
