package org.opentripplanner.ext.carpooling.routing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolStreetPathBuilder.createStreetPath;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_MIDPOINT_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithCapacity;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithDeviationBudget;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.collection.Pair;

class InsertionEvaluatorTest {

  /** Every beeline takes no time at this speed, so the beeline pre-check rules out no leg. */
  private static final double INFINITE_SPEED = Double.POSITIVE_INFINITY;

  private Map<WgsCoordinate, Vertex> vertexMap;

  @BeforeEach
  void setup() {
    vertexMap = new HashMap<>();

    for (WgsCoordinate coord : List.of(
      OSLO_CENTER,
      OSLO_EAST,
      OSLO_NORTH,
      OSLO_SOUTH,
      OSLO_WEST,
      OSLO_MIDPOINT_NORTH,
      OSLO_NORTHEAST,
      getCoordinateBetween(OSLO_SOUTH, OSLO_CENTER),
      getCoordinateBetween(OSLO_NORTHEAST, OSLO_NORTH)
    )) {
      var vertex = new SimpleVertex(
        "test-" + coord.latitude() + "-" + coord.longitude(),
        coord.latitude(),
        coord.longitude()
      );
      vertexMap.put(coord, vertex);
    }
  }

  private RoutableCarpoolTrip createRoutableTrip(CarpoolTrip trip) {
    var vertices = trip
      .stops()
      .stream()
      .map(stop -> vertexMap.get(stop.getCoordinate()))
      .toList();
    return RoutableCarpoolTripTestData.withVertices(trip, vertices);
  }

  private WgsCoordinate getCoordinate(Vertex vertex) {
    return new WgsCoordinate(vertex.getCoordinate().y, vertex.getCoordinate().x);
  }

  /**
   * Evaluates every insertion of the passenger into the trip and returns the best.
   */
  private InsertionCandidate findOptimalInsertion(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    CarpoolRouter carpoolRouter
  ) {
    return findOptimalInsertion(
      Duration.ZERO,
      trip,
      passengerPickup,
      passengerDropoff,
      carpoolRouter
    );
  }

  /** The same with {@code dwell} at every stop the driver makes, the passenger's included. */
  private InsertionCandidate findOptimalInsertion(
    Duration dwell,
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    CarpoolRouter carpoolRouter
  ) {
    var routableTrip = createRoutableTrip(trip);
    var evaluator = new InsertionEvaluator(carpoolRouter, dwell, INFINITE_SPEED);
    return evaluator.findBestInsertion(
      routableTrip,
      new PassengerSnap(vertexMap.get(passengerPickup), vertexMap.get(passengerDropoff), null, null)
    );
  }

  private WgsCoordinate getCoordinateBetween(WgsCoordinate coordinate1, WgsCoordinate coordinate2) {
    return new WgsCoordinate(
      (coordinate1.latitude() + coordinate2.latitude()) / 2,
      (coordinate1.longitude() + coordinate2.longitude()) / 2
    );
  }

  /** Routers answer {@code null} for an unroutable pair; a path is wrapped as a segment. */
  @Nullable
  private static RoutedSegment segmentOrNull(@Nullable StreetPath path) {
    return path == null ? null : RoutedSegment.of(path);
  }

  @Test
  void findOptimalInsertion_onDeviationBudgetExceeded_returnsNull() {
    var deviationBudget = Duration.ofMinutes(5);
    var trip = createTripWithStops(
      OSLO_SOUTH,
      List.of(
        createStopAt(OSLO_CENTER, deviationBudget),
        createStopAt(OSLO_NORTHEAST, deviationBudget)
      ),
      OSLO_NORTH,
      deviationBudget
    );

    var mockPath = createStreetPath(Duration.ofMinutes(4));
    CarpoolRouter routingFunction = (from, to) -> RoutedSegment.of(mockPath);

    var result = findOptimalInsertion(
      trip,
      getCoordinateBetween(OSLO_SOUTH, OSLO_CENTER),
      getCoordinateBetween(OSLO_NORTHEAST, OSLO_NORTH),
      routingFunction
    );

    assertNull(result);
  }

  @Test
  void findOptimalInsertion_noValidPositions_returnsNull() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);
    // Routing function returns null (simulating routing failure)
    // This causes evaluator to skip all positions
    CarpoolRouter routingFunction = (from, to) -> null;

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNull(result);
  }

  @Test
  void findOptimalInsertion_oneValidPosition_returnsCandidate() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);

    var mockPath = createStreetPath();

    CarpoolRouter routingFunction = (from, to) -> RoutedSegment.of(mockPath);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNotNull(result);
    assertEquals(1, result.pickupPosition());
    assertEquals(2, result.dropoffPosition());
  }

  @Test
  void noPathIsBuiltWhileEvaluating() {
    CarpoolRouter router = (from, to) ->
      new RoutedSegment() {
        @Override
        public int durationSeconds() {
          return 300;
        }

        @Override
        public StreetPath path() {
          throw new AssertionError("a path was built while evaluating");
        }
      };
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);

    assertNotNull(findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, router));
  }

  @Test
  void findOptimalInsertion_routingFails_skipsPosition() {
    // Use a trip with one stop to have multiple viable insertion positions
    var stop1 = createStopAt(OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    var mockPath = createStreetPath(Duration.ofMinutes(3));

    @SuppressWarnings("ConstantConditions")
    CarpoolRouter routingFunction = (from, to) -> {
      if (
        getCoordinate(from).equals(OSLO_CENTER) && getCoordinate(to).equals(OSLO_MIDPOINT_NORTH)
      ) {
        return null;
      } else {
        return RoutedSegment.of(mockPath);
      }
    };

    // Use passenger coordinates that are compatible with trip direction (CENTER->EAST->NORTH)
    var result = findOptimalInsertion(trip, OSLO_MIDPOINT_NORTH, OSLO_NORTHEAST, routingFunction);

    // Should skip failed routing and find a valid one
    assertNotNull(result);
    /*
      Since segment between OSLO_CENTER and OSLO_MIDPOINT_NORTH is invalid,
      pickup position has to be after OSLO_EAST
     */
    assertEquals(2, result.pickupPosition());
  }

  @Test
  void findOptimalInsertion_exceedsDeviationBudget_returnsNull() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(5), OSLO_CENTER, OSLO_NORTH);

    // Create routing that results in excessive additional time
    // Baseline is 2 segments * 5 min = 10 min
    // Modified route is 3 segments * 20 min = 60 min
    // Additional = 50 min, exceeds 5 min budget
    var mockPath = createStreetPath(Duration.ofMinutes(20));

    CarpoolRouter routingFunction = (from, to) -> RoutedSegment.of(mockPath);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    // Should not return candidate that exceeds budget
    assertNull(result);
  }

  @Test
  void findOptimalInsertion_tripWithStops_evaluatesAllPositions() {
    var stop1 = createStopAt(OSLO_EAST);
    var stop2 = createStopAt(OSLO_WEST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    var mockPath = createStreetPath();

    CarpoolRouter routingFunction = (from, to) -> RoutedSegment.of(mockPath);

    assertDoesNotThrow(() ->
      findOptimalInsertion(trip, OSLO_MIDPOINT_NORTH, OSLO_NORTHEAST, routingFunction)
    );
  }

  @Test
  void findOptimalInsertion_baselineDurationCalculationFails_returnsNull() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);

    CarpoolRouter routingFunction = (from, to) -> null;

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNull(result);
  }

  /**
   * Given two viable insertion positions with different total trip durations, the evaluator should
   * select the one with the shorter total.
   * <pre>
   * Trip: SOUTH → CENTER → NORTH (baseline: 10 + 10 = 20 min)
   * Passenger: pickup at EAST, dropoff at WEST
   *
   * Position (1,2) modified route: SOUTH → EAST → WEST → CENTER → NORTH
   *   segments: 8 + 4 + 9 + 10(reused) = 31 min
   *
   * Position (2,3) modified route: SOUTH → CENTER → EAST → WEST → NORTH
   *   segments: 10(reused) + 3 + 4 + 5 = 22 min  ← shorter, should be selected
   * </pre>
   */
  @Test
  void findBestInsertion_selectsShorterTotalTripDuration() {
    var stop = createStopAt(OSLO_CENTER, Duration.ofMinutes(30));
    var trip = createTripWithStops(OSLO_SOUTH, List.of(stop), OSLO_NORTH, Duration.ofMinutes(30));
    var routableTrip = createRoutableTrip(trip);

    final Map<Pair<WgsCoordinate>, StreetPath> pathsMap = new HashMap<>(
      Map.of(
        // Baseline segments
        new Pair<>(OSLO_SOUTH, OSLO_CENTER),
        createStreetPath(Duration.ofMinutes(10)),
        new Pair<>(OSLO_CENTER, OSLO_NORTH),
        createStreetPath(Duration.ofMinutes(10)),
        // Position (1,2) new segments
        new Pair<>(OSLO_SOUTH, OSLO_EAST),
        createStreetPath(Duration.ofMinutes(8)),
        new Pair<>(OSLO_EAST, OSLO_WEST),
        createStreetPath(Duration.ofMinutes(4)),
        new Pair<>(OSLO_WEST, OSLO_CENTER),
        createStreetPath(Duration.ofMinutes(9)),
        // Position (2,3) new segments
        new Pair<>(OSLO_CENTER, OSLO_EAST),
        createStreetPath(Duration.ofMinutes(3)),
        new Pair<>(OSLO_WEST, OSLO_NORTH),
        createStreetPath(Duration.ofMinutes(5))
      )
    );

    @SuppressWarnings("ConstantConditions")
    CarpoolRouter routingFunction = (from, to) ->
      segmentOrNull(pathsMap.get(new Pair<>(getCoordinate(from), getCoordinate(to))));

    var evaluator = new InsertionEvaluator(routingFunction, Duration.ZERO, INFINITE_SPEED);
    var result = evaluator.findBestInsertion(
      routableTrip,
      new PassengerSnap(vertexMap.get(OSLO_EAST), vertexMap.get(OSLO_WEST), null, null)
    );

    assertNotNull(result);
    assertEquals(2, result.pickupPosition());
    assertEquals(3, result.dropoffPosition());
    assertEquals(Duration.ofMinutes(22), result.totalTripDuration());
  }

  @Test
  void findOptimalInsertion_simpleTrip_hasExpectedStructure() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);

    var mockPath = createStreetPath();

    CarpoolRouter routingFunction = (from, to) -> RoutedSegment.of(mockPath);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNotNull(result);
    assertNotNull(result.trip());
    assertNotNull(result.routeSegments());
    assertFalse(result.routeSegments().isEmpty());
    assertTrue(result.pickupPosition() >= 0);
    assertTrue(result.dropoffPosition() > result.pickupPosition());
  }

  @Test
  void findOptimalInsertion_pickupAtExistingPoint_handlesCorrectly() {
    // Scenario: Trip A→B→C, passenger pickup at B (existing point), dropoff at new point
    // Expected: Segment A→B should be reused, B→dropoff and dropoff→C should be routed

    var stop1 = createStopAt(OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTHEAST);

    var mockPath = createStreetPath(Duration.ofMinutes(3));

    final int[] callCount = { 0 };
    CarpoolRouter carpoolRouter = (from, to) -> {
      callCount[0]++;
      return RoutedSegment.of(mockPath);
    };

    // Pickup exactly at OSLO_EAST (existing stop), dropoff at OSLO_NORTH (new)
    // OSLO_NORTH is directly on the way from OSLO_EAST to OSLO_NORTHEAST (same longitude as OSLO_EAST)
    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_NORTH, carpoolRouter);

    assertNotNull(result, "Should find valid insertion");

    // Modified route should have new segments
    assertTrue(result.routeSegments().size() >= 2);

    // Routing should be called for baseline and new segments
    assertTrue(callCount[0] >= 2, "Should have called routing at least 2 times");
  }

  @Test
  void legsTheBeelineRulesOutAreNotRouted() {
    // Three legs; the passenger sits 30 km off the route. The second stop allows one minute: at
    // 30 m/s the beeline detour through either leg before it is far more than that, so nothing is
    // routed into or out of them, while the last leg, before the destination's hour, is routed.
    var origin = new WgsCoordinate(63.43, 10.39);
    var first = origin.moveEastMeters(200);
    var second = origin.moveEastMeters(400);
    var end = origin.moveEastMeters(600);
    var farNorth = origin.moveNorthMeters(30_000);
    for (var c : List.of(origin, first, second, end, farNorth)) {
      vertexMap.put(c, new SimpleVertex("v-" + c, c.latitude(), c.longitude()));
    }
    var trip = createTripWithStops(
      origin,
      List.of(
        createStopAt(first, Duration.ofMinutes(60)),
        createStopAt(second, Duration.ofMinutes(1))
      ),
      end,
      Duration.ofMinutes(60)
    );
    var routed = new ArrayList<Pair<WgsCoordinate>>();
    CarpoolRouter recording = (from, to) -> {
      routed.add(new Pair<>(getCoordinate(from), getCoordinate(to)));
      return RoutedSegment.of(createStreetPath(Duration.ofMinutes(3)));
    };
    var evaluator = new InsertionEvaluator(recording, Duration.ZERO, 30.0);

    evaluator.findBestInsertion(
      createRoutableTrip(trip),
      new PassengerSnap(vertexMap.get(farNorth), vertexMap.get(farNorth), null, null)
    );

    assertThat(routed).doesNotContain(new Pair<>(origin, farNorth));
    assertThat(routed).doesNotContain(new Pair<>(first, farNorth));
    assertThat(routed).contains(new Pair<>(second, farNorth));
  }

  @Test
  void singleLegTrip_routesTheBaselineAndTheThreeNewSegmentsOnly() {
    // With pickup and dropoff in the only leg, the pickup's way out of the leg and the dropoff's
    // way into it are never needed, so they are not routed.
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);
    var routed = new ArrayList<Pair<WgsCoordinate>>();
    CarpoolRouter recording = (from, to) -> {
      routed.add(new Pair<>(getCoordinate(from), getCoordinate(to)));
      return RoutedSegment.of(createStreetPath(Duration.ofMinutes(5)));
    };

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, recording);

    assertNotNull(result);
    assertThat(routed).containsExactly(
      new Pair<>(OSLO_CENTER, OSLO_NORTH),
      new Pair<>(OSLO_CENTER, OSLO_EAST),
      new Pair<>(OSLO_WEST, OSLO_NORTH),
      new Pair<>(OSLO_EAST, OSLO_WEST)
    );
  }

  @Test
  void theRideIsRoutedOncePerRequest() {
    var rides = new int[] { 0 };
    CarpoolRouter router = (from, to) -> {
      if (getCoordinate(from).equals(OSLO_EAST) && getCoordinate(to).equals(OSLO_WEST)) {
        rides[0]++;
      }
      return RoutedSegment.of(createStreetPath());
    };
    var evaluator = new InsertionEvaluator(router, Duration.ZERO, INFINITE_SPEED);
    var snap = new PassengerSnap(vertexMap.get(OSLO_EAST), vertexMap.get(OSLO_WEST), null, null);
    var tripA = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);
    var tripB = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_SOUTH, OSLO_NORTHEAST);

    assertNotNull(evaluator.findBestInsertion(createRoutableTrip(tripA), snap));
    assertNotNull(evaluator.findBestInsertion(createRoutableTrip(tripB), snap));

    assertEquals(1, rides[0], "pickup -> dropoff does not depend on the trip");
  }

  @Test
  void thePassengerRidesOnlyLegsWithAFreeSeat() {
    // Room for two, both taken on the first leg: pickup and dropoff fit only into the second.
    var trip = createTripWithCapacity(
      2,
      List.of(
        createStopAt(2, OSLO_SOUTH),
        createStopAt(1, OSLO_CENTER),
        createStopAt(0, OSLO_NORTH)
      )
    );
    CarpoolRouter router = (from, to) -> RoutedSegment.of(createStreetPath());

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, router);

    assertNotNull(result);
    assertEquals(2, result.pickupPosition());
    assertEquals(3, result.dropoffPosition());
  }

  @Test
  void aRideAcrossLegsNeedsAFreeSeatOnEveryLeg() {
    // Room for two on SOUTH -> CENTER -> NORTH. The passenger can only be picked up in the first
    // leg and dropped off in the second.
    var router = drives(
      drive(OSLO_SOUTH, OSLO_CENTER, 10),
      drive(OSLO_CENTER, OSLO_NORTH, 10),
      drive(OSLO_SOUTH, OSLO_EAST, 6),
      drive(OSLO_EAST, OSLO_CENTER, 6),
      drive(OSLO_CENTER, OSLO_WEST, 6),
      drive(OSLO_WEST, OSLO_NORTH, 6)
    );
    var oneOnBoard = createTripWithCapacity(
      2,
      List.of(
        createStopAt(1, OSLO_SOUTH),
        createStopAt(1, OSLO_CENTER),
        createStopAt(0, OSLO_NORTH)
      )
    );
    var fullOnTheSecondLeg = createTripWithCapacity(
      2,
      List.of(
        createStopAt(1, OSLO_SOUTH),
        createStopAt(2, OSLO_CENTER),
        createStopAt(0, OSLO_NORTH)
      )
    );

    assertNotNull(findOptimalInsertion(oneOnBoard, OSLO_EAST, OSLO_WEST, router));
    assertNull(
      findOptimalInsertion(fullOnTheSecondLeg, OSLO_EAST, OSLO_WEST, router),
      "the car is full on the second leg"
    );
  }

  @Test
  void aDetourMustFitTheBudgetOfEveryStopItDelays() {
    // Every segment takes 5 minutes, so inserting the passenger adds 10. The intermediate stop
    // allows 1 minute and the destination 30: the passenger fits only after the intermediate stop.
    var stop = createStopAt(OSLO_CENTER, Duration.ofMinutes(1));
    var trip = createTripWithStops(OSLO_SOUTH, List.of(stop), OSLO_NORTH, Duration.ofMinutes(30));
    CarpoolRouter router = (from, to) -> RoutedSegment.of(createStreetPath());

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, router);

    assertNotNull(result);
    assertEquals(2, result.pickupPosition());
    assertEquals(3, result.dropoffPosition());
  }

  @Test
  void acrossLegsThePickupDetourMustFitTheStopsUpToTheDropoff() {
    var result = insertAcrossLegs(Duration.ofMinutes(2), Duration.ofMinutes(4));

    assertNotNull(result);
    assertEquals(1, result.pickupPosition());
    assertEquals(3, result.dropoffPosition());
    assertNull(
      insertAcrossLegs(Duration.ofMinutes(1), Duration.ofMinutes(30)),
      "CENTER allows less than the pickup detour"
    );
  }

  @Test
  void acrossLegsBothDetoursMustFitTheStopsAfterTheDropoff() {
    assertNull(
      insertAcrossLegs(Duration.ofMinutes(30), Duration.ofMinutes(3)),
      "NORTH allows less than both detours"
    );
  }

  @Test
  void theDwellAtThePickupAndTheDropoffCountsAgainstTheBudget() {
    // SOUTH -> NORTH in ten minutes, five minutes of budget. Picking the passenger up at EAST and
    // dropping them off at WEST adds four minutes of driving, and a dwell at each of the two stops.
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(5), OSLO_SOUTH, OSLO_NORTH);
    var router = drives(
      drive(OSLO_SOUTH, OSLO_NORTH, 10),
      drive(OSLO_SOUTH, OSLO_EAST, 6),
      drive(OSLO_EAST, OSLO_WEST, 2),
      drive(OSLO_WEST, OSLO_NORTH, 6)
    );

    assertNotNull(findOptimalInsertion(Duration.ZERO, trip, OSLO_EAST, OSLO_WEST, router));
    assertNull(
      findOptimalInsertion(Duration.ofMinutes(1), trip, OSLO_EAST, OSLO_WEST, router),
      "four minutes of driving and two of dwell exceed the budget"
    );
  }

  @Test
  void acrossLegsTheDwellCountsInBothDetours() {
    assertNotNull(insertAcrossLegs(Duration.ofMinutes(30), Duration.ofMinutes(5), Duration.ZERO));
    assertNull(
      insertAcrossLegs(Duration.ofMinutes(30), Duration.ofMinutes(5), Duration.ofMinutes(1)),
      "two detours of two minutes, each with a minute of dwell, exceed NORTH's budget"
    );
  }

  @Test
  void aDropoffLegThatCannotBeRoutedDoesNotEndTheSearch() {
    // SOUTH -> CENTER -> NORTHEAST -> NORTH, ten minutes a leg; the dropoff cannot be reached in
    // the middle leg.
    var trip = createTripWithStops(
      OSLO_SOUTH,
      List.of(createStopAt(OSLO_CENTER), createStopAt(OSLO_NORTHEAST)),
      OSLO_NORTH
    );
    var router = drives(
      drive(OSLO_SOUTH, OSLO_CENTER, 10),
      drive(OSLO_CENTER, OSLO_NORTHEAST, 10),
      drive(OSLO_NORTHEAST, OSLO_NORTH, 10),
      drive(OSLO_SOUTH, OSLO_EAST, 6),
      drive(OSLO_EAST, OSLO_CENTER, 6),
      drive(OSLO_NORTHEAST, OSLO_WEST, 6),
      drive(OSLO_WEST, OSLO_NORTH, 6)
    );

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, router);

    assertNotNull(result);
    assertEquals(1, result.pickupPosition());
    assertEquals(4, result.dropoffPosition());
  }

  /**
   * Trip SOUTH -> CENTER -> NORTH, ten minutes a leg. The passenger can only be picked up in the
   * first leg and dropped off in the second, each detour adding two minutes.
   */
  @Nullable
  private InsertionCandidate insertAcrossLegs(Duration budgetAtCenter, Duration budgetAtNorth) {
    return insertAcrossLegs(budgetAtCenter, budgetAtNorth, Duration.ZERO);
  }

  /** The same with {@code dwell} at every stop the driver makes, the passenger's included. */
  @Nullable
  private InsertionCandidate insertAcrossLegs(
    Duration budgetAtCenter,
    Duration budgetAtNorth,
    Duration dwell
  ) {
    var trip = createTripWithStops(
      OSLO_SOUTH,
      List.of(createStopAt(OSLO_CENTER, budgetAtCenter)),
      OSLO_NORTH,
      budgetAtNorth
    );
    var router = drives(
      drive(OSLO_SOUTH, OSLO_CENTER, 10),
      drive(OSLO_CENTER, OSLO_NORTH, 10),
      drive(OSLO_SOUTH, OSLO_EAST, 6),
      drive(OSLO_EAST, OSLO_CENTER, 6),
      drive(OSLO_CENTER, OSLO_WEST, 6),
      drive(OSLO_WEST, OSLO_NORTH, 6)
    );
    return findOptimalInsertion(dwell, trip, OSLO_EAST, OSLO_WEST, router);
  }

  /** A router that knows only the given drives. */
  @SafeVarargs
  private CarpoolRouter drives(Map.Entry<Pair<WgsCoordinate>, Duration>... drives) {
    var known = Map.ofEntries(drives);
    return (from, to) -> {
      var duration = known.get(new Pair<>(getCoordinate(from), getCoordinate(to)));
      return duration == null ? null : RoutedSegment.of(createStreetPath(duration));
    };
  }

  private static Map.Entry<Pair<WgsCoordinate>, Duration> drive(
    WgsCoordinate from,
    WgsCoordinate to,
    int minutes
  ) {
    return Map.entry(new Pair<>(from, to), Duration.ofMinutes(minutes));
  }
}
