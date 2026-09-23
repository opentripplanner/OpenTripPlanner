package org.opentripplanner.ext.carpooling.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
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

  @Test
  void theDriversSideTreeNeedsEachWaypointOnlyWithinItsLegsReach() {
    // Two legs a0 -> a1 -> a2, allowed 20 and 30 minutes; the passenger is 1 km past a0.
    var a0 = vertexEastOfOrigin(0);
    var a1 = vertexEastOfOrigin(10_000);
    var a2 = vertexEastOfOrigin(20_000);
    var passenger = vertexEastOfOrigin(1_000);
    var trip = new RoutableCarpoolTrip(
      createTripWithStops(
        a0.toWgsCoordinate(),
        List.of(createStopAt(a1.toWgsCoordinate())),
        a2.toWgsCoordinate()
      ),
      List.of(a0, a1, a2),
      new CarpoolCorridor(
        List.of(Duration.ofMinutes(10), Duration.ofMinutes(10)),
        List.of(Duration.ofMinutes(20), Duration.ofMinutes(30)),
        List.of()
      )
    );

    // Reverse tree, a_k -> passenger: the leg's limit minus the drive on to a_(k+1).
    var inward = DefaultCarpoolingService.waypointReach(passenger, List.of(trip), MAX_SPEED, false);
    assertThat(inward.keySet()).containsExactly(a0, a1);
    assertWithinTwoSeconds(Duration.ofMinutes(20).minusSeconds(9_000 / 40), inward.get(a0));
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(19_000 / 40), inward.get(a1));

    // Forward tree, passenger -> a_(k+1): the leg's limit minus the drive from a_k.
    var outward = DefaultCarpoolingService.waypointReach(passenger, List.of(trip), MAX_SPEED, true);
    assertThat(outward.keySet()).containsExactly(a1, a2);
    assertWithinTwoSeconds(Duration.ofMinutes(20).minusSeconds(1_000 / 40), outward.get(a1));
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(9_000 / 40), outward.get(a2));
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
