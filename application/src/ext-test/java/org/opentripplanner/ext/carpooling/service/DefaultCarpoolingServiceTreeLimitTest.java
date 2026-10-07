package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * How far the passenger's tree reaches: the leg's limit minus the beeline the driver needs, at the
 * fastest speed, between the passenger and the leg's start (forward tree) or end (reverse tree).
 * The test coordinates are placed with a metres-to-degrees approximation, hence the tolerance.
 */
class DefaultCarpoolingServiceTreeLimitTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final double MAX_SPEED = 40.0;

  @Test
  void forwardTreeSubtractsTheBeelineFromTheLegStart() {
    var leg = List.of(vertexEastOfOrigin(0), vertexEastOfOrigin(20_000));
    var passenger = vertexEastOfOrigin(4_000);

    var limit = DefaultCarpoolingService.passengerTreeLimit(
      passenger,
      leg,
      List.of(Duration.ofMinutes(30)),
      MAX_SPEED,
      true
    );

    // 4 km at 40 m/s is 100 s.
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(100), limit);
  }

  @Test
  void reverseTreeSubtractsTheBeelineToTheLegEnd() {
    var leg = List.of(vertexEastOfOrigin(0), vertexEastOfOrigin(20_000));
    var passenger = vertexEastOfOrigin(12_000);

    var limit = DefaultCarpoolingService.passengerTreeLimit(
      passenger,
      leg,
      List.of(Duration.ofMinutes(30)),
      MAX_SPEED,
      false
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
      DefaultCarpoolingService.passengerTreeLimit(near, waypoints, legLimits, MAX_SPEED, true)
    );

    var far = vertexEastOfOrigin(200_000);
    assertEquals(
      Duration.ZERO,
      DefaultCarpoolingService.passengerTreeLimit(far, waypoints, legLimits, MAX_SPEED, true)
    );
  }

  private static Vertex vertexEastOfOrigin(double meters) {
    var coordinate = ORIGIN.moveEastMeters(meters);
    return new SimpleVertex("v" + meters, coordinate.latitude(), coordinate.longitude());
  }

  private static void assertWithinTwoSeconds(Duration expected, Duration actual) {
    assertTrue(
      Math.abs(expected.minus(actual).toSeconds()) <= 2,
      "expected about " + expected + " but was " + actual
    );
  }
}
