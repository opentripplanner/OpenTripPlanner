package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.core.BicycleOptimizeType;

class BikePreferencesTest {

  public static final double SPEED = 2.0;
  public static final double RELUCTANCE = 1.2;
  public static final double WALKING_SPEED = 1.15;
  public static final int BOARD_COST = 660;
  public static final double WALKING_RELUCTANCE = 1.45;
  public static final int SWITCH_TIME = 200;
  public static final int SWITCH_COST = 450;
  public static final TimeSlopeSafetyTriangle TRIANGLE = TimeSlopeSafetyTriangle
    .of()
    .withSlope(1)
    .build();
  public static final BicycleOptimizeType OPTIMIZE_TYPE = BicycleOptimizeType.TRIANGLE;
  public static final int RENTAL_PICKUP_TIME = 30;
  public static final int PARK_COST = 30;

  private final BikePreferences subject = BikePreferences
    .of()
    .withSpeed(SPEED)
    .withReluctance(RELUCTANCE)
    .withBoardCost(BOARD_COST)
    .withWalkingSpeed(WALKING_SPEED)
    .withWalkingReluctance(WALKING_RELUCTANCE)
    .withSwitchTime(SWITCH_TIME)
    .withSwitchCost(SWITCH_COST)
    .withOptimizeType(OPTIMIZE_TYPE)
    .withRental(rental -> rental.withPickupTime(RENTAL_PICKUP_TIME).build())
    .withParking(parking -> parking.withParkCost(PARK_COST).build())
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
  void boardCost() {
    assertEquals(BOARD_COST, subject.boardCost());
  }

  @Test
  void walkingSpeed() {
    assertEquals(WALKING_SPEED, subject.walkingSpeed());
  }

  @Test
  void walkingReluctance() {
    assertEquals(WALKING_RELUCTANCE, subject.walkingReluctance());
  }

  @Test
  void switchTime() {
    assertEquals(SWITCH_TIME, subject.switchTime());
  }

  @Test
  void switchCost() {
    assertEquals(SWITCH_COST, subject.switchCost());
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
  void parking() {
    var vehicleParking = VehicleParkingPreferences.of().withParkCost(PARK_COST).build();
    assertEquals(vehicleParking, subject.parking());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(BikePreferences.DEFAULT, BikePreferences.of().build());
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
    assertEquals("BikePreferences{}", BikePreferences.DEFAULT.toString());
    assertEquals(
      "BikePreferences{" +
      "speed: 2.0, " +
      "reluctance: 1.2, " +
      "boardCost: $660, " +
      "walkingSpeed: 1.15, " +
      "walkingReluctance: 1.45, " +
      "switchTime: 3m20s, " +
      "switchCost: $450, " +
      "parking: VehicleParkingPreferences{parkCost: $30}, " +
      "rental: VehicleRentalPreferences{pickupTime: 30s}, " +
      "optimizeType: TRIANGLE, " +
      "optimizeTriangle: TimeSlopeSafetyTriangle[time=0.0, slope=1.0, safety=0.0]" +
      "}",
      subject.toString()
    );
  }
}
