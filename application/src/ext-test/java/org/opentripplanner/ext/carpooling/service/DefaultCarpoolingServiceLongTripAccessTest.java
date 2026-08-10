package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
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
 * Tests {@link DefaultCarpoolingService#routeAccessEgress} on a driver trip whose end-to-end
 * driving time exceeds the nearby-stop search radius
 * ({@link DefaultCarpoolingService#MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS}, 60
 * minutes). Each leg's tree is sized from OTP's own routed leg duration (see
 * {@link DefaultCarpoolingService#driverLegTreeLimits} and {@code resolveLegDurations}), not from
 * the radius, so the leg's far waypoint stays inside the tree and the trip produces access
 * candidates.
 *
 * <pre>
 *   P  (10 meters N of A)
 *   |
 *   A ===[80 min]=== D
 *   |
 *   S  (10 meters S of A, transit stop)
 * </pre>
 *
 * One straight road east at 10 meters/second: A to D is 48 km, so the carpool drives the single
 * leg A to D in ~80 min, with a 10-min deviation budget at A and D. The access request has the
 * passenger at P, dropped at stop S — both 10 meters from A.
 * <p>
 * The leg tree is sized from the routed baseline: 80 + 1 (slack) + 10 (budget) = 91 min, wider
 * than the 60-min radius. After dropping the passenger at S the carpool still drives on to D, so
 * the S-to-D segment (~80 min) must be routed; it goes only through D's waypoint tree (inserted
 * passenger segments have no goal-directed fallback). A tree capped at the 60-min radius could not
 * span it — S sits ~80 min back along the leg from D — so the insertion would be dropped and the
 * trip would yield no access candidates.
 */
class DefaultCarpoolingServiceLongTripAccessTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime SEARCH_TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZONE
  );

  // 10 m/s car speed keeps the arithmetic obvious: seconds == meters / 10.
  private static final float CAR_SPEED_MPS = 10.0f;

  private DefaultCarpoolingService service;
  private CarpoolingServiceTestContext context;
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
          // D is one long leg ~80 min from A — well beyond the 60-min nearby-stop radius.
          var D = intersection("D", ORIGIN.moveEastMeters(48000));

          coordA = A.toWgsCoordinate();
          coordD = D.toWgsCoordinate();

          // Bidirectional, speed-controlled car streets so A <-> D drives in ~80 min.
          street(A, D, 48000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          street(D, A, 48000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);

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

    context = CarpoolingServiceTestContext.of(model);
    service = context.service();
    transitServiceResolver = context.transitServiceResolver();
  }

  @Test
  void findsAccessResultsForTripLongerThanNearbyStopRadius() {
    // Routed leg ~80 min → tree limit 80 + 1 min slack + 10 min budget = 91 min, spanning the
    // A → D baseline.
    var tripStart = SEARCH_TIME.plusMinutes(30);
    var tripEnd = tripStart.plusMinutes(60);
    var trip = CarpoolTripTestData.createSimpleTripWithTimes(coordA, coordD, tripStart, tripEnd);
    context.upsertTrip(trip);

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
      "A carpool trip driving longer than the 60-min nearby-stop radius should still produce " +
        "access candidates"
    );

    int stopSIndex = transitServiceResolver.getStop(stopS.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopSIndex),
      "Access results should include stop S near the passenger origin"
    );
  }
}
