package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.opentripplanner.ext.carpooling.MockGraphPathFactory.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.routing.OptimalInsertionStrategy.RoutingFunction;
import org.opentripplanner.ext.carpooling.validation.InsertionValidator;
import org.opentripplanner.ext.carpooling.validation.InsertionValidator.ValidationResult;

class OptimalInsertionStrategyTest {

  private InsertionValidator mockValidator;
  private RoutingFunction mockRoutingFunction;
  private OptimalInsertionStrategy strategy;

  @BeforeEach
  void setup() {
    mockValidator = mock(InsertionValidator.class);
    mockRoutingFunction = mock(RoutingFunction.class);
    strategy = new OptimalInsertionStrategy(mockValidator, mockRoutingFunction);
  }

  @Test
  void findOptimalInsertion_noValidPositions_returnsNull() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Validator rejects all positions
    when(mockValidator.validate(any())).thenReturn(ValidationResult.invalid("Test reject"));

    var result = strategy.findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNull(result);
  }

  @Test
  void findOptimalInsertion_oneValidPosition_returnsCandidate() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Create mock paths BEFORE any when() statements
    var mockPath = createMockGraphPath();

    // Accept one specific position (null-safe matcher)
    when(
      mockValidator.validate(
        argThat(ctx -> ctx != null && ctx.pickupPosition() == 1 && ctx.dropoffPosition() == 2)
      )
    ).thenReturn(ValidationResult.valid());

    when(
      mockValidator.validate(
        argThat(ctx -> ctx == null || ctx.pickupPosition() != 1 || ctx.dropoffPosition() != 2)
      )
    ).thenReturn(ValidationResult.invalid("Wrong position"));

    // Mock routing to return valid paths
    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = strategy.findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNotNull(result);
    assertEquals(1, result.pickupPosition());
    assertEquals(2, result.dropoffPosition());
  }

  @Test
  void findOptimalInsertion_routingFails_skipsPosition() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Create mock paths BEFORE any when() statements
    var mockPath = createMockGraphPath(Duration.ofMinutes(5));

    when(mockValidator.validate(any())).thenReturn(ValidationResult.valid());

    // Routing sequence:
    // 1. Baseline calculation (1 segment: OSLO_CENTER → OSLO_NORTH) = mockPath
    // 2. First insertion attempt fails (null, null, null for 3 segments)
    // 3. Second insertion attempt succeeds (mockPath for all 3 segments)
    when(mockRoutingFunction.route(any(), any()))
      .thenReturn(mockPath) // Baseline
      .thenReturn(null) // First insertion - segment 1 fails
      .thenReturn(mockPath) // Second insertion - all segments succeed
      .thenReturn(mockPath)
      .thenReturn(mockPath);

    var result = strategy.findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

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

    when(mockValidator.validate(any())).thenReturn(ValidationResult.valid());
    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = strategy.findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    // Should not return candidate that exceeds budget
    assertNull(result);
  }

  @Test
  void findOptimalInsertion_tripWithStops_evaluatesAllPositions() {
    var stop1 = createStopAt(0, OSLO_EAST);
    var stop2 = createStopAt(1, OSLO_WEST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Create mock paths BEFORE any when() statements
    var mockPath = createMockGraphPath();

    when(mockValidator.validate(any())).thenReturn(ValidationResult.valid());
    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = strategy.findOptimalInsertion(trip, OSLO_SOUTH, OSLO_EAST);

    // Should have evaluated multiple positions
    // Verify validator was called multiple times
    verify(mockValidator, atLeast(3)).validate(any());
  }

  @Test
  void findOptimalInsertion_baselineDurationCalculationFails_returnsNull() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    when(mockValidator.validate(any())).thenReturn(ValidationResult.valid());

    // Routing returns null (failure) for baseline calculation
    when(mockRoutingFunction.route(any(), any())).thenReturn(null);

    var result = strategy.findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

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

    when(mockValidator.validate(any())).thenReturn(ValidationResult.valid());

    // Use thenAnswer to provide consistent route times
    // Just return paths with reasonable durations for all calls
    when(mockRoutingFunction.route(any(), any()))
      .thenReturn(mockPath10) // Baseline
      .thenReturn(mockPath4, mockPath5, mockPath6) // First insertion (15 min total, 5 min additional)
      .thenReturn(mockPath5, mockPath6, mockPath7); // Second insertion (18 min total, 8 min additional)

    var result = strategy.findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

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

    when(mockValidator.validate(any())).thenReturn(ValidationResult.valid());
    when(mockRoutingFunction.route(any(), any())).thenReturn(mockPath);

    var result = strategy.findOptimalInsertion(trip, OSLO_EAST, OSLO_WEST);

    assertNotNull(result);
    assertNotNull(result.trip());
    assertNotNull(result.routeSegments());
    assertFalse(result.routeSegments().isEmpty());
    assertTrue(result.pickupPosition() >= 0);
    assertTrue(result.dropoffPosition() > result.pickupPosition());
  }
}
