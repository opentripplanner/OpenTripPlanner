package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

/**
 * <pre>
 *   A --- B --- C --- D      the driver's leg is A -> D; S hangs off C, P off B
 *         |     |
 *         P     S
 * </pre>
 */
class CorridorRouterTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);

  private IntersectionVertex a;
  private IntersectionVertex b;
  private IntersectionVertex c;
  private IntersectionVertex d;
  private IntersectionVertex p;
  private IntersectionVertex s;
  private CarpoolTreeStreetRouter passengerRouter;
  private CorridorRouter router;
  private int pathRouterCalls;

  @BeforeEach
  void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          a = intersection("A", ORIGIN);
          b = intersection("B", ORIGIN.moveEastMeters(500));
          c = intersection("C", ORIGIN.moveEastMeters(1000));
          d = intersection("D", ORIGIN.moveEastMeters(1500));
          p = intersection("P", ORIGIN.moveEastMeters(500).moveSouthMeters(300));
          s = intersection("S", ORIGIN.moveEastMeters(1000).moveSouthMeters(300));
          biStreet(a, b, 500);
          biStreet(b, c, 500);
          biStreet(c, d, 500);
          biStreet(b, p, 300);
          biStreet(c, s, 300);
        }
      }
    );
    passengerRouter = new CarpoolTreeStreetRouter();
    passengerRouter.addVertex(p, CarpoolTreeStreetRouter.Direction.BOTH, Duration.ofHours(1));
    var goalDirected = new CarpoolStreetRouter(
      org.opentripplanner.street.service.StreetLimitationParametersService.DEFAULT
    );
    CarpoolRouter countingPathRouter = (from, to) -> {
      pathRouterCalls++;
      return goalDirected.route(from, to);
    };
    router = new CorridorRouter(passengerRouter, p, countingPathRouter);
  }

  @Test
  void answersRegisteredTripSegmentsFromTheCorridorWithoutSearching() {
    beginTrip(router);
    router.register(a, s, 90);
    router.register(s, d, 50);
    assertEquals(3, router.tripSegmentCount());

    var baseline = router.route(a, d);
    var toStop = router.route(a, s);
    var fromStop = router.route(s, d);
    assertNotNull(baseline);
    assertEquals(120, baseline.durationSeconds());
    assertEquals(90, toStop.durationSeconds());
    assertEquals(50, fromStop.durationSeconds());
    assertEquals(0, pathRouterCalls, "durations come from the corridor, no search");
  }

  @Test
  void answersPassengerSegmentsFromThePassengersTrees() {
    beginTrip(router);

    var toPassenger = router.route(a, p);
    var fromPassenger = router.route(p, s);
    assertNotNull(toPassenger, "A -> P via the passenger's reverse tree");
    assertNotNull(fromPassenger, "P -> S via the passenger's forward tree");
    assertEquals(a, toPassenger.from());
    assertEquals(p, toPassenger.to());
    assertEquals(toPassenger.path().getDuration(), toPassenger.durationSeconds());
  }

  @Test
  void pairsNeitherRegisteredNorTouchingThePassengerAreUnroutable() {
    beginTrip(router);
    assertNull(router.route(a, s), "S was not registered on this trip");
    assertNull(router.route(s, d));
  }

  @Test
  void corridorSegmentPathsAreRoutedOnDemandAndMemoised() {
    beginTrip(router);
    var baseline = router.route(a, d);
    assertEquals(0, pathRouterCalls);

    var path = baseline.path();
    assertEquals(1, pathRouterCalls);
    assertSame(a, path.states.getFirst().getVertex());
    assertSame(d, path.states.getLast().getVertex());
    assertSame(path, baseline.path(), "memoised");
    assertEquals(1, pathRouterCalls);
  }

  @Test
  void beginningATripForgetsThePreviousTripsSegments() {
    beginTrip(router);
    router.beginTrip();
    assertEquals(0, router.tripSegmentCount());
    assertNull(router.route(a, d));
  }

  @Test
  void passengerSegmentsComeFromTheTreesEvenWhenRegistered() {
    // A passenger snapped onto a trip's waypoint must not get that trip's corridor segment.
    beginTrip(router);
    router.register(p, d, 1);
    assertNotEquals(1, router.route(p, d).durationSeconds());
  }

  /** Registers the driver's leg A -> D of 120 s. */
  private void beginTrip(CorridorRouter router) {
    router.beginTrip();
    router.register(a, d, 120);
  }

  @Test
  void corridorSegmentWhosePathCannotBeRoutedFailsLoudly() {
    var unroutable = new CorridorRouter(passengerRouter, p, (from, to) -> null);
    beginTrip(unroutable);
    var segment = unroutable.route(a, d);
    assertNotNull(segment);
    assertThrows(IllegalStateException.class, segment::path);
  }
}
