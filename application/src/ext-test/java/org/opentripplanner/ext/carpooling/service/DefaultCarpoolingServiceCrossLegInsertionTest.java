package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
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
 * Regression test for {@link DefaultCarpoolingService#routeAccessEgress} on a cross-leg insertion
 * — pickup inserted on one leg of a multi-stop driver trip, dropoff on a later leg — where the
 * drive from the passenger's pickup to the next driver waypoint exceeds the nearby-stop search
 * radius ({@link DefaultCarpoolingService#MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS},
 * 60 minutes).
 * <p>
 * Every segment from/to the passenger in a modified route lies on a single leg of the trip, so its
 * drive is bounded by that leg's duration plus detour allowance. The passenger vertex's trees must
 * therefore span the longest candidate leg limit — a fixed cap silently drops budget-feasible
 * insertions whose {@code passenger → next waypoint} segment is longer than the cap, because
 * {@code route()} never falls back to the waypoint's correctly sized reverse tree while the
 * passenger has a forward tree of its own.
 *
 * <pre>
 * Graph (one-way driver legs, distances/speeds chosen so legs are 70 and 60 min):
 *
 *   P (10 m N of A)              S (10 m S of D, transit stop)
 *    \                          /
 *     A ==========> X ========> M   one-way A → M via X, 37 km + 5 km (61.7 + 8.3 min)
 *     A &lt;-- 6 km local road --&gt; D   bidirectional (10 min)
 *               M ===========> D    one-way M → D, 36 km (60 min)
 *
 *   Driver trip: A → M → D. The route bends back: D is only 6 km from A, while the
 *   driver goes out to M and returns. Deviation budget 10 min at M and D.
 *
 *   Access request: passenger at P. The only stop, S, sits on leg M → D, 10 car-minutes
 *   from P via the local road — within the nearby-stop radius.
 *
 *   The only budget-feasible insertion is cross-leg: pickup P on leg A → M, dropoff S on
 *   leg M → D (route A → P → M → S → D, detour ≈ two dwells). Dropping S on the first leg
 *   instead would force S → M via the local road and A (80 min vs 70 scheduled), breaching
 *   the 10-min budget at M. The cross-leg insertion's passenger → M segment is a 70-min
 *   drive, beyond the 60-min nearby-stop radius — reachable only because the passenger
 *   trees are sized to the largest candidate leg limit (1.5 × 70 + 10 = 115 min).
 *
 *   X matters because {@code DurationSkipEdgeStrategy} prunes edges by the elapsed time at
 *   the edge's from-state, overshooting a tree's limit by one edge: a single 42-km A → M
 *   edge would be traversed from an elapsed time of ~0 and defeat any limit. With X at
 *   61.7 min, a 60-min tree reaches X but prunes X → M, while the 115-min tree spans the leg.
 * </pre>
 */
class DefaultCarpoolingServiceCrossLegInsertionTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime SEARCH_TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZONE
  );

  // 10 m/s car speed keeps the arithmetic obvious: seconds == metres / 10.
  private static final float CAR_SPEED_MPS = 10.0f;
  private static final Duration BUDGET = Duration.ofMinutes(10);

  private DefaultCarpoolingService service;
  private CarpoolingRepository repository;
  private TransitServiceResolver transitServiceResolver;

  private TransitStopVertex stopS;
  private WgsCoordinate coordA;
  private WgsCoordinate coordM;
  private WgsCoordinate coordD;
  private WgsCoordinate coordP;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          var X = intersection("X", ORIGIN.moveEastMeters(37000));
          var M = intersection("M", ORIGIN.moveEastMeters(42000));
          var D = intersection("D", ORIGIN.moveEastMeters(6000));

          coordA = A.toWgsCoordinate();
          coordM = M.toWgsCoordinate();
          coordD = D.toWgsCoordinate();

          // One-way driver legs: out to M via X, back past A's area to D. X sits beyond the
          // 60-min mark so a tree capped there prunes X → M and cannot reach M (see class doc).
          street(A, X, 37000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          street(X, M, 5000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          street(M, D, 36000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          // Bidirectional local road tying A's area to D's, so the stop search from P reaches S
          // and an early dropoff of S has a (budget-breaching) way back to M.
          street(A, D, 6000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);
          street(D, A, 6000, StreetTraversalPermission.ALL, CAR_SPEED_MPS);

          // Passenger spur just north of A and a transit stop spur just south of D, both on the
          // drivable network so pickup/dropoff need no walking.
          var iP = intersection("iP", ORIGIN.moveNorthMeters(10));
          biStreet(A, iP, 10);
          coordP = iP.toWgsCoordinate();

          var iS = intersection("iS", ORIGIN.moveEastMeters(6000).moveSouthMeters(10));
          biStreet(D, iS, 10);
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
  void findsCrossLegInsertionWithPassengerSegmentLongerThanNearbyStopRadius() {
    var tripStart = SEARCH_TIME.plusMinutes(30);
    repository.upsertCarpoolTrip(trip(tripStart));

    var request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(coordP.latitude(), coordP.longitude()))
      .withTo(GenericLocation.fromCoordinate(coordM.latitude(), coordM.longitude()))
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
      "A budget-feasible cross-leg insertion whose passenger → waypoint segment exceeds the " +
        "nearby-stop radius should produce access candidates; an empty result means the " +
        "passenger trees are capped below the largest candidate leg limit."
    );

    int stopSIndex = transitServiceResolver.getStop(stopS.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopSIndex),
      "Access results should include stop S on the trip's second leg"
    );
  }

  /**
   * A → M → D with scheduled legs of 70 and 60 minutes, matching the street network's actual
   * driving times, and a 10-minute deviation budget at M and D.
   */
  private CarpoolTrip trip(ZonedDateTime tripStart) {
    var origin = CarpoolStop.of(stopId("A"))
      .withCoordinate(coordA)
      .withOnboardCount(1)
      .withDeviationBudget(Duration.ZERO)
      .build();
    var intermediate = CarpoolStop.of(stopId("M"))
      .withCoordinate(coordM)
      .withOnboardCount(1)
      .withExpectedArrivalTime(tripStart.plusMinutes(70))
      .withDeviationBudget(BUDGET)
      .build();
    var destination = CarpoolStop.of(stopId("D"))
      .withCoordinate(coordD)
      .withOnboardCount(1)
      .withExpectedArrivalTime(tripStart.plusMinutes(130))
      .withDeviationBudget(BUDGET)
      .build();
    return new CarpoolTripBuilder(FeedScopedId.ofNullable("TEST", "trip-bent-route"))
      .withStops(List.of(origin, intermediate, destination))
      .withTotalCapacity(CarpoolTrip.DEFAULT_TOTAL_CAPACITY)
      .withStartTime(tripStart)
      .withEndTime(tripStart.plusMinutes(130))
      .build();
  }

  private static FeedScopedId stopId(String id) {
    return FeedScopedId.ofNullable("TEST", "stop-" + id);
  }
}
