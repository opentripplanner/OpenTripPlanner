package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransfer;

class ViaLocationTest {

  private static final int STOP_A = 12;
  private static final int STOP_B = 13;
  private static final int STOP_C = 14;
  private static final Duration WAIT_TIME = Duration.ofMinutes(3);
  private static final int WAIT_TIME_SEC = (int) WAIT_TIME.toSeconds();
  private static final int C1 = 200;
  private static final int TX_DURATION = 35;
  private static final RaptorTransfer TX = new TestTransfer(STOP_B, TX_DURATION, C1);

  @Test
  void passThroughStop() {
    var subject = RaptorViaLocation.allowPassThrough("PassThrough A").addViaStop(STOP_C).build();

    assertEquals("PassThrough A", subject.label());
    assertTrue(subject.allowPassThrough());
    assertEquals(RaptorConstants.ZERO, subject.minimumWaitTime());
    assertEquals(
      "Via{label: PassThrough A, allowPassThrough, connections: [C]}",
      subject.toString(ViaLocationTest::stopName)
    );
    assertEquals(
      "Via{label: PassThrough A, allowPassThrough, connections: [14]}",
      subject.toString()
    );

    assertEquals(1, subject.connections().size());

    var c = subject.connections().getFirst();
    assertEquals(STOP_C, c.fromStop());
    assertEquals(STOP_C, c.toStop());
    assertTrue(c.isSameStop());
    Assertions.assertEquals(RaptorConstants.ZERO, c.durationInSeconds());
    assertEquals(RaptorConstants.ZERO, c.c1());
  }

  @Test
  void viaSingleStop() {
    var subject = RaptorViaLocation.via("Tx A").addViaStop(STOP_B).build();

    assertEquals("Tx A", subject.label());
    assertFalse(subject.allowPassThrough());
    assertEquals(RaptorConstants.ZERO, subject.minimumWaitTime());
    assertEquals("Via{label: Tx A, connections: [B]}", subject.toString(ViaLocationTest::stopName));
    assertEquals("Via{label: Tx A, connections: [13]}", subject.toString());
    assertEquals(1, subject.connections().size());

    var connection = subject.connections().getFirst();
    assertEquals(STOP_B, connection.fromStop());
    assertEquals(STOP_B, connection.toStop());
    assertTrue(connection.isSameStop());
    Assertions.assertEquals(RaptorConstants.ZERO, connection.durationInSeconds());
    assertEquals(RaptorConstants.ZERO, connection.c1());
  }

  @Test
  void testCombinationOfPassThroughAndTransfer() {
    var subject = RaptorViaLocation
      .allowPassThrough("PassThrough A")
      .addViaStop(STOP_C)
      .addViaTransfer(STOP_A, TX)
      .build();

    assertEquals("PassThrough A", subject.label());
    assertTrue(subject.allowPassThrough());
    assertEquals(RaptorConstants.ZERO, subject.minimumWaitTime());
    assertEquals(
      "Via{label: PassThrough A, allowPassThrough, connections: [C, A~B 35s]}",
      subject.toString(ViaLocationTest::stopName)
    );
    assertEquals(2, subject.connections().size());

    var c = subject.connections().getFirst();
    assertEquals(STOP_C, c.fromStop());
    assertEquals(STOP_C, c.toStop());
    assertTrue(c.isSameStop());
    Assertions.assertEquals(RaptorConstants.ZERO, c.durationInSeconds());
    assertEquals(RaptorConstants.ZERO, c.c1());

    c = subject.connections().getLast();
    assertEquals(STOP_A, c.fromStop());
    assertEquals(STOP_B, c.toStop());
    assertFalse(c.isSameStop());
    Assertions.assertEquals(TX_DURATION, c.durationInSeconds());
    assertEquals(C1, c.c1());
  }

  @Test
  void viaStopAorCWithWaitTime() {
    var subject = RaptorViaLocation
      .via("Plaza", WAIT_TIME)
      .addViaStop(STOP_C)
      .addViaTransfer(STOP_A, TX)
      .build();

    assertEquals("Plaza", subject.label());
    assertFalse(subject.allowPassThrough());
    assertEquals(WAIT_TIME_SEC, subject.minimumWaitTime());
    assertEquals(
      "Via{label: Plaza, minWaitTime: 3m, connections: [C 3m, A~B 3m35s]}",
      subject.toString(ViaLocationTest::stopName)
    );
    assertEquals(2, subject.connections().size());

    var connection = subject.connections().getFirst();
    assertEquals(STOP_C, connection.fromStop());
    assertEquals(STOP_C, connection.toStop());
    assertTrue(connection.isSameStop());
    Assertions.assertEquals(WAIT_TIME_SEC, connection.durationInSeconds());
    assertEquals(RaptorConstants.ZERO, connection.c1());

    connection = subject.connections().getLast();
    assertEquals(STOP_A, connection.fromStop());
    assertEquals(STOP_B, connection.toStop());
    assertFalse(connection.isSameStop());
    Assertions.assertEquals(WAIT_TIME_SEC + TX.durationInSeconds(), connection.durationInSeconds());
    assertEquals(C1, connection.c1());
  }

  static List<Arguments> isBetterThanTestCases() {
    // Subject is: STOP_A, STOP_B, MIN_DURATION, C1
    return List.of(
      Arguments.of(STOP_A, STOP_B, TX_DURATION, C1, true, "Same"),
      Arguments.of(STOP_C, STOP_B, TX_DURATION, C1, false, "toStop differ"),
      Arguments.of(STOP_A, STOP_C, TX_DURATION, C1, false, "fromStop differ"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION + 1, C1, true, "Wait time is better"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION - 1, C1, false, "Wait time is worse"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION, C1 + 1, true, "C1 is better"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION, C1 - 1, false, "C1 is worse")
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
    var subject = RaptorViaLocation
      .via("Subject")
      .addViaTransfer(STOP_A, new TestTransfer(STOP_B, TX_DURATION, C1))
      .build()
      .connections()
      .getFirst();

    var candidate = RaptorViaLocation
      .via("Candidate")
      .addViaTransfer(fromStop, new TestTransfer(toStop, minWaitTime, c1))
      .build()
      .connections()
      .getFirst();

    assertEquals(subject.isBetterOrEqual(candidate), expected, description);
  }

  @Test
  void throwsExceptionIfConnectionsIsNotParetoOptimal() {
    var e = assertThrows(
      IllegalArgumentException.class,
      () ->
        RaptorViaLocation
          .via("S")
          .addViaTransfer(STOP_A, new TestTransfer(STOP_B, TX_DURATION, C1))
          .addViaTransfer(STOP_A, new TestTransfer(STOP_B, TX_DURATION, C1))
          .build()
    );
    assertEquals(
      "All connection need to be pareto-optimal: (12~13 35s) <-> (12~13 35s)",
      e.getMessage()
    );
  }

  @Test
  void testEqualsAndHashCode() {
    var subject = RaptorViaLocation.via(null).addViaTransfer(STOP_A, TX).build();
    var same = RaptorViaLocation.via(null).addViaTransfer(STOP_A, TX).build();
    // Slightly less wait-time and slightly larger cost(c1)
    var other = RaptorViaLocation
      .via(null, Duration.ofSeconds(1))
      .addViaTransfer(STOP_A, TX)
      .build();

    assertEquals(subject, same);
    assertNotEquals(subject, other);
    assertNotEquals(subject, "Does not match another type");

    assertEquals(subject.hashCode(), same.hashCode());
    assertNotEquals(subject.hashCode(), other.hashCode());
  }

  @Test
  void testToString() {
    var subject = RaptorViaLocation.via("A|B").addViaStop(STOP_A).addViaStop(STOP_B).build();
    assertEquals("Via{label: A|B, connections: [12, 13]}", subject.toString());
    assertEquals(
      "Via{label: A|B, connections: [A, B]}",
      subject.toString(ViaLocationTest::stopName)
    );

    subject = RaptorViaLocation.via(null, WAIT_TIME).addViaStop(STOP_B).build();
    assertEquals("Via{minWaitTime: 3m, connections: [13 3m]}", subject.toString());
    assertEquals(
      "Via{minWaitTime: 3m, connections: [B 3m]}",
      subject.toString(ViaLocationTest::stopName)
    );

    subject = RaptorViaLocation.via(null).addViaTransfer(STOP_A, TX).build();
    assertEquals("Via{connections: [12~13 35s]}", subject.toString());
    assertEquals("Via{connections: [A~B 35s]}", subject.toString(ViaLocationTest::stopName));

    subject = RaptorViaLocation.allowPassThrough(null).addViaStop(STOP_C).build();
    assertEquals("Via{allowPassThrough, connections: [14]}", subject.toString());
    assertEquals(
      "Via{allowPassThrough, connections: [C]}",
      subject.toString(ViaLocationTest::stopName)
    );
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
