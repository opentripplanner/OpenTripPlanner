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
 * Tests {@link DefaultCarpoolingService#routeAccessEgress} on a cross-leg insertion — pickup on
 * one leg of a multi-stop driver trip, dropoff on a later leg — whose
 * {@code passenger → next waypoint} drive exceeds the nearby-stop search radius
 * ({@link DefaultCarpoolingService#MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS},
 * 60 minutes). It is found only because the passenger's routing tree is sized to the largest
 * candidate leg limit, not to a fixed cap.
 *
 * <pre>
 *   Right = EAST. The two legs run opposite ways over the same ground, so they are
 *   drawn on two lines; the small vertical gap is NOT north/south.
 *
 *   P
 *   │
 *   A ━━━━━━━━━━━━━━━━━━━━━━━━━━━━▶ X ━▶ M
 *    ╲                                   ┃
 *     D ◀━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 *     │
 *     S
 *
 *   leg 1  A → X → M : one-way EAST, 37 + 5 km, 70 min  (X just W of M, the 61.7-min mark)
 *   leg 2  M → D     : one-way WEST, 36 km, 60 min
 *   A ↔ D            : two-way local road, 6 km, 10 min
 *   A and D are close together; X and M are ~40 km EAST and only ~2 km north.
 *   P = passenger (10 meters N of A)    S = only transit stop (10 meters S of D)
 * </pre>
 *
 * Because the route loops far out to M and then doubles back to D right beside A, a cross-leg
 * insertion is the only feasible one (see below). The 2 km north offset of X and M is only there to
 * keep each waypoint off another street's geometry; every street length is declared (at a flat
 * 10 m/s), so the leg times above are exact. Deviation budget: 10 min at M and D, 0 at A.
 * <p>
 * Access request: passenger at P, dropped at the only stop S. S is 10 car-minutes from P over the
 * local road, so the nearby-stop search (≤ 60 min) finds it easily.
 * <p>
 * The only insertion that fits the budget is cross-leg: pick up P on leg A → M and drop S on leg
 * M → D, giving A → P → M → S → D. Dropping S on the first leg instead (A → P → S → M) would reach
 * M at ~90 min against a 70-min schedule, blowing the 10-min budget.
 * <p>
 * That makes the P → M ride a 70-min drive — longer than the 60-min radius — and it is routed
 * solely through the passenger's own forward tree (inserted passenger segments have no
 * goal-directed fallback). So it routes only because that tree is sized to the largest leg limit:
 * 70 + 1 (slack) + 10 (budget) = 81 min.
 * <p>
 * X is what makes a too-small tree actually fail here: {@code DurationSkipEdgeStrategy} prunes an
 * edge by the elapsed time at its start, so a single 42-km A → M edge would be crossed in one step
 * from ~0 elapsed and defeat any limit. Splitting it at X (61.7 min) lets a 60-min tree reach X but
 * prune X → M, while the 81-min tree spans the whole leg.
 */
class DefaultCarpoolingServiceCrossLegInsertionTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime SEARCH_TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZONE
  );

  // 10 m/s car speed keeps the arithmetic obvious: seconds == meters / 10.
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
          // X and M sit 2 km north of the A–D line. With all vertices collinear, D would lie
          // exactly on the A → X street's geometry, so linking the D driver waypoint would split
          // that street and open a shortcut from D's area onto the corridor — bypassing the
          // one-way layout the test depends on.
          var X = intersection("X", ORIGIN.moveEastMeters(37000).moveNorthMeters(2000));
          var M = intersection("M", ORIGIN.moveEastMeters(42000).moveNorthMeters(2000));
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
        "nearby-stop radius should produce access candidates"
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
