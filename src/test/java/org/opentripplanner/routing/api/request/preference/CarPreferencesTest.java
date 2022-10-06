package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

class CarPreferencesTest {

  private static final double SPEED = 20.111;
  private static final double EXPECTED_SPEED = 20.0;
  private static final double RELUCTANCE = 5.111;
  private static final double EXPECTED_RELUCTANCE = 5.1;
  private static final int PARK_TIME = 300;
  private static final int PARK_COST = 250;
  private static final int PICKUP_TIME = 600;
  private static final int PICKUP_COST = 500;
  private static final double ACCELERATION_SPEED = 3.1;
  private static final double DECELERATION_SPEED = 3.5;
  public static final int DROPOFF_TIME = 450;

  private final CarPreferences subject = CarPreferences
    .of()
    .withSpeed(SPEED)
    .withReluctance(RELUCTANCE)
    .withParkTime(PARK_TIME)
    .withParkCost(PARK_COST)
    .withPickupTime(PICKUP_TIME)
    .withPickupCost(PICKUP_COST)
    .withDropoffTime(DROPOFF_TIME)
    .withAccelerationSpeed(ACCELERATION_SPEED)
    .withDecelerationSpeed(DECELERATION_SPEED)
    .build();

  @Test
  void speed() {
    assertEquals(EXPECTED_SPEED, subject.speed());
  }

  @Test
  void reluctance() {
    assertEquals(EXPECTED_RELUCTANCE, subject.reluctance());
  }

  @Test
  void parkTime() {
    assertEquals(PARK_TIME, subject.parkTime());
  }

  @Test
  void parkCost() {
    assertEquals(PARK_COST, subject.parkCost());
  }

  @Test
  void pickupTime() {
    assertEquals(PICKUP_TIME, subject.pickupTime());
  }

  @Test
  void pickupCost() {
    assertEquals(PICKUP_COST, subject.pickupCost());
  }

  @Test
  void dropoffTime() {
    assertEquals(DROPOFF_TIME, subject.dropoffTime());
  }

  @Test
  void accelerationSpeed() {
    assertEquals(ACCELERATION_SPEED, subject.accelerationSpeed());
  }

  @Test
  void decelerationSpeed() {
    assertEquals(DECELERATION_SPEED, subject.decelerationSpeed());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(CarPreferences.DEFAULT, CarPreferences.of().build());
    assertSame(subject, subject.copyOf().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withSpeed(0.0).build();
    var same = other.copyOf().withSpeed(SPEED).build();
    assertEqualsAndHashCode(StreetPreferences.DEFAULT, subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("CarPreferences{}", CarPreferences.DEFAULT.toString());
    assertEquals(
      "CarPreferences{" +
      "speed: 20.0, " +
      "reluctance: 5.1, " +
      "parkTime: 300, " +
      "parkCost: 250, " +
      "pickupTime: 600, " +
      "pickupCost: 500, " +
      "dropoffTime: 450, " +
      "accelerationSpeed: 3.1, decelerationSpeed: 3.5" +
      "}",
      subject.toString()
    );
  }
}
