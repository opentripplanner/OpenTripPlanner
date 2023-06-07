package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class WalkPreferencesTest {

  private static final double SPEED = 1.7111;
  private static final double EXPECTED_SPEED = 1.71;
  private static final double RELUCTANCE = 2.51;
  private static final double EXPECTED_RELUCTANCE = 2.5;
  private static final int BOARD_COST = 301;
  private static final double STAIRS_RELUCTANCE = 3.011;
  private static final double EXPECTED_STAIRS_RELUCTANCE = 3.0;
  private static final double STAIRS_TIME_FACTOR = 1.3111;
  private static final double EXPECTED_STAIRS_TIME_FACTOR = 1.31;
  private static final double SAFETY_FACTOR = 0.5111;
  private static final double EXPECTED_SAFETY_FACTOR = 0.51;

  private final WalkPreferences subject = WalkPreferences
    .of()
    .withSpeed(SPEED)
    .withReluctance(RELUCTANCE)
    .withBoardCost(BOARD_COST)
    .withStairsReluctance(STAIRS_RELUCTANCE)
    .withStairsTimeFactor(STAIRS_TIME_FACTOR)
    .withSafetyFactor(SAFETY_FACTOR)
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
  void boardCost() {
    assertEquals(BOARD_COST, subject.boardCost());
  }

  @Test
  void stairsReluctance() {
    assertEquals(EXPECTED_STAIRS_RELUCTANCE, subject.stairsReluctance());
  }

  @Test
  void stairsTimeFactor() {
    assertEquals(EXPECTED_STAIRS_TIME_FACTOR, subject.stairsTimeFactor());
  }

  @Test
  void safetyFactor() {
    assertEquals(EXPECTED_SAFETY_FACTOR, subject.safetyFactor());
  }

  @Test
  void testEqualsAndHAshCode() {
    // Return same object if no value is set
    assertSame(subject, subject.copyOf().build());
    assertSame(TransitPreferences.DEFAULT, TransitPreferences.of().build());

    // By changing the speed back and forth we force the builder to create a new instance
    var other = subject.copyOf().withSpeed(10.0).build();
    var copy = other.copyOf().withSpeed(SPEED).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToSting() {
    assertEquals("WalkPreferences{}", WalkPreferences.DEFAULT.toString());
    assertEquals(
      "WalkPreferences{speed: 1.71, reluctance: 2.5, boardCost: $301, stairsReluctance: 3.0, stairsTimeFactor: 1.31, safetyFactor: 0.51}",
      subject.toString()
    );
  }

  void assertNotTheSame(Consumer<WalkPreferences.Builder> body) {
    var copy = subject.copyOf();
    body.accept(copy);
    WalkPreferences walk = copy.build();
    assertNotEquals(subject, walk);
  }
}
