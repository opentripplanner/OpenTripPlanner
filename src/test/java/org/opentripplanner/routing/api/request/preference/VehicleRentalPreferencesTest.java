package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import org.junit.jupiter.api.Test;

class VehicleRentalPreferencesTest {

  public static final int PICKUP_TIME = 25;
  public static final int PICKUP_COST = 250;
  public static final int DROPOFF_TIME = 45;
  public static final int DROPOFF_COST = 450;
  public static final int ARRIVE_IN_RENTAL_COST = 500;
  public static final boolean USE_AVAILABILITY_INFORMATION = true;
  private final VehicleRentalPreferences subject = VehicleRentalPreferences
    .of()
    .withPickupTime(PICKUP_TIME)
    .withPickupCost(PICKUP_COST)
    .withDropoffTime(DROPOFF_TIME)
    .withDropoffCost(DROPOFF_COST)
    .withArrivingInRentalVehicleAtDestinationCost(ARRIVE_IN_RENTAL_COST)
    .withUseAvailabilityInformation(USE_AVAILABILITY_INFORMATION)
    .build();

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
  void dropoffCost() {
    assertEquals(DROPOFF_COST, subject.dropoffCost());
  }

  @Test
  void useAvailabilityInformation() {
    assertEquals(USE_AVAILABILITY_INFORMATION, subject.useAvailabilityInformation());
  }

  @Test
  void arrivingInRentalVehicleAtDestinationCost() {
    assertEquals(ARRIVE_IN_RENTAL_COST, subject.arrivingInRentalVehicleAtDestinationCost());
  }

  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(VehicleRentalPreferences.DEFAULT, VehicleRentalPreferences.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withPickupTime(450).build();
    var copy = other.copyOf().withPickupTime(PICKUP_TIME).build();
    assertEqualsAndHashCode(StreetPreferences.DEFAULT, subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("VehicleRentalPreferences{}", VehicleRentalPreferences.DEFAULT.toString());
    assertEquals(
      "VehicleRentalPreferences{" +
      "pickupTime: 25s, " +
      "pickupCost: $250, " +
      "dropoffTime: 45s, " +
      "dropoffCost: $450, " +
      "useAvailabilityInformation, " +
      "arrivingInRentalVehicleAtDestinationCost: 500.0" +
      "}",
      subject.toString()
    );
  }
}
