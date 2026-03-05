package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_WEST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createStopAt;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithStops;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.WgsCoordinate;

class DirectionalCompatibilityFilterTest {

  private DirectionalCompatibilityFilter filter;

  @BeforeEach
  void setup() {
    filter = new DirectionalCompatibilityFilter();
  }

  @Test
  void accepts_passengerAlignedWithTrip_returnsTrue() {
    // Trip goes north
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger also going north
    var passengerPickup = OSLO_EAST;
    // Northeast
    var passengerDropoff = new WgsCoordinate(59.9549, 10.7922);

    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerOppositeDirection_returnsFalse() {
    // Trip goes north
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

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
    // East side
    var passengerPickup = new WgsCoordinate(59.9339, 10.7922);
    // South of east
    var passengerDropoff = new WgsCoordinate(59.9139, 10.7922);

    // Should accept because passenger aligns with East→South segment
    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerFarFromRoute_butDirectionallyAligned_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger far to the east but directionally aligned (both going north)
    // Way east
    var passengerPickup = new WgsCoordinate(59.9139, 11.0000);
    var passengerDropoff = new WgsCoordinate(59.9439, 11.0000);

    // Should accept - only checks direction, not distance (that's DistanceBasedFilter's job)
    assertTrue(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void accepts_passengerPartiallyAligned_withinTolerance_returnsTrue() {
    // Going north
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger going northeast (~45° off)
    // Should accept within default tolerance (60°)
    assertTrue(filter.accepts(trip, OSLO_CENTER, OSLO_NORTHEAST));
  }

  @Test
  void accepts_passengerPerpendicularToTrip_returnsFalse() {
    // Going north
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger going east (90° perpendicular)
    // Should reject (exceeds 60° tolerance)
    assertFalse(filter.accepts(trip, OSLO_CENTER, OSLO_EAST));
  }

  @Test
  void accepts_complexRoute_multipleSegments_findsCompatibleSegment() {
    // Trip with multiple segments going different directions
    // Go east first
    var stop1 = createStopAt(0, OSLO_EAST);
    // Then northeast
    var stop2 = createStopAt(1, OSLO_NORTHEAST);
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
    // Going north
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger nearby but going opposite direction
    // North
    var passengerPickup = new WgsCoordinate(59.9239, 10.7522);
    // South (backtracking)
    var passengerDropoff = new WgsCoordinate(59.9139, 10.7522);

    assertFalse(filter.accepts(trip, passengerPickup, passengerDropoff));
  }

  @Test
  void customBearingTolerance_acceptsWithinCustomTolerance() {
    // Custom filter with 90° tolerance (very permissive)
    var customFilter = new DirectionalCompatibilityFilter(90.0);

    // Going north
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger going east (90° perpendicular)
    // Should accept with 90° tolerance (default 60° would reject)
    assertTrue(customFilter.accepts(trip, OSLO_CENTER, OSLO_EAST));
  }

  @Test
  void customBearingTolerance_rejectsOutsideCustomTolerance() {
    // Custom filter with 30° tolerance (strict)
    var customFilter = new DirectionalCompatibilityFilter(30.0);

    // Going north
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Passenger going northeast (~45° off)
    // Should reject with 30° tolerance (default 60° would accept)
    assertFalse(customFilter.accepts(trip, OSLO_CENTER, OSLO_NORTHEAST));
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
