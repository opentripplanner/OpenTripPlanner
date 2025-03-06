package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;

class VehicleWalkingPreferencesTest {

  private static final double SPEED = 1.45;
  private static final double RELUCTANCE = 5.5;
  private static final Duration MOUNT_DISMOUNT_TIME = Duration.ofSeconds(15);
  private static final Cost MOUNT_DISMOUNT_COST = Cost.costOfSeconds(20);
  private static final double STAIRS_RELUCTANCE = 11;

  private final VehicleWalkingPreferences subject = createPreferences();

  @Test
  void speed() {
    assertEquals(SPEED, subject.speed());
  }

  @Test
  void reluctance() {
    assertEquals(RELUCTANCE, subject.reluctance());
  }

  @Test
  void mountDismountTime() {
    assertEquals(MOUNT_DISMOUNT_TIME, subject.mountDismountTime());
  }

  @Test
  void mountDismountCost() {
    assertEquals(MOUNT_DISMOUNT_COST, subject.mountDismountCost());
  }

  @Test
  void stairsReluctance() {
    assertEquals(STAIRS_RELUCTANCE, subject.stairsReluctance());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withSpeed(5.4).build();
    var same = other.copyOf().withSpeed(SPEED).build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("VehicleWalkingPreferences{}", VehicleWalkingPreferences.DEFAULT.toString());
    assertEquals(
      "VehicleWalkingPreferences{" +
      "speed: 1.45, " +
      "reluctance: 5.5, " +
      "mountDismountTime: PT15S, " +
      "mountDismountCost: $20, " +
      "stairsReluctance: 11.0}",
      subject.toString()
    );
  }

  private VehicleWalkingPreferences createPreferences() {
    return VehicleWalkingPreferences.of()
      .withSpeed(SPEED)
      .withReluctance(RELUCTANCE)
      .withMountDismountTime(MOUNT_DISMOUNT_TIME)
      .withMountDismountCost(MOUNT_DISMOUNT_COST.toSeconds())
      .withStairsReluctance(STAIRS_RELUCTANCE)
      .build();
  }
}
