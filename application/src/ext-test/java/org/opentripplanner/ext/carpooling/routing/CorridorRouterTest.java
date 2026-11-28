package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor.CorridorStop;
import org.opentripplanner.ext.carpooling.util.StreetPathUtils;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * <pre>
 *   A --- B --- C --- D      the driver's leg is A -> D; S hangs off C, P off B
 *         |     |
 *         P     S
 * </pre>
 */
class CorridorRouterTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);
  /** A stop on the leg, dropped off at 90 s + 50 s, picked up at 80 s + 40 s. */
  private static final CorridorStop STOP = new CorridorStop(
    FeedScopedId.ofNullable("TEST", "S"),
    0,
    90,
    50,
    80,
    40
  );

  private IntersectionVertex a;
  private IntersectionVertex b;
  private IntersectionVertex c;
  private IntersectionVertex d;
  private IntersectionVertex p;
  private IntersectionVertex s;
  private IntersectionVertex unconnected;
  private CorridorRouter router;
  private int legSeconds;

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
          unconnected = intersection("U", ORIGIN.moveNorthMeters(500));
        }
      }
    );
    var passengerRouter = new CarpoolTreeStreetRouter();
    passengerRouter.addVertex(p, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(1));
    passengerRouter.addVertex(p, CarpoolTreeStreetRouter.Direction.TO, Duration.ofHours(1));
    var pathRouter = new CarpoolStreetRouter(
      StreetLimitationParametersService.DEFAULT,
      CarpoolingParameters.DEFAULT.maxTripDuration()
    );
    router = new CorridorRouter(passengerRouter, List.of(p), pathRouter);
    legSeconds = (int) CarpoolTreeStreetRouter.carTree(a, false, Duration.ofHours(1), null)
      .getState(d)
      .getElapsedTimeSeconds();
  }

  @Test
  void registeredTripSegmentsComeFromTheCorridor() {
    router.beginTrip(trip(a, d, legSeconds));
    router.registerStop(STOP, s, true);

    var baseline = router.route(a, d);
    var toStop = router.route(a, s);
    var fromStop = router.route(s, d);
    assertNotNull(baseline);
    assertEquals(legSeconds, baseline.durationSeconds());
    assertEquals(90, toStop.durationSeconds());
    assertEquals(50, fromStop.durationSeconds());
  }

  @Test
  void aPickupGetsTheStopsPickupTimes() {
    router.beginTrip(trip(a, d, legSeconds));
    router.registerStop(STOP, s, false);

    assertEquals(80, router.route(a, s).durationSeconds());
    assertEquals(40, router.route(s, d).durationSeconds());
  }

  @Test
  void segmentsTouchingThePassengerComeFromThePassengersTrees() {
    router.beginTrip(trip(a, d, legSeconds));

    assertNotNull(router.route(a, p), "A -> P via the passenger's reverse tree");
    assertNotNull(router.route(p, s), "P -> S via the passenger's forward tree");
  }

  @Test
  void pairsNeitherRegisteredNorTouchingThePassengerAreUnroutable() {
    router.beginTrip(trip(a, d, legSeconds));
    assertNull(router.route(a, s), "S was not registered on this trip");
    assertNull(router.route(s, d));
  }

  @Test
  void aCorridorSegmentsPathRunsBetweenItsEnds() {
    router.beginTrip(trip(a, d, legSeconds));
    var baseline = router.route(a, d);

    var path = baseline.path();
    assertSame(a, StreetPathUtils.firstVertex(path));
    assertSame(d, StreetPathUtils.lastVertex(path));
    assertEquals(legSeconds, path.duration().toSeconds());
    assertSame(path, baseline.path(), "kept");
  }

  @Test
  void beginningATripForgetsThePreviousTripsSegments() {
    router.beginTrip(trip(a, d, legSeconds));
    router.beginTrip(trip(b, c, legSeconds));
    assertNull(router.route(a, d));
  }

  @Test
  void aRegisteredSegmentWinsOverThePassengersTrees() {
    router.beginTrip(trip(p, d, 7200));
    assertEquals(7200, router.route(p, d).durationSeconds());
  }

  @Test
  void aSegmentThatCannotBeDrivenFailsLoudly() {
    router.beginTrip(trip(a, unconnected, 100));
    assertThrows(IllegalStateException.class, () -> router.route(a, unconnected).path());
  }

  /** A one-leg trip, {@code from} to {@code to}, that its corridor says takes {@code seconds}. */
  private static RoutableCarpoolTrip trip(Vertex from, Vertex to, int seconds) {
    var leg = List.of(Duration.ofSeconds(seconds));
    return new RoutableCarpoolTrip(
      createSimpleTrip(from.toWgsCoordinate(), to.toWgsCoordinate()),
      new CarpoolCorridor(leg, leg, List.of(), List.of(from, to), 40.0)
    );
  }
}
