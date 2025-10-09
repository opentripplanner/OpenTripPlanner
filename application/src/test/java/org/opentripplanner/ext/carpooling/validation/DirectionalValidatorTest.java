package org.opentripplanner.ext.carpooling.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.util.PassengerCountTimeline;
import org.opentripplanner.ext.carpooling.validation.InsertionValidator.ValidationContext;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class DirectionalValidatorTest {

  private DirectionalValidator validator;

  @BeforeEach
  void setup() {
    validator = new DirectionalValidator();
  }

  @Test
  void validate_forwardProgress_returnsValid() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Insert along the route direction
    var pickup = new WgsCoordinate(59.9239, 10.7522); // Between center and north
    var dropoff = new WgsCoordinate(59.9339, 10.7522);

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void validate_pickupCausesBacktracking_returnsInvalid() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Pickup south of starting point (backtracking)
    var pickup = OSLO_SOUTH;
    var dropoff = OSLO_NORTH;

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    assertFalse(result.isValid());
    assertTrue(result.reason().contains("backtrack") || result.reason().contains("forward"));
  }

  @Test
  void validate_dropoffCausesBacktracking_allowedWithinTolerance() {
    // DirectionalValidator uses 90° tolerance, which allows some backtracking
    // This test verifies that moderate backtracking within tolerance is accepted
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Dropoff south of pickup (some backtracking, but within 90° tolerance)
    var pickup = new WgsCoordinate(59.9239, 10.7522);
    var dropoff = OSLO_CENTER; // Back toward start

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    // With 90° tolerance, moderate backtracking is allowed for routing flexibility
    assertTrue(result.isValid());
  }

  @Test
  void validate_moderateDetour_allowed() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Slight eastward detour, but still generally northward
    var pickup = new WgsCoordinate(59.9239, 10.7622); // North-east
    var dropoff = new WgsCoordinate(59.9339, 10.7622);

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid()); // Should allow reasonable detours
  }

  @Test
  void validate_pickupAtBeginning_checksFromBoarding() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_MIDPOINT_NORTH, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Insert at very beginning
    var pickup = new WgsCoordinate(59.9189, 10.7522); // Just north of center
    var dropoff = OSLO_MIDPOINT_NORTH;

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void validate_dropoffAtEnd_checkesToAlighting() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_MIDPOINT_NORTH, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Insert with dropoff near end
    var pickup = OSLO_MIDPOINT_NORTH;
    var dropoff = new WgsCoordinate(59.9389, 10.7522); // Just south of north

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void validate_multiStopRoute_checksCorrectSegments() {
    var stop1 = createStopAt(0, OSLO_EAST);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Insert between first and second segment
    var pickup = new WgsCoordinate(59.9189, 10.7722); // Between center and east
    var dropoff = new WgsCoordinate(59.9289, 10.7722);

    var context = new ValidationContext(2, 3, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void validate_largePerpendicularDetour_allowedWithinTolerance() {
    // DirectionalValidator uses 90° tolerance to allow perpendicular detours
    // This is intentional to provide routing flexibility
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var routeCoords = List.of(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Perpendicular detour (going east when route goes north = 90°)
    var pickup = new WgsCoordinate(59.9239, 10.7522);
    var dropoff = new WgsCoordinate(59.9239, 10.8522); // Far east

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = validator.validate(context);
    // 90° is exactly at the tolerance boundary and is allowed
    assertTrue(result.isValid());
  }

  @Test
  void validate_beyondToleranceDetour_returnsInvalid() {
    // Test that detours beyond the configured tolerance are rejected
    // Use a stricter validator to test this behavior
    var strictValidator = new DirectionalValidator(45.0); // 45° tolerance

    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // Use 3 points so dropoff validation can occur between points
    var routeCoords = List.of(OSLO_CENTER, OSLO_MIDPOINT_NORTH, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Pickup going north-northeast (~45°) - should pass
    var pickup = new WgsCoordinate(59.9189, 10.7622);
    // Dropoff going east (90° from north) - should exceed 45° tolerance
    var dropoff = new WgsCoordinate(59.9239, 10.8522);

    var context = new ValidationContext(1, 2, pickup, dropoff, routeCoords, timeline);

    var result = strictValidator.validate(context);
    // 90° deviation should be rejected with 45° tolerance
    assertFalse(result.isValid());
  }
}
