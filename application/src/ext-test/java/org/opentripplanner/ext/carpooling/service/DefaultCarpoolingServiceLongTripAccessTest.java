package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * Regression test for {@link DefaultCarpoolingService#routeAccessEgress} on a driver trip whose
 * end-to-end driving time exceeds the nearby-stop search radius
 * ({@link DefaultCarpoolingService#MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS}, 60
 * minutes).
 * <p>
 * Access/egress baseline routing builds shortest-path trees rooted at the driver trip's own
 * waypoints. When those trees borrowed the 60-minute nearby-stop radius as their expansion limit,
 * the baseline {@code origin → destination} route of any trip longer than 60 minutes could not be
 * reconstructed: the frontier died at an intermediate vertex before reaching the destination, so
 * the whole trip was silently discarded and produced no access candidates. The trees are now sized
 * per leg from the trip's scheduled timeline (see
 * {@link DefaultCarpoolingService#driverLegTreeLimits}).
 *
 * <pre>
 * Graph (going east, distances/speeds chosen so A → D drives in ~80 min):
 *
 *   P (10 m N of A)        S (10 m S of A, transit stop)
 *    \                    /
 *     A ===== M ========= D
 *      70 min     10 min
 *
 *   Carpool trip: A → D  (single leg, scheduled span 60 min → limit 60 × 1.5 + 10 min budget =
 *   100 min)
 *   A → M takes ~70 min, M → D ~10 min, so the baseline A → D is ~80 min: unreachable under
 *   the old 60-min tree cap (M sits past it and is never expanded to D), reachable under 100 min.
 *   Access request: passenger at P, dropped at stop S.
 * </pre>
 */
class DefaultCarpoolingServiceLongTripAccessTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime SEARCH_TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZONE
  );

  // 10 m/s car speed keeps the arithmetic obvious: seconds == metres / 10.
  private static final float CAR_SPEED_MPS = 10.0f;

  private DefaultCarpoolingService service;
  private CarpoolingRepository repository;
  private TransitServiceResolver transitServiceResolver;

  private TransitStopVertex stopS;
  private WgsCoordinate coordA;
  private WgsCoordinate coordD;
  private WgsCoordinate coordP;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          // M is ~70 min from A, D another ~10 min — so the only path to D runs through a vertex
          // that lies beyond the old 60-min tree cap.
          var M = intersection("M", ORIGIN.moveEastMeters(42000));
          var D = intersection("D", ORIGIN.moveEastMeters(48000));

          coordA = A.toWgsCoordinate();
          coordD = D.toWgsCoordinate();

          // Bidirectional, speed-controlled car streets so A → D drives in ~80 min.
          street(A, M, 42000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          street(M, A, 42000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          street(M, D, 6000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          street(D, M, 6000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);

          // Passenger spur just north of A and a transit stop spur just south of A, both on the
          // drivable network so pickup/dropoff need no walking.
          var iP = intersection("iP", ORIGIN.moveNorthMeters(10));
          biStreet(A, iP, 10);
          coordP = iP.toWgsCoordinate();

          var iS = intersection("iS", ORIGIN.moveSouthMeters(10));
          biStreet(A, iS, 10);
          stopS = stop("S", iS.toWgsCoordinate());
          biLink(iS, stopS);
        }
      }
    );

    var context = CarpoolingServiceTestContext.of(model);
    service = context.service();
    repository = context.repository();
    transitServiceResolver = context.transitServiceResolver();
  }

  @Test
  void findsAccessResultsForTripLongerThanNearbyStopRadius() {
    // Scheduled span is exactly 60 min, so the driver tree limit is 60 × 1.5 + 10 min budget =
    // 100 min — enough to span the ~80-min A → D baseline, which the old 60-min cap could not.
    var tripStart = SEARCH_TIME.plusMinutes(30);
    var tripEnd = tripStart.plusMinutes(60);
    var trip = CarpoolTripTestData.createSimpleTripWithTimes(coordA, coordD, tripStart, tripEnd);
    repository.upsertCarpoolTrip(trip);

    var request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(coordP.latitude(), coordP.longitude()))
      .withTo(GenericLocation.fromCoordinate(coordD.latitude(), coordD.longitude()))
      .withDateTime(SEARCH_TIME.toInstant())
      .withJourney(j -> j.withAccess(new StreetRequest(StreetMode.CARPOOL)))
      .buildRequest();

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      transitServiceResolver,
      SEARCH_TIME
    );

    assertFalse(
      results.isEmpty(),
      "A carpool trip longer than the 60-min nearby-stop radius should still produce access " +
        "candidates; an empty result means the driver-baseline tree was capped at that radius."
    );

    int stopSIndex = transitServiceResolver.getStop(stopS.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopSIndex),
      "Access results should include stop S near the passenger origin"
    );
  }
}
