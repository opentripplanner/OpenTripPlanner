package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter.Direction;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class PassengerTreesTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final double MAX_SPEED = 40.0;

  @Test
  void forwardTreeSubtractsTheBeelineFromTheLegStart() {
    var leg = List.of(vertexEastOfOrigin(0), vertexEastOfOrigin(20_000));
    var passenger = vertexEastOfOrigin(4_000);

    var limit = PassengerTrees.treeLimit(
      passenger,
      leg,
      List.of(Duration.ofMinutes(30)),
      MAX_SPEED,
      Direction.FROM
    );

    // 4 km at 40 m/s is 100 s.
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(100), limit);
  }

  @Test
  void reverseTreeSubtractsTheBeelineToTheLegEnd() {
    var leg = List.of(vertexEastOfOrigin(0), vertexEastOfOrigin(20_000));
    var passenger = vertexEastOfOrigin(12_000);

    var limit = PassengerTrees.treeLimit(
      passenger,
      leg,
      List.of(Duration.ofMinutes(30)),
      MAX_SPEED,
      Direction.TO
    );

    // 8 km at 40 m/s is 200 s.
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(200), limit);
  }

  @Test
  void theWidestLegDecidesAndTheLimitIsNeverNegative() {
    var waypoints = List.of(
      vertexEastOfOrigin(0),
      vertexEastOfOrigin(1_000),
      vertexEastOfOrigin(30_000)
    );
    var legLimits = List.of(Duration.ofMinutes(2), Duration.ofMinutes(40));

    // Leg 0: 2 min - 50 s; leg 1: 40 min - 25 s.
    var near = vertexEastOfOrigin(2_000);
    assertWithinTwoSeconds(
      Duration.ofMinutes(40).minusSeconds(25),
      PassengerTrees.treeLimit(near, waypoints, legLimits, MAX_SPEED, Direction.FROM)
    );

    var far = vertexEastOfOrigin(200_000);
    assertEquals(
      Duration.ZERO,
      PassengerTrees.treeLimit(far, waypoints, legLimits, MAX_SPEED, Direction.FROM)
    );
  }

  private static Vertex vertexEastOfOrigin(double meters) {
    var coordinate = ORIGIN.moveEastMeters(meters);
    return new SimpleVertex("v" + meters, coordinate.latitude(), coordinate.longitude());
  }

  /** The vertices are placed by an approximate metres-to-degrees conversion. */
  private static void assertWithinTwoSeconds(Duration expected, Duration actual) {
    assertTrue(
      Math.abs(expected.minus(actual).toSeconds()) <= 2,
      "expected about " + expected + " but was " + actual
    );
  }
}
