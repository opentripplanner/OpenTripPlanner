package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertNotEqualsAndHashCode;

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

  private final WalkPreferences subject = WalkPreferences.of()
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
  void testEqualsAndHashCodeWithCopiedPreferences() {
    // Return same object if no value is set
    assertSame(subject, subject.copyOf().build());
    assertSame(TransitPreferences.DEFAULT, TransitPreferences.of().build());

    // By changing the speed back and forth we force the builder to create a new instance
    var other = subject.copyOf().withSpeed(10.0).build();
    var copy = other.copyOf().withSpeed(SPEED).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testEqualsAndHashCodeWithNewlyConstructedPreferences() {
    // Test that nothing breaks while adding wrapper objects for values
    var sameSpeed = 2.3;
    var sameReluctance = 2.51;
    var sameStairsReluctance = 3.2;
    var sameSafetyFactor = 0.5;
    var sameEscalatorReluctance = 2.45;
    var sameBoardCost = 60;
    var firstEqual = WalkPreferences.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .withBoardCost(sameBoardCost)
      .build();
    var secondEqual = WalkPreferences.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .withBoardCost(sameBoardCost)
      .build();
    assertEqualsAndHashCode(firstEqual, secondEqual);

    // Test that changing speed means preferences are not equal
    var notSameSpeed = sameSpeed + 1;
    var differentSpeedPreferences = WalkPreferences.of()
      .withSpeed(notSameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .withBoardCost(sameBoardCost)
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentSpeedPreferences);

    // Test that changing reluctance means preferences are not equal
    var notSameReluctance = sameReluctance + 1;
    var differentReluctancePreferences = WalkPreferences.of()
      .withSpeed(sameSpeed)
      .withReluctance(notSameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .withBoardCost(sameBoardCost)
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentReluctancePreferences);

    // Test that changing stairs reluctance means preferences are not equal
    var notSameStairsReluctance = sameStairsReluctance + 1;
    var differentStairsReluctancePreferences = WalkPreferences.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(notSameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .withBoardCost(sameBoardCost)
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentStairsReluctancePreferences);

    // Test that changing safety factor means preferences are not equal
    var notSameSafetyFactor = sameSafetyFactor + 0.1;
    var differentSafetyFactorPreferences = WalkPreferences.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(notSameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .withBoardCost(sameBoardCost)
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentSafetyFactorPreferences);

    // Test that changing escalator reluctance means preferences are not equal
    var notSameEscalatorReluctance = sameEscalatorReluctance + 1;
    var differentEscalatorReluctancePreferences = WalkPreferences.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(notSameEscalatorReluctance))
      .withBoardCost(sameBoardCost)
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentEscalatorReluctancePreferences);

    // Test that changing board cost means preferences are not equal
    var notSameBoardCost = sameBoardCost + 1;
    var differentBoardCostPreferences = WalkPreferences.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .withBoardCost(notSameBoardCost)
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentBoardCostPreferences);
  }

  @Test
  void testToString() {
    assertEquals("WalkPreferences{}", WalkPreferences.DEFAULT.toString());
    assertEquals(
      "WalkPreferences{speed: 1.71, reluctance: 2.5, boardCost: $301, stairsReluctance: 3.0, stairsTimeFactor: 1.31, safetyFactor: 0.51}",
      subject.toString()
    );
  }
}
