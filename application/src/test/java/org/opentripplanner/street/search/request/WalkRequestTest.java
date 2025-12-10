package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertEqualsAndHashCode;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertNotEqualsAndHashCode;

import org.junit.jupiter.api.Test;

class WalkRequestTest {

  private static final double SPEED = 1.7111;
  private static final double EXPECTED_SPEED = 1.71;
  private static final double RELUCTANCE = 2.51;
  private static final double EXPECTED_RELUCTANCE = 2.5;
  private static final double STAIRS_RELUCTANCE = 3.011;
  private static final double EXPECTED_STAIRS_RELUCTANCE = 3.0;
  private static final double STAIRS_TIME_FACTOR = 1.3111;
  private static final double EXPECTED_STAIRS_TIME_FACTOR = 1.31;
  private static final double SAFETY_FACTOR = 0.5111;
  private static final double EXPECTED_SAFETY_FACTOR = 0.51;

  private final WalkRequest subject = WalkRequest.of()
    .withSpeed(SPEED)
    .withReluctance(RELUCTANCE)
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
  void testEqualsAndHashCodeWithCopiedRequest() {
    // Return same object if no value is set
    assertSame(subject, subject.copyOf().build());

    // By changing the speed back and forth we force the builder to create a new instance
    var other = subject.copyOf().withSpeed(10.0).build();
    var copy = other.copyOf().withSpeed(SPEED).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testEqualsAndHashCodeWithNewlyConstructedRequest() {
    // Test that nothing breaks while adding wrapper objects for values
    var sameSpeed = 2.3;
    var sameReluctance = 2.51;
    var sameStairsReluctance = 3.2;
    var sameSafetyFactor = 0.5;
    var sameEscalatorReluctance = 2.45;
    var firstEqual = WalkRequest.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .build();
    var secondEqual = WalkRequest.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .build();
    assertEqualsAndHashCode(firstEqual, secondEqual);

    // Test that changing speed means Request are not equal
    var notSameSpeed = sameSpeed + 1;
    var differentSpeedRequest = WalkRequest.of()
      .withSpeed(notSameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentSpeedRequest);

    // Test that changing reluctance means Request are not equal
    var notSameReluctance = sameReluctance + 1;
    var differentReluctanceRequest = WalkRequest.of()
      .withSpeed(sameSpeed)
      .withReluctance(notSameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentReluctanceRequest);

    // Test that changing stairs reluctance means Request are not equal
    var notSameStairsReluctance = sameStairsReluctance + 1;
    var differentStairsReluctanceRequest = WalkRequest.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(notSameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentStairsReluctanceRequest);

    // Test that changing safety factor means Request are not equal
    var notSameSafetyFactor = sameSafetyFactor + 0.1;
    var differentSafetyFactorRequest = WalkRequest.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(notSameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(sameEscalatorReluctance))
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentSafetyFactorRequest);

    // Test that changing escalator reluctance means Request are not equal
    var notSameEscalatorReluctance = sameEscalatorReluctance + 1;
    var differentEscalatorReluctanceRequest = WalkRequest.of()
      .withSpeed(sameSpeed)
      .withReluctance(sameReluctance)
      .withStairsReluctance(sameStairsReluctance)
      .withSafetyFactor(sameSafetyFactor)
      .withEscalator(escalator -> escalator.withReluctance(notSameEscalatorReluctance))
      .build();
    assertNotEqualsAndHashCode(firstEqual, differentEscalatorReluctanceRequest);
  }

  @Test
  void testToString() {
    assertEquals("WalkRequest{}", WalkRequest.DEFAULT.toString());
    assertEquals(
      "WalkRequest{speed: 1.71, reluctance: 2.5, stairsReluctance: 3.0, stairsTimeFactor: 1.31, safetyFactor: 0.51}",
      subject.toString()
    );
  }
}
