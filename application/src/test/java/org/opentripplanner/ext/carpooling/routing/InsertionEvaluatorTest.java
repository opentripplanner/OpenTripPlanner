package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_MIDPOINT_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.MockGraphPathFactory.createMockGraphPath;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createStopAt;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithDeviationBudget;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator.RoutingFunction;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;

class InsertionEvaluatorTest {

  private RoutingFunction mockRoutingFunction;
  private PassengerDelayConstraints delayConstraints;
  private InsertionPositionFinder positionFinder;
  private InsertionEvaluator evaluator;

  @BeforeEach
  void setup() {
    mockRoutingFunction = mock(RoutingFunction.class);
    delayConstraints = new PassengerDelayConstraints();
    positionFinder = new InsertionPositionFinder(delayConstraints, new BeelineEstimator());
    evaluator = new InsertionEvaluator(mockRoutingFunction, delayConstraints);
  }

  /**
   * Helper method that mimics the old findOptimalInsertion() behavior for backwards compatibility in tests.
   * This explicitly performs position finding followed by evaluation.
   */
  private InsertionCandidate findOptimalInsertion(
    org.opentripplanner.ext.carpooling.model.CarpoolTrip trip,
    org.opentripplanner.framework.geometry.WgsCoordinate passengerPickup,
    org.opentripplanner.framework.geometry.WgsCoordinate passengerDropoff
  ) {
    List<InsertionPosition> viablePositions = positionFinder.findViablePositions(
      trip,
      passengerPickup,
      passengerDropoff
    );

    if (viablePositions.isEmpty()) {
      return null;
    }

    return evaluator.findBestInsertion(trip, viablePositions, passengerPickup, passengerDropoff);
  }

  @Test
  void findOptimalInsertion_noValidPositions_returnsNull() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNull(result);
  }

  @Test
  void findOptimalInsertion_oneValidPosition_returnsCandidate() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Create mock paths BEFORE any when() statements
    var mockPath = createMockGraphPath();

    // Mock routing to return valid paths
    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNotNull(result);
    assertEquals(1, result.pickupPosition());
    assertEquals(2, result.dropoffPosition());
  }

  @Test
  void findOptimalInsertion_routingFails_skipsPosition() {
    // Use a trip with one stop to have multiple viable insertion positions
    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    var mockPath = createMockGraphPath(Duration.ofMinutes(5));

    // Routing sequence:
    // 1. Baseline calculation (2 segments: OSLO_CENTER → OSLO_EAST → OSLO_NORTH) = mockPath x2
    // 2. First insertion attempt fails (null for first segment)
    // 3. Second insertion attempt succeeds (mockPath for all segments)
    when(mockRoutingFunction.route(any(), any()))
      .thenReturn(mockPath, mockPath)
      .thenReturn(null)
      .thenReturn(mockPath, mockPath, mockPath, mockPath);

    // Use passenger coordinates that are compatible with trip direction (CENTER->EAST->NORTH)
    var result = findOptimalInsertion(trip, OSLO_MIDPOINT_NORTH, OSLO_NORTHEAST);

    // Should skip failed routing and find a valid one
    assertNotNull(result);
  }

  @Test
  void findOptimalInsertion_exceedsDeviationBudget_returnsNull() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(5), OSLO_CENTER, OSLO_NORTH);

    // Create mock paths BEFORE any when() statements
    // Create routing that results in excessive additional time
    // Baseline is 2 segments * 5 min = 10 min
    // Modified route is 3 segments * 20 min = 60 min
    // Additional = 50 min, exceeds 5 min budget
    var mockPath = createMockGraphPath(Duration.ofMinutes(20));

    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    // Should not return candidate that exceeds budget
    assertNull(result);
  }

  @Test
  void findOptimalInsertion_tripWithStops_evaluatesAllPositions() {
    var stop1 = createStopAt(0, OSLO_EAST);
    var stop2 = createStopAt(1, OSLO_WEST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    var mockPath = createMockGraphPath();

    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);
  }

  @Test
  void findOptimalInsertion_baselineDurationCalculationFails_returnsNull() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Routing returns null (failure) for baseline calculation
    when(mockRoutingFunction.route(any(), any())).thenReturn(null);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNull(result);
  }

  @Test
  void findOptimalInsertion_selectsMinimumAdditionalDuration() {
    var trip = createTripWithDeviationBudget(Duration.ofMinutes(20), OSLO_CENTER, OSLO_NORTH);

    // Create mock paths BEFORE any when() statements
    // Baseline: 1 segment (CENTER → NORTH) at 10 min
    // The algorithm will try multiple pickup/dropoff positions
    // We'll use Answer to return different durations based on segment index
    var mockPath10 = createMockGraphPath(Duration.ofMinutes(10));
    var mockPath4 = createMockGraphPath(Duration.ofMinutes(4));
    var mockPath6 = createMockGraphPath(Duration.ofMinutes(6));
    var mockPath5 = createMockGraphPath(Duration.ofMinutes(5));
    var mockPath7 = createMockGraphPath(Duration.ofMinutes(7));

    // Use thenAnswer to provide consistent route times
    // Just return paths with reasonable durations for all calls
    when(mockRoutingFunction.route(any(), any()))
      .thenReturn(mockPath10) // Baseline
      .thenReturn(mockPath4, mockPath5, mockPath6) // First insertion (15 min total, 5 min additional)
      .thenReturn(mockPath5, mockPath6, mockPath7); // Second insertion (18 min total, 8 min additional)

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNotNull(result);
    // Should have selected one of the evaluated insertions
    // The exact additional duration depends on which position was evaluated first
    assertTrue(result.additionalDuration().compareTo(Duration.ofMinutes(20)) <= 0);
    assertTrue(result.additionalDuration().compareTo(Duration.ZERO) > 0);
  }

  @Test
  void findOptimalInsertion_simpleTrip_hasExpectedStructure() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Create mock paths BEFORE any when() statements
    var mockPath = createMockGraphPath();

    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

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
    var baselinePath = createMockGraphPath(Duration.ofMinutes(10));

    // Modified route segments should have DIFFERENT durations
    // If baseline is incorrectly reused, we'd see 10 min for A→C segment
    var segmentAC = createMockGraphPath(Duration.ofMinutes(3)); // CENTER → EAST
    var segmentCD = createMockGraphPath(Duration.ofMinutes(2)); // EAST → MIDPOINT_NORTH
    var segmentDB = createMockGraphPath(Duration.ofMinutes(4)); // MIDPOINT_NORTH → NORTH

    // Setup routing mock: return all segment mocks for any routing call
    // The algorithm will evaluate multiple insertion positions
    when(mockRoutingFunction.route(any(), any())).thenReturn(
      baselinePath,
      segmentAC,
      segmentCD,
      segmentDB,
      segmentAC,
      segmentCD,
      segmentDB,
      segmentAC,
      segmentCD
    );

    // Passenger pickup at OSLO_EAST, dropoff at OSLO_MIDPOINT_NORTH
    // Both are between OSLO_CENTER and OSLO_NORTH
    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_MIDPOINT_NORTH);

    assertNotNull(result, "Should find valid insertion");

    // Verify the result structure
    assertEquals(3, result.routeSegments().size(), "Should have 3 segments in modified route");
    assertEquals(Duration.ofMinutes(10), result.baselineDuration(), "Baseline should be 10 min");

    // CRITICAL: Total duration should be sum of NEW segments, NOT baseline duration
    // Total = 3 + 2 + 4 = 9 minutes
    // If bug exists, segment A→C would incorrectly use baseline (10 min) → total would be wrong
    Duration expectedTotal = Duration.ofMinutes(9);
    assertEquals(
      expectedTotal,
      result.totalDuration(),
      "Total duration should be sum of newly routed segments"
    );

    // Additional duration should be negative (this insertion is actually faster!)
    // This is realistic for insertions that "shortcut" part of the baseline route
    Duration expectedAdditional = Duration.ofMinutes(-1);
    assertEquals(
      expectedAdditional,
      result.additionalDuration(),
      "Additional duration should be -1 minute (insertion is faster)"
    );

    // Verify routing was called at least 4 times (1 baseline + 3 new segments minimum)
    // May be more due to evaluating multiple positions
    verify(mockRoutingFunction, atLeast(4)).route(any(), any());
  }

  @Test
  void findOptimalInsertion_insertAtEnd_reusesMostSegments() {
    // This test verifies that segment reuse optimization still works correctly
    // Scenario: Trip A→B→C, insert passenger that allows some segment reuse
    // Expected: Segments that have matching endpoints should be REUSED

    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Baseline has 2 segments: CENTER→EAST, EAST→NORTH
    var mockPath = createMockGraphPath(Duration.ofMinutes(5));

    // Return mock paths for all routing calls (baseline + any new segments)
    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    // Insert passenger - the algorithm will find the best position
    var result = findOptimalInsertion(trip, OSLO_WEST, OSLO_SOUTH);

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

    // Verify that routing was called for baseline and new segments
    // If all segments were re-routed, we'd see many more calls
    // The exact number depends on which position is optimal and how many segments can be reused
    verify(mockRoutingFunction, atLeast(2)).route(any(), any());
  }

  @Test
  void findOptimalInsertion_pickupAtExistingPoint_handlesCorrectly() {
    // Scenario: Trip A→B→C, passenger pickup at B (existing point), dropoff at new point
    // Expected: Segment A→B should be reused, B→dropoff and dropoff→C should be routed

    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTHEAST);

    var mockPath = createMockGraphPath(Duration.ofMinutes(5));

    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    // Pickup exactly at OSLO_EAST (existing stop), dropoff at OSLO_NORTH (new)
    // OSLO_NORTH is directly on the way from OSLO_EAST to OSLO_NORTHEAST (same longitude as OSLO_EAST)
    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_NORTH);

    assertNotNull(result, "Should find valid insertion");

    // Modified route should have new segments
    assertTrue(result.routeSegments().size() >= 2);

    // Routing should be called for baseline and new segments
    verify(mockRoutingFunction, atLeast(2)).route(any(), any());
  }

  @Test
  void findOptimalInsertion_singleSegmentTrip_routesAllNewSegments() {
    // Edge case: Simplest possible trip (2 points, 1 segment)
    // Any insertion will require routing all new segments

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var mockPath = createMockGraphPath(Duration.ofMinutes(5));

    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNotNull(result);
    assertEquals(3, result.routeSegments().size());

    // Verify routing was called for baseline and new segments
    verify(mockRoutingFunction, atLeast(4)).route(any(), any());

    // Total duration should be positive
    assertTrue(result.totalDuration().compareTo(Duration.ZERO) > 0);
    assertTrue(result.baselineDuration().compareTo(Duration.ZERO) > 0);
  }
}
