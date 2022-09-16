package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WalkPreferencesTest {

  private static final double SPEED = 1.71;
  private static final double RELUCTANCE = 2.51;
  private static final int BOARD_COST = 301;
  private static final double STAIRS_RELUCTANCE = 3.01;
  private static final double STAIRS_TIME_FACTOR = 1.31;
  private static final double SAFETY_FACTOR = 0.51;

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
  void stairsReluctance() {
    assertEquals(STAIRS_RELUCTANCE, subject.stairsReluctance());
  }

  @Test
  void stairsTimeFactor() {
    assertEquals(STAIRS_TIME_FACTOR, subject.stairsTimeFactor());
  }

  @Test
  void safetyFactor() {
    assertEquals(SAFETY_FACTOR, subject.safetyFactor());
  }

  @Test
  void testEqualsAndHAshCode() {
    // By changing the speed back and forth we force the builder to create a new instance
    var copy = subject.copyOf().withSpeed(10.0).build().copyOf().withSpeed(SPEED).build();

    assertNotSame(copy, subject);
    assertEquals(copy, subject);
    assertEquals(copy.hashCode(), subject.hashCode());

    var others = Stream
      .of(
        subject.copyOf().withSpeed(1),
        subject.copyOf().withReluctance(1),
        subject.copyOf().withBoardCost(1),
        subject.copyOf().withStairsReluctance(1),
        subject.copyOf().withStairsTimeFactor(1),
        subject.copyOf().withSafetyFactor(1)
      )
      .map(WalkPreferences.Builder::build)
      .toList();

    for (WalkPreferences other : others) {
      assertNotEquals(subject, other);
      assertNotEquals(subject.hashCode(), other.hashCode());
    }
  }

  @Test
  void testToSting() {
    assertEquals("WalkPreferences{}", WalkPreferences.DEFAULT.toString());
    assertEquals(
      "WalkPreferences{speed: 1.71, reluctance: 2.51, boardCost: 301, stairsReluctance: 3.01, stairsTimeFactor: 1.31, safetyFactor: 0.51}",
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
