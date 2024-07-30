package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.api.model.RaptorConstants;

class ViaConnectionTest {

  private static final int STOP_A = 12;
  private static final int STOP_B = 13;
  private static final int STOP_C = 14;
  private static final Duration MIN_DURATION = Duration.ofMinutes(3);
  private static final int MIN_DURATION_SEC = (int) MIN_DURATION.toSeconds();
  private static final int C1 = 200;

  @Test
  void passThroughStop() {
    var subject = ViaConnection.passThroughStop(STOP_C);
    assertEquals(STOP_C, subject.fromStop());
    assertEquals(STOP_C, subject.toStop());
    assertTrue(subject.allowPassThrough());
    assertTrue(subject.isSameStop());
    Assertions.assertEquals(RaptorConstants.ZERO, subject.durationInSeconds());
    assertEquals(RaptorConstants.ZERO, subject.c1());
  }

  @Test
  void viaSingleStop() {
    var subject = ViaConnection.stop(STOP_C, MIN_DURATION);
    assertEquals(STOP_C, subject.fromStop());
    assertEquals(STOP_C, subject.toStop());
    assertFalse(subject.allowPassThrough());
    assertTrue(subject.isSameStop());
    assertEquals(MIN_DURATION_SEC, subject.durationInSeconds());
    assertEquals(RaptorConstants.ZERO, subject.c1());
  }

  @Test
  void viaCoordinateOrTransfer() {
    var subject = ViaConnection.stop(STOP_A, STOP_B, MIN_DURATION, C1);
    assertEquals(STOP_A, subject.fromStop());
    assertEquals(STOP_B, subject.toStop());
    assertFalse(subject.allowPassThrough());
    assertFalse(subject.isSameStop());
    assertEquals(MIN_DURATION_SEC, subject.durationInSeconds());
    assertEquals(C1, subject.c1());
  }

  static List<Arguments> isBetterThanTestCases() {
    // Subject is: STOP_A, STOP_B, MIN_DURATION, C1
    return List.of(
      Arguments.of(STOP_A, STOP_B, MIN_DURATION_SEC, C1, true, "Same"),
      Arguments.of(STOP_C, STOP_B, MIN_DURATION_SEC, C1, false, "toStop differ"),
      Arguments.of(STOP_A, STOP_C, MIN_DURATION_SEC, C1, false, "fromStop differ"),
      Arguments.of(STOP_A, STOP_B, MIN_DURATION_SEC + 1, C1, true, "Wait time is better"),
      Arguments.of(STOP_A, STOP_B, MIN_DURATION_SEC - 1, C1, false, "Wait time is worse"),
      Arguments.of(STOP_A, STOP_B, MIN_DURATION_SEC, C1 + 1, true, "C1 is better"),
      Arguments.of(STOP_A, STOP_B, MIN_DURATION_SEC, C1 - 1, false, "C1 is worse")
    );
  }

  @ParameterizedTest
  @MethodSource("isBetterThanTestCases")
  void isBetterThan(
    int fromStop,
    int toStop,
    int minWaitTime,
    int c1,
    boolean expected,
    String description
  ) {
    var subject = ViaConnection.stop(STOP_A, STOP_B, MIN_DURATION, C1);
    var candidate = ViaConnection.stop(fromStop, toStop, Duration.ofSeconds(minWaitTime), c1);
    assertEquals(subject.isBetterThan(candidate), expected, description);
  }

  @Test
  void testEqualsAndHashCode() {
    var subject = ViaConnection.stop(STOP_A, STOP_B, MIN_DURATION, C1);
    var same = ViaConnection.stop(STOP_A, STOP_B, MIN_DURATION, C1);
    // Slightly less wait-time and slightly larger cost(c1)
    var other = ViaConnection.stop(
      STOP_A,
      STOP_B,
      MIN_DURATION.minus(Duration.ofSeconds(1)),
      C1 + 1
    );

    assertEquals(subject, same);
    assertNotEquals(subject, other);
    assertNotEquals(subject, "Does not match another type");

    assertEquals(subject.hashCode(), same.hashCode());
    assertNotEquals(subject.hashCode(), other.hashCode());
  }

  @Test
  void testToString() {
    var viaStopAB = ViaConnection.stop(STOP_A, STOP_B, MIN_DURATION, C1);
    var viaStopB = ViaConnection.stop(STOP_B, MIN_DURATION);
    var passThroughC = ViaConnection.passThroughStop(STOP_C);

    assertEquals("Via(3m 12~13)", viaStopAB.toString());
    assertEquals("Via(3m A~B)", viaStopAB.toString(ViaConnectionTest::stopName));
    assertEquals("Via(3m 13)", viaStopB.toString());
    assertEquals("Via(3m B)", viaStopB.toString(ViaConnectionTest::stopName));
    assertEquals("PassThrough(14)", passThroughC.toString());
    assertEquals("PassThrough(C)", passThroughC.toString(ViaConnectionTest::stopName));
  }

  private static String stopName(int i) {
    return switch (i) {
      case 12 -> "A";
      case 13 -> "B";
      case 14 -> "C";
      default -> throw new IllegalArgumentException("Unknown stop: " + i);
    };
  }
}
