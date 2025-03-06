package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

class ScooterPreferencesTest {

  public static final double SPEED = 2.0;
  public static final double RELUCTANCE = 1.2;
  public static final TimeSlopeSafetyTriangle TRIANGLE = TimeSlopeSafetyTriangle.of()
    .withSlope(1)
    .build();
  public static final VehicleRoutingOptimizeType OPTIMIZE_TYPE =
    VehicleRoutingOptimizeType.TRIANGLE;
  public static final int RENTAL_PICKUP_TIME = 30;

  private final ScooterPreferences subject = ScooterPreferences.of()
    .withSpeed(SPEED)
    .withReluctance(RELUCTANCE)
    .withOptimizeType(OPTIMIZE_TYPE)
    .withRental(rental -> rental.withPickupTime(RENTAL_PICKUP_TIME).build())
    .withOptimizeTriangle(it -> it.withSlope(1).build())
    .build();

  @Test
  void speed() {
    assertEquals(SPEED, subject.speed());
  }

  @Test
  void reluctance() {
    assertEquals(RELUCTANCE, subject.reluctance());
  }

  @Test
  void optimizeType() {
    assertEquals(OPTIMIZE_TYPE, subject.optimizeType());
  }

  @Test
  void optimizeTriangle() {
    assertEquals(TRIANGLE, subject.optimizeTriangle());
  }

  @Test
  void rental() {
    var vehicleRental = VehicleRentalPreferences.of().withPickupTime(RENTAL_PICKUP_TIME).build();
    assertEquals(vehicleRental, subject.rental());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(ScooterPreferences.DEFAULT, ScooterPreferences.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withSpeed(0.7).build();
    var same = other.copyOf().withSpeed(SPEED).build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("ScooterPreferences{}", ScooterPreferences.DEFAULT.toString());
    assertEquals(
      "ScooterPreferences{" +
      "speed: 2.0, " +
      "reluctance: 1.2, " +
      "rental: VehicleRentalPreferences{pickupTime: 30s}, " +
      "optimizeType: TRIANGLE, " +
      "optimizeTriangle: TimeSlopeSafetyTriangle[time=0.0, slope=1.0, safety=0.0]" +
      "}",
      subject.toString()
    );
  }

  @Test
  void testForcedTriangleOptimization() {
    var trianglePreferences = ScooterPreferences.of()
      .withForcedOptimizeTriangle(it -> it.withSlope(1).build())
      .build();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, trianglePreferences.optimizeType());

    var conflictingPreferences = ScooterPreferences.of()
      .withOptimizeType(VehicleRoutingOptimizeType.SAFE_STREETS)
      .withForcedOptimizeTriangle(it -> it.withSlope(1).build())
      .build();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, conflictingPreferences.optimizeType());

    var emptyTrianglePreferences = ScooterPreferences.of()
      .withOptimizeType(VehicleRoutingOptimizeType.SAFE_STREETS)
      .withForcedOptimizeTriangle(it -> it.build())
      .build();
    assertEquals(VehicleRoutingOptimizeType.SAFE_STREETS, emptyTrianglePreferences.optimizeType());
  }
}
