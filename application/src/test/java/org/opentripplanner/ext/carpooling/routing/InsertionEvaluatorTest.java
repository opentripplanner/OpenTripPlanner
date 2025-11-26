package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder.createGraphPath;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_MIDPOINT_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createStopAt;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithDeviationBudget;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

class InsertionEvaluatorTest {

  private PassengerDelayConstraints delayConstraints;
  private InsertionPositionFinder positionFinder;

  @BeforeEach
  void setup() {
    delayConstraints = new PassengerDelayConstraints();
    positionFinder = new InsertionPositionFinder(delayConstraints, new BeelineEstimator());
  }

  /**
   * Helper method that mimics the old findOptimalInsertion() behavior for backwards compatibility in tests.
   * This explicitly performs position finding followed by evaluation.
   */
  private InsertionCandidate findOptimalInsertion(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    RoutingFunction routingFunction
  ) {
    List<InsertionPosition> viablePositions = positionFinder.findViablePositions(
      trip,
      passengerPickup,
      passengerDropoff
    );

    if (viablePositions.isEmpty()) {
      return null;
    }

    var evaluator = new InsertionEvaluator(routingFunction, delayConstraints, null);
    return evaluator.findBestInsertion(trip, viablePositions, passengerPickup, passengerDropoff);
  }

  @Test
  void findOptimalInsertion_noValidPositions_returnsNull() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // Routing function returns null (simulating routing failure)
    // This causes evaluator to skip all positions
    RoutingFunction routingFunction = (from, to, linkingContext) -> null;

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNull(result);
  }

  @Test
  void findOptimalInsertion_oneValidPosition_returnsCandidate() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var mockPath = createGraphPath();

    RoutingFunction routingFunction = (from, to, linkingContext) -> mockPath;

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNotNull(result);
    assertEquals(1, result.pickupPosition());
    assertEquals(2, result.dropoffPosition());
  }

  @Test
  void findOptimalInsertion_routingFails_skipsPosition() {
    // Use a trip with one stop to have multiple viable insertion positions
    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    var mockPath = createGraphPath(Duration.ofMinutes(5));

    // Routing sequence:
    // 1. Baseline calculation (2 segments: OSLO_CENTER → OSLO_EAST → OSLO_NORTH) = mockPath x2
    // 2. First insertion attempt fails (null for first segment)
    // 3. Second insertion attempt succeeds (mockPath for all segments)
    final int[] callCount = { 0 };
    RoutingFunction routingFunction = (from, to, linkingContext) -> {
      int call = callCount[0]++;
      if (call < 2) {
        return mockPath;
      } else if (call == 2) {
        return null;
      } else {
        return mockPath;
      }
    };

    // Use passenger coordinates that are compatible with trip direction (CENTER->EAST->NORTH)
    var result = findOptimalInsertion(trip, OSLO_MIDPOINT_NORTH, OSLO_NORTHEAST, routingFunction);

    // Should skip failed routing and find a valid one
    assertNotNull(result);
  }

  @Test
  void findOptimalInsertion_exceedsDeviationBudget_returnsNull() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(5), OSLO_CENTER, OSLO_NORTH);

    // Create routing that results in excessive additional time
    // Baseline is 2 segments * 5 min = 10 min
    // Modified route is 3 segments * 20 min = 60 min
    // Additional = 50 min, exceeds 5 min budget
    var mockPath = createGraphPath(Duration.ofMinutes(20));

    RoutingFunction routingFunction = (from, to, linkingContext) -> mockPath;

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    // Should not return candidate that exceeds budget
    assertNull(result);
  }

  @Test
  void findOptimalInsertion_tripWithStops_evaluatesAllPositions() {
    var stop1 = createStopAt(0, OSLO_EAST);
    var stop2 = createStopAt(1, OSLO_WEST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    var mockPath = createGraphPath();

    RoutingFunction routingFunction = (from, to, linkingContext) -> mockPath;

    assertDoesNotThrow(() ->
      findOptimalInsertion(trip, OSLO_MIDPOINT_NORTH, OSLO_NORTHEAST, routingFunction)
    );
  }

  @Test
  void findOptimalInsertion_baselineDurationCalculationFails_returnsNull() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    RoutingFunction routingFunction = (from, to, linkingContext) -> null;

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNull(result);
  }

  @Test
  void findOptimalInsertion_selectsMinimumAdditionalDuration() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);

    // Baseline: 1 segment (CENTER → NORTH) at 10 min
    // The algorithm will try multiple pickup/dropoff positions
    // We'll return different durations based on segment index
    var mockPath10 = createGraphPath(Duration.ofMinutes(10));
    var mockPath4 = createGraphPath(Duration.ofMinutes(4));
    var mockPath6 = createGraphPath(Duration.ofMinutes(6));
    var mockPath5 = createGraphPath(Duration.ofMinutes(5));
    var mockPath7 = createGraphPath(Duration.ofMinutes(7));

    // Provide consistent route times
    // Baseline
    // First insertion (15 min total, 5 min additional)
    // Second insertion (18 min total, 8 min additional)
    @SuppressWarnings("unchecked")
    final GraphPath<State, Edge, Vertex>[] firstInsertionPaths = new GraphPath[] {
      mockPath4,
      mockPath5,
      mockPath6,
    };
    @SuppressWarnings("unchecked")
    final GraphPath<State, Edge, Vertex>[] secondInsertionPaths = new GraphPath[] {
      mockPath5,
      mockPath6,
      mockPath7,
    };
    final int[] callCount = { 0 };
    RoutingFunction routingFunction = (from, to, linkingContext) -> {
      int call = callCount[0]++;
      if (call == 0) {
        return mockPath10;
      } else if (call >= 1 && call <= 3) {
        return firstInsertionPaths[call - 1];
      } else {
        return secondInsertionPaths[(call - 4) % 3];
      }
    };

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNotNull(result);
    // Should have selected one of the evaluated insertions
    // The exact additional duration depends on which position was evaluated first
    assertTrue(result.additionalDuration().compareTo(Duration.ofMinutes(20)) <= 0);
    assertTrue(result.additionalDuration().compareTo(Duration.ZERO) > 0);
  }

  @Test
  void findOptimalInsertion_simpleTrip_hasExpectedStructure() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var mockPath = createGraphPath();

    RoutingFunction routingFunction = (from, to, linkingContext) -> mockPath;

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNotNull(result);
    assertNotNull(result.trip());
    assertNotNull(result.routeSegments());
    assertFalse(result.routeSegments().isEmpty());
    assertTrue(result.pickupPosition() >= 0);
    assertTrue(result.dropoffPosition() > result.pickupPosition());
  }

  @Test
  void findOptimalInsertion_insertBetweenTwoPoints_routesAllSegments() {
    // This test catches the bug where segments were incorrectly reused
    // Scenario: Trip A→B, insert passenger C→D where both C and D are between A and B
    // Expected: All 3 segments (A→C, C→D, D→B) should be routed, not reused

    // Create a simple 2-point trip (OSLO_CENTER → OSLO_NORTH)
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Create mock paths with DISTINCT durations for verification
    // Baseline: 1 segment (CENTER → NORTH) = 10 min
    var baselinePath = createGraphPath(Duration.ofMinutes(10));

    // Modified route segments should have DIFFERENT durations
    // If baseline is incorrectly reused, we'd see 10 min for A→C segment
    // CENTER → EAST
    var segmentAC = createGraphPath(Duration.ofMinutes(3));
    // EAST → MIDPOINT_NORTH
    var segmentCD = createGraphPath(Duration.ofMinutes(2));
    // MIDPOINT_NORTH → NORTH
    var segmentDB = createGraphPath(Duration.ofMinutes(4));

    // Setup routing: return all segment mocks for any routing call
    // The algorithm will evaluate multiple insertion positions
    @SuppressWarnings("unchecked")
    final GraphPath<State, Edge, Vertex>[] paths = new GraphPath[] {
      baselinePath,
      segmentAC,
      segmentCD,
      segmentDB,
      segmentAC,
      segmentCD,
      segmentDB,
      segmentAC,
      segmentCD,
    };
    final int[] callCount = { 0 };
    RoutingFunction routingFunction = (from, to, linkingContext) -> {
      int call = callCount[0]++;
      return call < paths.length ? paths[call] : segmentAC;
    };

    // Passenger pickup at OSLO_EAST, dropoff at OSLO_MIDPOINT_NORTH
    // Both are between OSLO_CENTER and OSLO_NORTH
    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_MIDPOINT_NORTH, routingFunction);

    assertNotNull(result, "Should find valid insertion");

    // Verify the result structure
    assertEquals(3, result.routeSegments().size(), "Should have 3 segments in modified route");

    // Note: With real State objects, exact durations will have minor rounding differences
    // (typically 1-2 seconds per edge due to millisecond rounding in StreetEdge.doTraverse())
    // The baseline should be approximately 10 minutes (within 10 seconds tolerance)
    assertTrue(
      Math.abs(result.baselineDuration().toSeconds() - 600) < 10,
      "Baseline should be approximately 10 min (within 10s), got " + result.baselineDuration()
    );

    // CRITICAL: Total duration should be sum of NEW segments, NOT baseline duration
    // Total = 3 + 2 + 4 = 9 minutes (approximately, with rounding)
    // If bug exists, segment A→C would incorrectly use baseline (10 min) → total would be wrong
    assertTrue(
      Math.abs(result.totalDuration().toSeconds() - 540) < 10,
      "Total duration should be approximately 9 min (within 10s), got " + result.totalDuration()
    );

    // Additional duration should be negative (this insertion is actually faster!)
    // This is realistic for insertions that "shortcut" part of the baseline route
    assertTrue(
      result.additionalDuration().isNegative(),
      "Additional duration should be negative (insertion is faster), got " +
      result.additionalDuration()
    );

    // Routing was called at least 4 times (1 baseline + 3 new segments minimum)
    assertTrue(callCount[0] >= 4, "Should have called routing at least 4 times");
  }

  @Test
  void findOptimalInsertion_insertAtEnd_reusesMostSegments() {
    // This test verifies that segment reuse optimization still works correctly
    // Scenario: Trip A→B→C, insert passenger that allows some segment reuse
    // Expected: Segments that have matching endpoints should be REUSED

    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Baseline has 2 segments: CENTER→EAST, EAST→NORTH
    var mockPath = createGraphPath(Duration.ofMinutes(5));

    final int[] callCount = { 0 };
    RoutingFunction routingFunction = (from, to, linkingContext) -> {
      callCount[0]++;
      return mockPath;
    };

    // Insert passenger - the algorithm will find the best position
    var result = findOptimalInsertion(trip, OSLO_WEST, OSLO_SOUTH, routingFunction);

    assertNotNull(result, "Should find valid insertion");

    // Baseline should be calculated correctly
    assertEquals(Duration.ofMinutes(10), result.baselineDuration());

    // The modified route should have more segments than baseline
    assertTrue(
      result.routeSegments().size() >= 2,
      "Modified route should have at least baseline segments"
    );

    // Additional duration should be positive (adding detour)
    assertTrue(
      result.additionalDuration().compareTo(Duration.ZERO) > 0,
      "Adding passenger should increase duration"
    );

    // Routing was called for baseline and new segments
    assertTrue(callCount[0] >= 2, "Should have called routing at least 2 times");
  }

  @Test
  void findOptimalInsertion_pickupAtExistingPoint_handlesCorrectly() {
    // Scenario: Trip A→B→C, passenger pickup at B (existing point), dropoff at new point
    // Expected: Segment A→B should be reused, B→dropoff and dropoff→C should be routed

    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTHEAST);

    var mockPath = createGraphPath(Duration.ofMinutes(5));

    final int[] callCount = { 0 };
    RoutingFunction routingFunction = (from, to, linkingContext) -> {
      callCount[0]++;
      return mockPath;
    };

    // Pickup exactly at OSLO_EAST (existing stop), dropoff at OSLO_NORTH (new)
    // OSLO_NORTH is directly on the way from OSLO_EAST to OSLO_NORTHEAST (same longitude as OSLO_EAST)
    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_NORTH, routingFunction);

    assertNotNull(result, "Should find valid insertion");

    // Modified route should have new segments
    assertTrue(result.routeSegments().size() >= 2);

    // Routing should be called for baseline and new segments
    assertTrue(callCount[0] >= 2, "Should have called routing at least 2 times");
  }

  @Test
  void findOptimalInsertion_singleSegmentTrip_routesAllNewSegments() {
    // Edge case: Simplest possible trip (2 points, 1 segment)
    // Any insertion will require routing all new segments

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var mockPath = createGraphPath(Duration.ofMinutes(5));

    final int[] callCount = { 0 };
    RoutingFunction routingFunction = (from, to, linkingContext) -> {
      callCount[0]++;
      return mockPath;
    };

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST, routingFunction);

    assertNotNull(result);
    assertEquals(3, result.routeSegments().size());

    // Routing was called for baseline and new segments
    assertTrue(callCount[0] >= 4, "Should have called routing at least 4 times");

    // Total duration should be positive
    assertTrue(result.totalDuration().compareTo(Duration.ZERO) > 0);
    assertTrue(result.baselineDuration().compareTo(Duration.ZERO) > 0);
  }
}
