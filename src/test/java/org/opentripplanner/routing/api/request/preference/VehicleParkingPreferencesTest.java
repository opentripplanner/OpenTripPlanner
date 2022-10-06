package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VehicleParkingPreferencesTest {

  private final VehicleParkingPreferences AVAILABILITY_TRUE = VehicleParkingPreferences.of(true);
  private final VehicleParkingPreferences AVAILABILITY_FALSE = VehicleParkingPreferences.of(false);

  @Test
  void testFactoryOf() {
    assertSame(AVAILABILITY_FALSE, VehicleParkingPreferences.DEFAULT);
    assertSame(AVAILABILITY_FALSE, VehicleParkingPreferences.of(false));
    assertSame(AVAILABILITY_TRUE, VehicleParkingPreferences.of(true));
  }

  @Test
  void useAvailabilityInformation() {
    assertFalse(VehicleParkingPreferences.DEFAULT.useAvailabilityInformation());
    assertTrue(AVAILABILITY_TRUE.useAvailabilityInformation());
    assertFalse(AVAILABILITY_FALSE.useAvailabilityInformation());
  }

  @Test
  void testEquals() {
    assertEquals(AVAILABILITY_TRUE, VehicleParkingPreferences.of(true));
    assertEquals(AVAILABILITY_FALSE, VehicleParkingPreferences.of(false));
  }

  @Test
  void testHashCode() {
    assertEquals(AVAILABILITY_TRUE.hashCode(), AVAILABILITY_TRUE.hashCode());
    assertEquals(AVAILABILITY_FALSE.hashCode(), AVAILABILITY_FALSE.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("VehicleParkingPreferences{}", AVAILABILITY_FALSE.toString());
    assertEquals(
      "VehicleParkingPreferences{useAvailabilityInformation}",
      AVAILABILITY_TRUE.toString()
    );
  }
}
