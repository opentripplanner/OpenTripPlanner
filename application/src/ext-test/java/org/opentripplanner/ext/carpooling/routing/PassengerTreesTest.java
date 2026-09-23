package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter.Direction;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class PassengerTreesTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final double MAX_SPEED = 40.0;
  private static final Duration AN_HOUR = Duration.ofHours(1);

  @Test
  void anOutwardTreeSubtractsTheBeelineFromTheLegsStart() {
    var trip = trip(List.of(vertexEastOfOrigin(0), vertexEastOfOrigin(20_000)), 30);
    var passenger = vertexEastOfOrigin(4_000);

    var limit = trees(trip).limit(passenger, Direction.FROM);

    // 4 km at 40 m/s is 100 s.
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(100), limit);
  }

  @Test
  void anInwardTreeSubtractsTheBeelineToTheLegsEnd() {
    var trip = trip(List.of(vertexEastOfOrigin(0), vertexEastOfOrigin(20_000)), 30);
    var passenger = vertexEastOfOrigin(12_000);

    var limit = trees(trip).limit(passenger, Direction.TO);

    // 8 km at 40 m/s is 200 s.
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(200), limit);
  }

  @Test
  void theWidestLegDecidesAndTheLimitIsNeverNegative() {
    var trip = trip(
      List.of(vertexEastOfOrigin(0), vertexEastOfOrigin(1_000), vertexEastOfOrigin(30_000)),
      2,
      40
    );

    // Leg 0: 2 min - 50 s; leg 1: 40 min - 25 s.
    var near = vertexEastOfOrigin(2_000);
    assertWithinTwoSeconds(
      Duration.ofMinutes(40).minusSeconds(25),
      trees(trip).limit(near, Direction.FROM)
    );

    var far = vertexEastOfOrigin(200_000);
    assertEquals(Duration.ZERO, trees(trip).limit(far, Direction.FROM));
  }

  private static PassengerTrees trees(RoutableCarpoolTrip trip) {
    return new PassengerTrees(List.of(trip), MAX_SPEED, AN_HOUR);
  }

  /** A trip through the given stops, each leg allowed the given number of minutes. */
  private static RoutableCarpoolTrip trip(List<Vertex> stops, int... legLimitMinutes) {
    var legLimits = Arrays.stream(legLimitMinutes).mapToObj(Duration::ofMinutes).toList();
    var intermediate = stops
      .subList(1, stops.size() - 1)
      .stream()
      .map(stop -> createStopAt(stop.toWgsCoordinate()))
      .toList();
    return new RoutableCarpoolTrip(
      createTripWithStops(
        stops.getFirst().toWgsCoordinate(),
        intermediate,
        stops.getLast().toWgsCoordinate()
      ),
      stops,
      new CarpoolCorridor(legLimits, legLimits, List.of())
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
