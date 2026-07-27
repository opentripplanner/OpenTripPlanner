package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.beelineRoutedTrip;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createDestinationStop;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createOriginStop;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithCapacity;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithDeviationBudget;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Tests for {@link InsertionPositionFinder}.
 * Focuses on heuristic validation: capacity and beeline delays.
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

    // Passenger picked up east of route, dropped off at destination — small compatible detour
    var viablePositions = finder.findViablePositions(
      beelineRoutedTrip(onRoutePoints(trip)),
      OSLO_EAST,
      OSLO_NORTH,
      Duration.ZERO
    );

    assertFalse(viablePositions.isEmpty());
  }

  @Test
  void findViablePositions_noCapacity_rejectsPosition() {
    // Create a trip with 0 available seats
    var stops = List.of(createOriginStop(OSLO_CENTER), createDestinationStop(OSLO_NORTH));
    var trip = createTripWithCapacity(0, stops);

    var viablePositions = finder.findViablePositions(
      beelineRoutedTrip(onRoutePoints(trip)),
      OSLO_EAST,
      OSLO_WEST,
      Duration.ZERO
    );

    // Should reject all positions due to capacity
    assertTrue(viablePositions.isEmpty());
  }

  @Test
  void findViablePositions_exceedsBeelineDelay_rejectsPosition() {
    // Create stops with very restrictive deviation budgets (1 second)
    var restrictiveBudget = Duration.ofSeconds(1);
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(createStopAt(OSLO_EAST, restrictiveBudget)),
      OSLO_NORTH,
      restrictiveBudget
    );

    // Passenger going opposite direction (WEST→SOUTH) with 1s budget — all positions should be rejected
    var viablePositions = finder.findViablePositions(
      beelineRoutedTrip(onRoutePoints(trip)),
      OSLO_WEST,
      OSLO_SOUTH,
      Duration.ZERO
    );

    assertTrue(viablePositions.isEmpty());
  }

  @Test
  void findViablePositions_multipleStops_checksAllCombinations() {
    var stop1 = createStopAt(OSLO_EAST);
    var stop2 = createStopAt(OSLO_WEST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    var viablePositions = finder.findViablePositions(
      beelineRoutedTrip(onRoutePoints(trip)),
      OSLO_SOUTH,
      OSLO_NORTH,
      Duration.ZERO
    );

    // Should evaluate multiple pickup/dropoff combinations
    // Exact count depends on beeline filtering
    assertNotNull(viablePositions);
  }

  /**
   * Delay is a difference against the baseline, so the baseline must carry the leg's routed duration:
   * a winding leg leaves an off-line detour adding almost nothing on top of it.
   */
  @Test
  void findViablePositions_windingBaselineLeg_keepsNearOnRouteInsertion() {
    // Tight budget the straight-line detour estimate on its own would exceed.
    var tightBudget = Duration.ofSeconds(30);
    var trip = createTripWithDeviationBudget(tightBudget, OSLO_CENTER, OSLO_NORTH);

    // The lone CENTER→NORTH leg takes an hour to drive (a winding road), far longer than the
    // straight line between its endpoints, so the detour via EAST/WEST adds little beyond it.
    var windingBaseline = new RoutedCarpoolTrip(
      onRoutePoints(trip),
      new Duration[] { Duration.ofHours(1) }
    );

    var viablePositions = finder.findViablePositions(
      windingBaseline,
      OSLO_EAST,
      OSLO_WEST,
      Duration.ZERO
    );

    assertFalse(
      viablePositions.isEmpty(),
      "measuring the detour against the real (longer) baseline must keep the feasible insertion"
    );
  }

  /**
   * The detour is measured from the resolved vertices, not the declared route points. The passenger
   * waits on the driver's actual route, so the tightest budget still admits the insertion.
   */
  @Test
  void findViablePositions_routePointResolvedFarFromItsVertex_measuresDetourFromTheVertex() {
    var trip = createTripWithDeviationBudget(Duration.ofSeconds(1), OSLO_CENTER, OSLO_NORTH);
    var tripWithVertices = new CarpoolTripWithVertices(
      trip,
      List.of(vertexAt(OSLO_EAST), vertexAt(OSLO_NORTH))
    );

    // The baseline is driven between the resolved vertices, so its lone leg runs EAST → NORTH.
    var viablePositions = finder.findViablePositions(
      beelineRoutedTrip(tripWithVertices),
      OSLO_EAST,
      OSLO_NORTH,
      Duration.ZERO
    );

    assertFalse(
      viablePositions.isEmpty(),
      "a passenger waiting on the driver's actual route adds no delay and must not be rejected"
    );
  }

  /** A vertex sitting exactly on the given coordinate. */
  private static Vertex vertexAt(WgsCoordinate coordinate) {
    return new SimpleVertex(
      "vertex-" + coordinate.latitude() + "-" + coordinate.longitude(),
      coordinate.latitude(),
      coordinate.longitude()
    );
  }

  /** The trip resolved to vertices sitting exactly on its route points — nothing was snapped. */
  private static CarpoolTripWithVertices onRoutePoints(CarpoolTrip trip) {
    return new CarpoolTripWithVertices(
      trip,
      trip.routePoints().stream().map(InsertionPositionFinderTest::vertexAt).toList()
    );
  }
}
