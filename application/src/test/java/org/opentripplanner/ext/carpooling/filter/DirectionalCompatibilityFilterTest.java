package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class DirectionalCompatibilityFilterTest {

  private DirectionalCompatibilityFilter filter;

  @BeforeEach
  void setup() {
    filter = new DirectionalCompatibilityFilter();
  }

  @Test
  void accepts_passengerAlignedWithTrip_returnsTrue() {
    var trip = createSimpleTrip(
      OSLO_CENTER,
      OSLO_NORTH // Trip goes north
    );

    // Passenger also going north
    var passengerPickup = OSLO_EAST;
    var passengerDropoff = new WgsCoordinate(59.9549, 10.7922); // Northeast

    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerOppositeDirection_returnsFalse() {
    var trip = createSimpleTrip(
      OSLO_CENTER,
      OSLO_NORTH // Trip goes north
    );

    // Passenger going south
    var passengerPickup = OSLO_EAST;
    var passengerDropoff = OSLO_CENTER;

    assertFalse(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_tripAroundLake_passengerOnSegment_returnsTrue() {
    // Trip goes around a lake: North → East → South → West
    var stop1 = createStopAt(0, LAKE_EAST);
    var stop2 = createStopAt(1, LAKE_SOUTH);
    var trip = createTripWithStops(LAKE_NORTH, List.of(stop1, stop2), LAKE_WEST);

    // Passenger aligned with the southward segment (East → South)
    var passengerPickup = new WgsCoordinate(59.9339, 10.7922); // East side
    var passengerDropoff = new WgsCoordinate(59.9139, 10.7922); // South of east

    // Should accept because passenger aligns with East→South segment
    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerFarFromRoute_butDirectionallyAligned_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger far to the east but directionally aligned (both going north)
    var passengerPickup = new WgsCoordinate(59.9139, 11.0000); // Way east
    var passengerDropoff = new WgsCoordinate(59.9439, 11.0000);

    // Should accept - only checks direction, not distance (that's DistanceBasedFilter's job)
    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerPartiallyAligned_withinTolerance_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH); // Going north

    // Passenger going northeast (~45° off)
    var passengerPickup = OSLO_CENTER;
    var passengerDropoff = OSLO_NORTHEAST;

    // Should accept within default tolerance (60°)
    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerPerpendicularToTrip_returnsFalse() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH); // Going north

    // Passenger going east (90° perpendicular)
    var passengerPickup = OSLO_CENTER;
    var passengerDropoff = OSLO_EAST;

    // Should reject (exceeds 60° tolerance)
    assertFalse(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_complexRoute_multipleSegments_findsCompatibleSegment() {
    // Trip with multiple segments going different directions
    var stop1 = createStopAt(0, OSLO_EAST); // Go east first
    var stop2 = createStopAt(1, OSLO_NORTHEAST); // Then northeast
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Passenger going northeast (aligns with second segment)
    var passengerPickup = new WgsCoordinate(59.9289, 10.7722);
    var passengerDropoff = new WgsCoordinate(59.9389, 10.7822);

    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_tripWithSingleStop_checksAllSegments() {
    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Passenger aligned with first segment (Center → East)
    var passengerPickup = new WgsCoordinate(59.9139, 10.7622);
    var passengerDropoff = new WgsCoordinate(59.9139, 10.7822);

    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerWithinCorridorButWrongDirection_returnsFalse() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH); // Going north

    // Passenger nearby but going opposite direction
    var passengerPickup = new WgsCoordinate(59.9239, 10.7522); // North
    var passengerDropoff = new WgsCoordinate(59.9139, 10.7522); // South (backtracking)

    assertFalse(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void customBearingTolerance_acceptsWithinCustomTolerance() {
    // Custom filter with 90° tolerance (very permissive)
    var customFilter = new DirectionalCompatibilityFilter(90.0);

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH); // Going north

    // Passenger going east (90° perpendicular)
    var passengerPickup = OSLO_CENTER;
    var passengerDropoff = OSLO_EAST;

    // Should accept with 90° tolerance (default 60° would reject)
    assertTrue(customFilter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void customBearingTolerance_rejectsOutsideCustomTolerance() {
    // Custom filter with 30° tolerance (strict)
    var customFilter = new DirectionalCompatibilityFilter(30.0);

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH); // Going north

    // Passenger going northeast (~45° off)
    var passengerPickup = OSLO_CENTER;
    var passengerDropoff = OSLO_NORTHEAST;

    // Should reject with 30° tolerance (default 60° would accept)
    assertFalse(customFilter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void getBearingToleranceDegrees_returnsConfiguredValue() {
    var customFilter = new DirectionalCompatibilityFilter(45.0);
    assertEquals(45.0, customFilter.getBearingToleranceDegrees());
  }

  @Test
  void defaultBearingTolerance_is60Degrees() {
    assertEquals(60.0, filter.getBearingToleranceDegrees());
  }
}
