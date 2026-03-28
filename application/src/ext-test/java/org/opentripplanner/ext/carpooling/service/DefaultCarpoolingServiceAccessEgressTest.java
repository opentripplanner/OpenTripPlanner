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
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter;
import org.opentripplanner.graph_builder.module.nearbystops.SiteRepositoryResolver;
import org.opentripplanner.graph_builder.module.nearbystops.StopResolver;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Integration tests for {@link DefaultCarpoolingService#routeAccessEgress}.
 * <p>
 * These tests use a real street graph with transit stops to verify the full
 * access/egress routing pipeline including nearby stop finding, insertion
 * evaluation, and result mapping.
 * <p>
 * Graph layout (going east):
 * <pre>
 *                     T1     P2      T3
 *                    / \     / \     / \
 *   P1 ---------- A ----- B ----- C ----- D ---------- P3
 *                    \ /             \ /
 *                    T2              T4
 *
 *   Carpool trip: A -> (B -> C) -> D  (2 or 4 stops)
 *   Transit stops: T1..T4
 *     T1 = north of A-B midpoint (connected to A and B)
 *     T2 = south of A-B midpoint (connected to A and B)
 *     T3 = north of C-D midpoint (connected to C and D)
 *     T4 = south of C-D midpoint (connected to C and D)
 *   Passenger locations:
 *     P1 = 50km west of A (connected to A)
 *     P2 = between B and C, north (connected to B and C)
 *     P3 = 50km east of D (connected to D)
 *   Access tests: passenger P2 -> P3
 *   Egress tests: passenger P1 -> P2
 * </pre>
 */
class DefaultCarpoolingServiceAccessEgressTest extends GraphRoutingTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(59.9139, 10.7522);
  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime SEARCH_TIME = LocalDateTime.of(2025, 6, 15, 12, 0).atZone(
    ZONE
  );

  private DefaultCarpoolingService service;
  private CarpoolingRepository repository;
  private StopResolver stopResolver;
  private LinkingContext linkingContext;

  private TransitStopVertex stopT1;
  private TransitStopVertex stopT2;
  private TransitStopVertex stopT3;
  private TransitStopVertex stopT4;

  // Carpool trip waypoints
  private WgsCoordinate coordA;
  private WgsCoordinate coordB;
  private WgsCoordinate coordC;
  private WgsCoordinate coordD;

  // Passenger locations
  private WgsCoordinate coordP1;
  private WgsCoordinate coordP2;
  private WgsCoordinate coordP3;

  @BeforeEach
  void setUp() {
    var model = modelOf(
      new GraphRoutingTest.Builder() {
        @Override
        public void build() {
          // Main road intersections going east, same as DirectTest
          var A = intersection("A", ORIGIN);
          var B = intersection("B", ORIGIN.moveEastMeters(500));
          var C = intersection("C", ORIGIN.moveEastMeters(1500));
          var D = intersection("D", ORIGIN.moveEastMeters(2000));

          coordA = A.toWgsCoordinate();
          coordB = B.toWgsCoordinate();
          coordC = C.toWgsCoordinate();
          coordD = D.toWgsCoordinate();

          // Main road streets
          biStreet(A, B, 500);
          biStreet(B, C, 1000);
          biStreet(C, D, 500);

          // Transit stop intersections
          var iT1 = intersection("iT1", ORIGIN.moveEastMeters(250).moveNorthMeters(200));
          var iT2 = intersection("iT2", ORIGIN.moveEastMeters(250).moveSouthMeters(200));
          var iT3 = intersection("iT3", ORIGIN.moveEastMeters(1750).moveNorthMeters(200));
          var iT4 = intersection("iT4", ORIGIN.moveEastMeters(1750).moveSouthMeters(200));

          // Connect transit stop intersections to main road
          biStreet(A, iT1, 320);
          biStreet(B, iT1, 320);
          biStreet(A, iT2, 320);
          biStreet(B, iT2, 320);
          biStreet(C, iT3, 320);
          biStreet(D, iT3, 320);
          biStreet(C, iT4, 320);
          biStreet(D, iT4, 320);

          // Transit stops at each transit intersection
          stopT1 = stop("T1", iT1.toWgsCoordinate());
          stopT2 = stop("T2", iT2.toWgsCoordinate());
          stopT3 = stop("T3", iT3.toWgsCoordinate());
          stopT4 = stop("T4", iT4.toWgsCoordinate());

          biLink(iT1, stopT1);
          biLink(iT2, stopT2);
          biLink(iT3, stopT3);
          biLink(iT4, stopT4);

          // Passenger locations
          var iP1 = intersection("P1", ORIGIN.moveWestMeters(50000));
          biStreet(iP1, A, 50000);

          var iP2 = intersection("P2", ORIGIN.moveEastMeters(1000).moveNorthMeters(200));
          biStreet(B, iP2, 539);
          biStreet(C, iP2, 539);

          var iP3 = intersection("P3", ORIGIN.moveEastMeters(52000));
          biStreet(D, iP3, 50000);

          // Passenger coordinates
          coordP1 = iP1.toWgsCoordinate();
          coordP2 = iP2.toWgsCoordinate();
          coordP3 = iP3.toWgsCoordinate();

          // Store vertices for linking context and independent routing
          DefaultCarpoolingServiceAccessEgressTest.this.vertexA = A;
          DefaultCarpoolingServiceAccessEgressTest.this.vertexP1 = iP1;
          DefaultCarpoolingServiceAccessEgressTest.this.vertexP2 = iP2;
          DefaultCarpoolingServiceAccessEgressTest.this.vertexP3 = iP3;
          DefaultCarpoolingServiceAccessEgressTest.this.vertexIT3 = iT3;
          DefaultCarpoolingServiceAccessEgressTest.this.vertexIT4 = iT4;
        }
      }
    );

    Graph graph = model.graph();
    var timetableRepository = model.timetableRepository();
    stopResolver = new SiteRepositoryResolver(timetableRepository.getSiteRepository());
    VertexLinker vertexLinker = VertexLinkerTestFactory.of(graph);
    TransitService transitService = new DefaultTransitService(timetableRepository);
    repository = new DefaultCarpoolingRepository();

    // LinkingContext: P1 -> {iP1}, P2 -> {iP2}, P3 -> {iP3}
    var p1Location = GenericLocation.fromCoordinate(coordP1.latitude(), coordP1.longitude());
    var p2Location = GenericLocation.fromCoordinate(coordP2.latitude(), coordP2.longitude());
    var p3Location = GenericLocation.fromCoordinate(coordP3.latitude(), coordP3.longitude());
    linkingContext = new LinkingContext(
      Map.of(
        p1Location,
        Set.<Vertex>of(vertexP1),
        p2Location,
        Set.<Vertex>of(vertexP2),
        p3Location,
        Set.<Vertex>of(vertexP3)
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

  private IntersectionVertex vertexA;
  private IntersectionVertex vertexP1;
  private IntersectionVertex vertexP2;
  private IntersectionVertex vertexP3;
  private IntersectionVertex vertexIT3;
  private IntersectionVertex vertexIT4;

  private RouteRequest buildCarpoolRequest(
    WgsCoordinate from,
    WgsCoordinate to,
    ZonedDateTime dateTime
  ) {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(from.latitude(), from.longitude()))
      .withTo(GenericLocation.fromCoordinate(to.latitude(), to.longitude()))
      .withDateTime(dateTime.toInstant())
      .withJourney(j ->
        j
          .withAccess(new StreetRequest(StreetMode.CARPOOL))
          .withEgress(new StreetRequest(StreetMode.CARPOOL))
      )
      .buildRequest();
  }

  @Test
  void returnsEmptyWhenAccessModeIsNotCarpool() {
    var request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(coordP2.latitude(), coordP2.longitude()))
      .withTo(GenericLocation.fromCoordinate(coordP3.latitude(), coordP3.longitude()))
      .withJourney(j -> j.withAccess(new StreetRequest(StreetMode.WALK)))
      .buildRequest();

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.WALK),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertTrue(results.isEmpty());
  }

  @Test
  void returnsEmptyWhenEgressModeIsNotCarpool() {
    var request = RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(coordP1.latitude(), coordP1.longitude()))
      .withTo(GenericLocation.fromCoordinate(coordP2.latitude(), coordP2.longitude()))
      .withJourney(j -> j.withEgress(new StreetRequest(StreetMode.WALK)))
      .buildRequest();

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.WALK),
      AccessEgressType.EGRESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertTrue(results.isEmpty());
  }

  @Test
  void returnsEmptyWhenNoCarpoolTripsInRepository() {
    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertTrue(results.isEmpty());
  }

  @Test
  void returnsEmptyWhenTripsFailTimeFilter() {
    var pastTime = SEARCH_TIME.minusDays(30);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, pastTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertTrue(results.isEmpty());
  }

  @Test
  void findsAccessResultsForCompatibleTrip() {
    var departureTime = SEARCH_TIME.plusMinutes(30);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, departureTime);
    repository.upsertCarpoolTrip(trip);

    // Access test: passenger at P2 going to P3
    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertFalse(results.isEmpty(), "Should find access results for a compatible trip");

    for (CarpoolAccessEgress accessEgress : results) {
      assertTrue(accessEgress.durationInSeconds() > 0, "Duration should be positive");
      assertTrue(accessEgress.hasOpeningHours(), "Carpool access should have opening hours");
      assertFalse(accessEgress.isWalkOnly(), "Carpool access should not be walk-only");
      assertNotNull(accessEgress.getSegments(), "Segments should not be null");
      assertFalse(accessEgress.getSegments().isEmpty(), "Segments should not be empty");
    }

    int stopT3Index = stopResolver.getRegularStop(stopT3.getId()).getIndex();
    int stopT4Index = stopResolver.getRegularStop(stopT4.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT3Index),
      "At least one access result should include stopT3"
    );
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT4Index),
      "At least one access result should include stopT4"
    );
  }

  @Test
  void findsEgressResultsForCompatibleTrip() {
    var departureTime = SEARCH_TIME.plusMinutes(30);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, departureTime);
    repository.upsertCarpoolTrip(trip);

    // Egress test: passenger at P1 going to P2
    var request = buildCarpoolRequest(coordP1, coordP2, SEARCH_TIME);

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.EGRESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertFalse(results.isEmpty(), "Should find egress results for a compatible trip");

    for (CarpoolAccessEgress accessEgress : results) {
      assertTrue(accessEgress.durationInSeconds() > 0, "Duration should be positive");
      assertNotNull(accessEgress.getSegments(), "Segments should not be null");
    }

    int stopT1Index = stopResolver.getRegularStop(stopT1.getId()).getIndex();
    int stopT2Index = stopResolver.getRegularStop(stopT2.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT1Index),
      "At least one egress result should include stopT1"
    );
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT2Index),
      "At least one egress result should include stopT2"
    );
  }

  @Test
  void accessResultsHaveMatchingArrivalDepartureAndDuration() {
    var departureTime = SEARCH_TIME.plusMinutes(30);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, departureTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);
    var transitSearchTimeZero = SEARCH_TIME;

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      transitSearchTimeZero
    );

    assertFalse(results.isEmpty(), "Should find access results for a compatible trip");

    for (CarpoolAccessEgress accessEgress : results) {
      int departure = accessEgress.getDepartureTimeOfPassenger();
      int arrival = accessEgress.getArrivalTimeOfPassenger();

      assertTrue(
        departure >= 0,
        "Departure time should be non-negative relative to search time zero"
      );
      assertTrue(
        arrival > departure,
        "Arrival (" + arrival + ") should be after departure (" + departure + ")"
      );
      assertEquals(
        arrival - departure,
        accessEgress.durationInSeconds(),
        "Duration should equal arrival - departure"
      );
    }

    int stopT3Index = stopResolver.getRegularStop(stopT3.getId()).getIndex();
    int stopT4Index = stopResolver.getRegularStop(stopT4.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT3Index),
      "At least one access result should include stopT3"
    );
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT4Index),
      "At least one access result should include stopT4"
    );
  }

  @Test
  void twoTripsReturnTwoResultsPerStop() {
    var departureTime1 = SEARCH_TIME.plusMinutes(30);
    var departureTime2 = SEARCH_TIME.plusMinutes(60);

    var trip1 = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, departureTime1);
    var trip2 = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, departureTime2);

    repository.upsertCarpoolTrip(trip1);
    repository.upsertCarpoolTrip(trip2);

    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertFalse(results.isEmpty(), "Should find results from multiple trips");

    var resultsByStop = results.stream().collect(Collectors.groupingBy(CarpoolAccessEgress::stop));

    for (var entry : resultsByStop.entrySet()) {
      assertEquals(
        2,
        entry.getValue().size(),
        "Stop " + entry.getKey() + " should have exactly 2 results (one per trip)"
      );
    }

    int stopT3Index = stopResolver.getRegularStop(stopT3.getId()).getIndex();
    int stopT4Index = stopResolver.getRegularStop(stopT4.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT3Index),
      "At least one access result should include stopT3"
    );
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT4Index),
      "At least one access result should include stopT4"
    );
  }

  @Test
  void tripWithIntermediateStopsProducesResults() {
    var departureTime = SEARCH_TIME.plusMinutes(30);
    var tripWithTime = CarpoolTripTestData.createTripWithTime(
      departureTime,
      4,
      List.of(
        CarpoolTripTestData.createOriginStopWithTime(coordA, departureTime, departureTime),
        CarpoolTripTestData.createStopAt(1, coordB),
        CarpoolTripTestData.createStopAt(2, coordC),
        CarpoolTripTestData.createDestinationStopWithTime(
          coordD,
          3,
          departureTime.plusHours(1),
          departureTime.plusHours(1)
        )
      )
    );

    repository.upsertCarpoolTrip(tripWithTime);

    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    assertFalse(results.isEmpty(), "Trip with intermediate stops should produce results");

    int stopT3Index = stopResolver.getRegularStop(stopT3.getId()).getIndex();
    int stopT4Index = stopResolver.getRegularStop(stopT4.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT3Index),
      "At least one access result should include stopT3"
    );
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT4Index),
      "At least one access result should include stopT4"
    );
  }

  @Test
  void earliestDepartureTimeRespectsRequestedDepartureTime() {
    var departureTime = SEARCH_TIME.plusMinutes(30);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, departureTime);
    repository.upsertCarpoolTrip(trip);

    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      SEARCH_TIME
    );

    for (CarpoolAccessEgress accessEgress : results) {
      int depTime = accessEgress.getDepartureTimeOfPassenger();

      int earliest = accessEgress.earliestDepartureTime(0);
      assertEquals(depTime, earliest, "Earliest departure should be the carpool departure time");

      int tooLate = accessEgress.earliestDepartureTime(depTime + 1);
      assertTrue(
        tooLate < 0,
        "Should return TIME_NOT_SET when requested departure is after carpool departure"
      );
    }

    int stopT3Index = stopResolver.getRegularStop(stopT3.getId()).getIndex();
    int stopT4Index = stopResolver.getRegularStop(stopT4.getId()).getIndex();
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT3Index),
      "At least one access result should include stopT3"
    );
    assertTrue(
      results.stream().anyMatch(r -> r.stop() == stopT4Index),
      "At least one access result should include stopT4"
    );
  }

  @Test
  void accessDepartureAndArrivalTimesMatchIndependentRouting() {
    var departureTime = SEARCH_TIME.plusMinutes(30);
    var trip = CarpoolTripTestData.createSimpleTripWithTime(coordA, coordD, departureTime);
    repository.upsertCarpoolTrip(trip);

    // Independently compute driving times:
    // - A to P2 (for passenger departure time)
    // - P2 to each transit stop (for passenger arrival time, since the carpool detours via P2)
    var router = new CarpoolTreeStreetRouter();
    router.addVertex(vertexA, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(2));
    router.addVertex(vertexP2, CarpoolTreeStreetRouter.Direction.FROM, Duration.ofHours(2));

    var pathAToP2 = router.route(vertexA, vertexP2);
    assertNotNull(pathAToP2, "Should be able to route from A to P2");
    var drivingDurationAToP2 = Duration.between(
      pathAToP2.states.getFirst().getTime(),
      pathAToP2.states.getLast().getTime()
    );
    assertTrue(drivingDurationAToP2.toSeconds() > 1, "Driving duration to P2 should be positive");

    var pathP2ToIT3 = router.route(vertexP2, vertexIT3);
    assertNotNull(pathP2ToIT3, "Should be able to route from P2 to iT3");
    var drivingDurationP2ToIT3 = Duration.between(
      pathP2ToIT3.states.getFirst().getTime(),
      pathP2ToIT3.states.getLast().getTime()
    );
    assertTrue(
      drivingDurationP2ToIT3.toSeconds() > 0,
      "Driving duration from P2 to iT3 should be positive"
    );

    var pathP2ToIT4 = router.route(vertexP2, vertexIT4);
    assertNotNull(pathP2ToIT4, "Should be able to route from P2 to iT4");
    var drivingDurationP2ToIT4 = Duration.between(
      pathP2ToIT4.states.getFirst().getTime(),
      pathP2ToIT4.states.getLast().getTime()
    );
    assertTrue(
      drivingDurationP2ToIT4.toSeconds() > 0,
      "Driving duration from P2 to iT4 should be positive"
    );

    var request = buildCarpoolRequest(coordP2, coordP3, SEARCH_TIME);
    var transitSearchTimeZero = SEARCH_TIME;

    var results = service.routeAccessEgress(
      request,
      new StreetRequest(StreetMode.CARPOOL),
      AccessEgressType.ACCESS,
      stopResolver,
      linkingContext,
      transitSearchTimeZero
    );

    assertFalse(results.isEmpty(), "Should find access results");

    var expectedDeparture = (int) Duration.between(
      transitSearchTimeZero.toInstant(),
      departureTime.plus(drivingDurationAToP2).toInstant()
    ).getSeconds();

    int stopT3Index = stopResolver.getRegularStop(stopT3.getId()).getIndex();
    int stopT4Index = stopResolver.getRegularStop(stopT4.getId()).getIndex();
    var targetStopIndices = Set.of(stopT3Index, stopT4Index);

    Map<Integer, Duration> expectedDrivingP2ToStop = Map.of(
      stopT3Index,
      drivingDurationP2ToIT3,
      stopT4Index,
      drivingDurationP2ToIT4
    );

    var filteredResults = results
      .stream()
      .filter(r -> targetStopIndices.contains(r.stop()))
      .toList();
    assertEquals(
      2,
      filteredResults.size(),
      "Should have exactly 2 results: one for T3 and one for T4"
    );
    assertTrue(
      filteredResults.stream().anyMatch(r -> r.stop() == stopT3Index),
      "Should have a result for T3"
    );
    assertTrue(
      filteredResults.stream().anyMatch(r -> r.stop() == stopT4Index),
      "Should have a result for T4"
    );

    for (CarpoolAccessEgress accessEgress : filteredResults) {
      assertEquals(
        expectedDeparture,
        accessEgress.getDepartureTimeOfPassenger(),
        "Departure time should equal trip start plus driving time from A to P2"
      );

      var drivingP2ToStop = expectedDrivingP2ToStop.get(accessEgress.stop());
      // The service adds CARPOOL_STOP_DURATION (1 min) for the passenger pickup at P2
      var expectedArrival =
        expectedDeparture +
        (int) drivingP2ToStop.getSeconds() +
        (int) DefaultCarpoolingService.CARPOOL_STOP_DURATION.getSeconds();
      assertEquals(
        expectedArrival,
        accessEgress.getArrivalTimeOfPassenger(),
        "Arrival time should equal passenger departure plus driving time from P2 to stop plus pickup duration"
      );
    }
  }
}
