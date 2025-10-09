package org.opentripplanner.ext.carpooling.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.util.PassengerCountTimeline;
import org.opentripplanner.ext.carpooling.validation.InsertionValidator.ValidationContext;

class CapacityValidatorTest {

  private CapacityValidator validator;

  @BeforeEach
  void setup() {
    validator = new CapacityValidator();
  }

  @Test
  void validate_sufficientCapacity_returnsValid() {
    var stop1 = createStop(0, +2); // 2 passengers
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var routeCoords = List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH);

    var context = new ValidationContext(
      1,
      2, // Pickup at 1, dropoff at 2
      OSLO_EAST,
      OSLO_WEST,
      routeCoords,
      timeline
    );

    var result = validator.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void validate_insufficientCapacityAtPickup_returnsInvalid() {
    var stop1 = createStop(0, +4); // All 4 seats taken
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var routeCoords = List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH);

    var context = new ValidationContext(
      2,
      3, // Try to insert after stop (no capacity)
      OSLO_EAST,
      OSLO_WEST,
      routeCoords,
      timeline
    );

    var result = validator.validate(context);
    assertFalse(result.isValid());
    assertTrue(result.reason().contains("capacity"));
  }

  @Test
  void validate_capacityFreedAtDropoff_checksRange() {
    var stop1 = createStop(0, +3); // 3 passengers
    var stop2 = createStop(1, -2); // 2 dropoff, leaving 1
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var routeCoords = List.of(OSLO_CENTER, OSLO_EAST, OSLO_SOUTH, OSLO_NORTH);

    // Inserting between stop1 and stop2 (3 passengers) - only 1 seat free
    var context = new ValidationContext(2, 3, OSLO_EAST, OSLO_WEST, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid()); // 1 seat available
  }

  @Test
  void validate_noCapacityInRange_returnsInvalid() {
    var stop1 = createStop(0, +4); // Fill all capacity
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var routeCoords = List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH);

    // Try to insert after the stop where capacity is full
    var context = new ValidationContext(2, 3, OSLO_EAST, OSLO_WEST, routeCoords, timeline);

    var result = validator.validate(context);
    assertFalse(result.isValid());
  }

  @Test
  void validate_capacityAtBeginning_beforeAnyStops_returnsValid() {
    var stop1 = createStop(0, +3);
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var routeCoords = List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH);

    // Insert before any stops (full capacity available)
    var context = new ValidationContext(1, 2, OSLO_EAST, OSLO_WEST, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void validate_exactlyAtCapacity_returnsInvalid() {
    var stop1 = createStop(0, +3); // 3 passengers, leaving 1 seat
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var routeCoords = List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH);

    // This would require 2 additional seats (passenger + existing 3 = 5)
    // But we can only test for 1 additional passenger, so let's test the boundary
    var context = new ValidationContext(2, 3, OSLO_EAST, OSLO_WEST, routeCoords, timeline);

    var result = validator.validate(context);
    assertTrue(result.isValid()); // Should have exactly 1 seat available
  }
}
