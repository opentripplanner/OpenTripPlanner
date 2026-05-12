package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.filter.DistanceBasedFilter;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Integration tests for {@link DefaultCarpoolingService#routeDirect}.
 * <p>
 * These tests use a real street graph to verify the full direct routing pipeline
 * including filtering, position finding, insertion evaluation, and itinerary mapping.
 * <p>
 * Graph layout (main road going east, P and Q sit south of the road):
 * <pre>
 *           500m         1000m         500m
 *      A ---------- B ----------- C ---------- D
 *      |\          /               \          /
 *      | \        / 255         255 \        / 255
 *      |  \      /                   \      /
 *      |   P ---------- 1400 ---------- Q
 *      |            (P-Q shortcut)    /
 *      +-------------- 1500 ----------+
 *               (direct A-Q bypass)
 *
 *   A = tripStart, D = tripEnd
 *   P = passenger pickup, connected to both A and B (255m each)
 *   Q = passenger dropoff, connected to both C and D (255m each)
 *   P-Q direct shortcut (1400m) beats the main-road P-B-C-Q path (1510m) by 110m,
 *   so the carpool's shared segment (pickup -> dropoff) routes over it.
 *   A-Q direct bypass (1500m) is shorter than A-P-Q (255 + 1400 = 1655m), so the
 *   shortest A->Q path skips the pickup. The carpool itself is still forced to drive
 *   A->P->Q to pick up the passenger; this edge exists so the test cannot rely on
 *   "route tripStart to dropoff directly" as a proxy for what the carpool drives.
 * </pre>
 */
class DefaultCarpoolingServiceDirectTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime SEARCH_TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZONE
  );

  private DefaultCarpoolingService service;
  private CarpoolingRepository repository;
  private LinkingContext linkingContext;

  private WgsCoordinate coordB;
  private WgsCoordinate coordC;

  private WgsCoordinate tripStart;
  private WgsCoordinate tripEnd;

  private WgsCoordinate passengerPickup;
  private WgsCoordinate passengerDropoff;

  private IntersectionVertex vertexTripStart;
  private IntersectionVertex vertexTripEnd;
  private IntersectionVertex vertexPickup;
  private IntersectionVertex vertexDropoff;

  private WgsCoordinate farAwayDropoff;
  private IntersectionVertex vertexFarAway;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          var A = intersection("A", ORIGIN);
          var B = intersection("B", ORIGIN.moveEastMeters(500));
          var C = intersection("C", ORIGIN.moveEastMeters(1500));
          var D = intersection("D", ORIGIN.moveEastMeters(2000));

          var P = intersection("P", ORIGIN.moveEastMeters(250).moveSouthMeters(200));
          var Q = intersection("Q", ORIGIN.moveEastMeters(1750).moveSouthMeters(200));

          var farNorth = Q.toWgsCoordinate().moveNorthMeters(
            DistanceBasedFilter.DEFAULT_MAX_DISTANCE_METERS + 10000
          );
          var F = intersection("F", farNorth);

          coordB = B.toWgsCoordinate();
          coordC = C.toWgsCoordinate();
          tripStart = A.toWgsCoordinate();
          tripEnd = D.toWgsCoordinate();
          passengerPickup = P.toWgsCoordinate();
          passengerDropoff = Q.toWgsCoordinate();
          vertexTripStart = A;
          vertexTripEnd = D;
          vertexPickup = P;
          vertexDropoff = Q;
          farAwayDropoff = F.toWgsCoordinate();
          vertexFarAway = F;

          biStreet(A, B, 500);
          biStreet(B, C, 1000);
          biStreet(C, D, 500);
          biStreet(A, P, 255);
          biStreet(B, P, 255);
          biStreet(C, Q, 255);
          biStreet(D, Q, 255);
          biStreet(P, Q, 1400);
          biStreet(A, Q, 1500);
          biStreet(Q, F, (int) DistanceBasedFilter.DEFAULT_MAX_DISTANCE_METERS + 10000);
        }
      }
    );

    Graph graph = model.graph();
    var timetableRepository = model.timetableRepository();
    VertexLinker vertexLinker = VertexLinkerTestFactory.of(graph);
    TransitService transitService = new DefaultTransitService(timetableRepository);
    repository = new DefaultCarpoolingRepository();

    var pickupLocation = GenericLocation.fromCoordinate(
      passengerPickup.latitude(),
      passengerPickup.longitude()
    );
    var dropoffLocation = GenericLocation.fromCoordinate(
      passengerDropoff.latitude(),
      passengerDropoff.longitude()
    );
    var farAwayLocation = GenericLocation.fromCoordinate(
      farAwayDropoff.latitude(),
      farAwayDropoff.longitude()
    );
    linkingContext = new LinkingContext(
      Map.of(
        pickupLocation,
        Set.<Vertex>of(vertexPickup),
        dropoffLocation,
        Set.<Vertex>of(vertexDropoff),
        farAwayLocation,
        Set.<Vertex>of(vertexFarAway)
      ),
      Collections.emptySet(),
      Collections.emptySet()
    );

    StreetLimitationParametersService streetLimitationParams =
      new StreetLimitationParametersService() {
        @Override
        public float maxCarSpeed() {
          return 40.0f;
        }

        @Override
        public int maxAreaNodes() {
          return 500;
        }
      };

    service = new DefaultCarpoolingService(
      repository,
      streetLimitationParams,
      transitService,
      vertexLinker
    );
  }

  private RouteRequest buildDirectCarpoolRequest(
    WgsCoordinate from,
    WgsCoordinate to,
    ZonedDateTime dateTime
  ) {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(from.latitude(), from.longitude()))
      .withTo(GenericLocation.fromCoordinate(to.latitude(), to.longitude()))
      .withDateTime(dateTime.toInstant())
      .withJourney(j -> j.withDirect(new StreetRequest(StreetMode.CARPOOL)))
      .buildRequest();
  }

  @Test
  void returnsEmptyWhenDirectModeIsNotCarpool() {
    var request = RouteRequest.of()
      .withFrom(
        GenericLocation.fromCoordinate(passengerPickup.latitude(), passengerPickup.longitude())
      )
      .withTo(
        GenericLocation.fromCoordinate(passengerDropoff.latitude(), passengerDropoff.longitude())
      )
      .withJourney(j -> j.withDirect(new StreetRequest(StreetMode.WALK)))
      .buildRequest();

    var results = service.routeDirect(request, linkingContext);

    assertTrue(results.isEmpty());
  }

  @Test
  void returnsEmptyWhenNoCarpoolTripsInRepository() {
    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);

    var results = service.routeDirect(request, linkingContext);

    assertTrue(results.isEmpty());
  }

  @Test
  void returnsEmptyWhenTripsFailTimeFilter() {
    var pastTime = SEARCH_TIME.minusDays(30);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(tripStart, tripEnd, pastTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);

    var results = service.routeDirect(request, linkingContext);

    assertTrue(results.isEmpty());
  }

  @Test
  void findsDirectResultsForCompatibleTrip() {
    var departureTime = SEARCH_TIME.plusMinutes(10);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(tripStart, tripEnd, departureTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);

    var results = service.routeDirect(request, linkingContext);

    assertFalse(results.isEmpty(), "Should find direct results for a compatible trip");

    for (var itinerary : results) {
      assertEquals(1, itinerary.legs().size(), "Carpool itinerary should have exactly one leg");
      assertTrue(
        itinerary.totalDuration().toSeconds() > 0,
        "Itinerary duration should be positive"
      );
    }
  }

  @Test
  void returnsEmptyWhenDropoffExceedsMaxDistance() {
    var departureTime = SEARCH_TIME.plusMinutes(10);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(tripStart, tripEnd, departureTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildDirectCarpoolRequest(passengerPickup, farAwayDropoff, SEARCH_TIME);

    var results = service.routeDirect(request, linkingContext);

    assertTrue(
      results.isEmpty(),
      "Should return no results when dropoff exceeds DEFAULT_MAX_DISTANCE_METERS from trip"
    );
  }

  @Test
  void twoTripsReturnTwoResults() {
    var departureTime1 = SEARCH_TIME.plusMinutes(10);
    var departureTime2 = SEARCH_TIME.plusMinutes(20);

    var trip1 = CarpoolTripTestData.createSimpleTripWithTime(tripStart, tripEnd, departureTime1);
    var trip2 = CarpoolTripTestData.createSimpleTripWithTime(tripStart, tripEnd, departureTime2);

    repository.upsertCarpoolTrip(trip1);
    repository.upsertCarpoolTrip(trip2);

    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);

    var results = service.routeDirect(request, linkingContext);

    assertEquals(2, results.size(), "Should find exactly 2 results for 2 compatible trips");
  }

  @Test
  void tripWithIntermediateStopsProducesResults() {
    var departureTime = SEARCH_TIME.plusMinutes(10);
    var tripWithStops = CarpoolTripTestData.createTripWithTime(
      departureTime,
      4,
      List.of(
        CarpoolTripTestData.createOriginStopWithTime(tripStart, departureTime, departureTime),
        CarpoolTripTestData.createStopAt(coordB),
        CarpoolTripTestData.createStopAt(coordC),
        CarpoolTripTestData.createDestinationStopWithTime(
          tripEnd,
          departureTime.plusHours(1),
          departureTime.plusHours(1)
        )
      )
    );

    repository.upsertCarpoolTrip(tripWithStops);

    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);

    var results = service.routeDirect(request, linkingContext);

    assertFalse(results.isEmpty(), "Trip with intermediate stops should produce results");

    for (var itinerary : results) {
      assertEquals(1, itinerary.legs().size(), "Carpool itinerary should have exactly one leg");
    }
  }

  @Test
  void routeFollowsIntermediateStopsInsteadOfDirectPath() {
    var departureTime = SEARCH_TIME.plusMinutes(10);
    var tripWithStops = CarpoolTripTestData.createTripWithTime(
      departureTime,
      4,
      List.of(
        CarpoolTripTestData.createOriginStopWithTime(tripStart, departureTime, departureTime),
        CarpoolTripTestData.createStopAt(coordB),
        CarpoolTripTestData.createStopAt(coordC),
        CarpoolTripTestData.createDestinationStopWithTime(
          tripEnd,
          departureTime.plusHours(1),
          departureTime.plusHours(1)
        )
      )
    );

    repository.upsertCarpoolTrip(tripWithStops);

    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);

    var results = service.routeDirect(request, linkingContext);

    assertFalse(results.isEmpty(), "Should find results for trip with intermediate stops");
    assertEquals(1, results.size(), "Should find exactly one result");

    var leg = results.getFirst().legs().getFirst();
    var geometry = leg.legGeometry();
    var coordinates = geometry.getCoordinates();

    boolean passesNearB = false;
    boolean passesNearC = false;
    for (var coord : coordinates) {
      // Geometry coordinates are (lon, lat)
      double distToB = coordB.distanceTo(new WgsCoordinate(coord.y, coord.x));
      double distToC = coordC.distanceTo(new WgsCoordinate(coord.y, coord.x));
      if (distToB < 5) {
        passesNearB = true;
      }
      if (distToC < 5) {
        passesNearC = true;
      }
    }

    assertTrue(passesNearB, "Route geometry should pass near intermediate stop B");
    assertTrue(passesNearC, "Route geometry should pass near intermediate stop C");

    // The route through B and C should be longer than the direct beeline distance
    double beelineDistance = passengerPickup.distanceTo(passengerDropoff);
    assertTrue(
      leg.distanceMeters() > beelineDistance,
      "Route distance (" +
        leg.distanceMeters() +
        "m) should exceed beeline distance (" +
        beelineDistance +
        "m)"
    );
  }

  @Test
  void tripDepartingBeforeRequestTimeIsRejected() {
    // TimeBasedFilter uses a one-sided window [T, T+searchWindow]: trips that departed before
    // the passenger's requested time have already left and must be rejected, even if they
    // fall within the old symmetric ±window.
    var departureTime = SEARCH_TIME.minusMinutes(10);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(tripStart, tripEnd, departureTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);
    var results = service.routeDirect(request, linkingContext);

    assertTrue(results.isEmpty(), "Trip departing before request time must be rejected");
  }

  @Test
  void resultItinerariesHaveValidStartAndEndTimes() {
    var departureTime = SEARCH_TIME.plusMinutes(10);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(tripStart, tripEnd, departureTime);
    repository.upsertCarpoolTrip(trip);

    // The carpool is forced to route via the pickup, so we sum the two segments it actually
    // drives (tripStart -> pickup, then pickup -> dropoff) rather than routing tripStart -> dropoff
    // directly. The graph includes an A-Q bypass edge whose shortest path skips the pickup, so a
    // test that used router.route(tripStart, dropoff) here would not match what the carpool
    // drives; this guards against regressing to that shortcut-in-the-test.
    var router = new CarpoolTreeStreetRouter();
    router.addVertex(vertexTripStart, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(2));
    router.addVertex(vertexPickup, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(2));

    var pathToPickup = router.route(vertexTripStart, vertexPickup);
    assertNotNull(pathToPickup, "Should route from trip start to pickup");
    var drivingToPickup = Duration.between(
      pathToPickup.states.getFirst().getTime(),
      pathToPickup.states.getLast().getTime()
    );

    var pathPickupToDropoff = router.route(vertexPickup, vertexDropoff);
    assertNotNull(pathPickupToDropoff, "Should route from pickup to dropoff");
    var drivingPickupToDropoff = Duration.between(
      pathPickupToDropoff.states.getFirst().getTime(),
      pathPickupToDropoff.states.getLast().getTime()
    );

    var request = buildDirectCarpoolRequest(passengerPickup, passengerDropoff, SEARCH_TIME);
    var stopDuration = request.preferences().car().pickupTime();
    // Start time is when the car arrives at the pickup. The boarding dwell is part of the
    // leg's duration, so it is included in the end time rather than before the start.
    var expectedStartTime = departureTime.plus(drivingToPickup);
    var expectedEndTime = expectedStartTime.plus(stopDuration).plus(drivingPickupToDropoff);

    var results = service.routeDirect(request, linkingContext);

    assertFalse(results.isEmpty(), "Should find results");

    for (var itinerary : results) {
      assertEquals(1, itinerary.legs().size(), "Carpool itinerary should have exactly one leg");
      assertNotNull(itinerary.startTime(), "Start time should not be null");
      assertNotNull(itinerary.endTime(), "End time should not be null");
      assertEquals(
        expectedStartTime.toInstant(),
        itinerary.startTime().toInstant(),
        "Start time should equal trip departure plus driving time to pickup (arrival at pickup)"
      );
      assertEquals(
        expectedEndTime.toInstant(),
        itinerary.endTime().toInstant(),
        "End time should equal start time plus boarding dwell plus driving time from pickup " +
          "to dropoff"
      );
    }
  }
}
