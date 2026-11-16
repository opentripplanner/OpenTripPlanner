package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  /** One leg, 20 km, allowed to take 30 minutes: at 40 m/s that is a 72 km ellipse. */
  private static final CarpoolCorridor CORRIDOR = corridor(40.0);

  @Test
  void aPointTheDetourCanReachMayBeServed() {
    var nearTheLine = A.moveEastMeters(10_000).moveNorthMeters(5_000);
    assertTrue(CORRIDOR.mayServe(vertex("p", nearTheLine)));
    assertTrue(CORRIDOR.mayServe(WAYPOINTS.getFirst()), "the leg start itself");
  }

  @Test
  void aPointBeyondTheDetourBudgetMayNot() {
    var farNorth = A.moveEastMeters(10_000).moveNorthMeters(60_000);
    assertFalse(CORRIDOR.mayServe(vertex("p", farNorth)));
    // Slower cars make the ellipse smaller: 30 min at 15 m/s is 27 km of driving, and a point
    // 12 km off the middle of the 20 km leg needs 2 x 15.6 km.
    var offTheLine = A.moveEastMeters(10_000).moveNorthMeters(12_000);
    assertTrue(CORRIDOR.mayServe(vertex("p", offTheLine)));
    assertFalse(corridor(15.0).mayServe(vertex("p", offTheLine)));
  }

  @Test
  void oneLimitPerLegIsRequired() {
    assertThrows(IllegalArgumentException.class, () ->
      new CarpoolCorridor(List.of(Duration.ofMinutes(15)), List.of(), List.of(), WAYPOINTS, 40.0)
    );
  }

  private static CarpoolCorridor corridor(double maxCarSpeed) {
    return new CarpoolCorridor(
      List.of(Duration.ofMinutes(15)),
      List.of(Duration.ofMinutes(30)),
      List.of(),
      WAYPOINTS,
      maxCarSpeed
    );
  }

  private static Vertex vertex(String label, WgsCoordinate coordinate) {
    return new SimpleVertex(label, coordinate.latitude(), coordinate.longitude());
  }
}
