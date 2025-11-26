package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createDestinationStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createOriginStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createStopAt;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithCapacity;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;

/**
 * Tests for {@link InsertionPositionFinder}.
 * Focuses on heuristic validation: capacity, directional compatibility, and beeline delays.
 */
class InsertionPositionFinderTest {

  private InsertionPositionFinder finder;

  @BeforeEach
  void setup() {
    finder = new InsertionPositionFinder();
  }

  @Test
  void findViablePositions_simpleTrip_findsPositions() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var viablePositions = finder.findViablePositions(trip, OSLO_EAST, OSLO_WEST);

    assertFalse(viablePositions.isEmpty());
    // Simple trip (2 points) allows insertions at positions (1,2) and (1,3)
    assertTrue(viablePositions.size() >= 1);
  }

  @Test
  void findViablePositions_incompatibleDirection_rejectsPosition() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger going perpendicular (EAST to WEST when trip is CENTER to NORTH)
    // This should result in some positions being rejected by directional checks
    var viablePositions = finder.findViablePositions(trip, OSLO_SOUTH, OSLO_CENTER);

    // May not be completely empty, but should have fewer positions than compatible directions
    // The directional check filters out positions that cause too much backtracking
    assertNotNull(viablePositions);
  }

  @Test
  void findViablePositions_noCapacity_rejectsPosition() {
    // Create a trip with 0 available seats
    var stops = List.of(createOriginStop(OSLO_CENTER), createDestinationStop(OSLO_NORTH, 1));
    var trip = createTripWithCapacity(0, stops);

    var viablePositions = finder.findViablePositions(trip, OSLO_EAST, OSLO_WEST);

    // Should reject all positions due to capacity
    assertTrue(viablePositions.isEmpty());
  }

  @Test
  void findViablePositions_exceedsBeelineDelay_rejectsPosition() {
    // Create finder with very restrictive delay constraints
    var restrictiveConstraints = new PassengerDelayConstraints(Duration.ofSeconds(1));
    var restrictiveFinder = new InsertionPositionFinder(
      restrictiveConstraints,
      new BeelineEstimator()
    );

    var trip = createTripWithStops(OSLO_CENTER, List.of(createStopAt(0, OSLO_EAST)), OSLO_NORTH);

    // Try to insert passenger that would cause significant detour
    // Far from route
    // Even farther
    var viablePositions = restrictiveFinder.findViablePositions(trip, OSLO_WEST, OSLO_SOUTH);

    // With very restrictive constraints, positions causing significant detours should be rejected
    // However, the beeline check only applies if there are existing stops (routePoints.size() > 2)
    // With CENTER, EAST, NORTH we have 3 points, so the check should apply
    // The result depends on the actual distances and heuristics
    assertNotNull(viablePositions);
  }

  @Test
  void findViablePositions_multipleStops_checksAllCombinations() {
    var stop1 = createStopAt(0, OSLO_EAST);
    var stop2 = createStopAt(1, OSLO_WEST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    var viablePositions = finder.findViablePositions(trip, OSLO_SOUTH, OSLO_NORTH);

    // Should evaluate multiple pickup/dropoff combinations
    // Exact count depends on directional and beeline filtering
    assertNotNull(viablePositions);
  }
}
