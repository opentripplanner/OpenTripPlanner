package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor.api.model.RaptorConstants;

class RaptorViaLocationTest implements RaptorTestConstants {

  private static final Duration MINIMUM_WAIT_TIME = Duration.ofSeconds(23);
  private static final String VIA_LABEL = "Via";
  private static final String PASS_THROUGH_LABEL = "PassThrough";
  private static final int TX_C1 = 3000;
  private static final int TX_DURATION = D30s;
  private static final TestTransfer TRANSFER = TestTransfer.transfer(STOP_C, TX_DURATION, TX_C1);

  private final RaptorViaLocation subject = RaptorViaLocation.via(VIA_LABEL, MINIMUM_WAIT_TIME)
    .addViaTransfer(STOP_B, TRANSFER)
    .addViaStop(STOP_A)
    .build();

  private final RaptorViaLocation subjectPassThrough = RaptorViaLocation.passThrough(
    PASS_THROUGH_LABEL
  )
    .addPassThroughStop(STOP_D)
    .build();

  private final RaptorViaConnection transferConnection = subject
    .connections()
    .stream()
    .filter(it -> it.isTransfer())
    .findFirst()
    .orElseThrow();

  private final RaptorViaConnection stopConnection = subject
    .connections()
    .stream()
    .filter(it -> it.isSameStop())
    .findFirst()
    .orElseThrow();

  private final RaptorViaConnection passThroughStopConnection = subjectPassThrough
    .connections()
    .stream()
    .findFirst()
    .orElseThrow();

  @Test
  void passThrough() {
    assertFalse(subject.isPassThroughSearch());
    assertTrue(subjectPassThrough.isPassThroughSearch());
  }

  @Test
  void connections() {
    var connections = subject.connections();
    assertTrue(connections.contains(transferConnection), connections.toString());
    assertTrue(connections.contains(stopConnection), connections.toString());
  }

  @Test
  void testToString() {
    assertEquals(
      "RaptorViaLocation{via Via wait 23s : [(stop 2 ~ 3, 53s), (stop 1, 23s)]}",
      subject.toString()
    );
    assertEquals(
      "RaptorViaLocation{pass-through PassThrough : [(stop 4)]}",
      subjectPassThrough.toString()
    );

    assertEquals(
      "RaptorViaLocation{via Via wait 23s : [(stop B ~ C, 53s), (stop A, 23s)]}",
      subject.toString(RaptorTestConstants::stopIndexToName)
    );
  }

  @Test
  void minimumWaitTime() {
    assertEquals(MINIMUM_WAIT_TIME.toSeconds(), subject.minimumWaitTime());
    assertEquals(Duration.ZERO.toSeconds(), subjectPassThrough.minimumWaitTime());
  }

  @Test
  void label() {
    assertEquals(VIA_LABEL, subject.label());
    assertEquals(PASS_THROUGH_LABEL, subjectPassThrough.label());
  }

  @Test
  void fromStop() {
    assertEquals(STOP_A, stopConnection.fromStop());
    assertEquals(STOP_B, transferConnection.fromStop());
    assertEquals(STOP_D, passThroughStopConnection.fromStop());
  }

  @Test
  void transfer() {
    assertNull(stopConnection.transfer());
    assertNull(passThroughStopConnection.transfer());
    assertEquals(TRANSFER, transferConnection.transfer());
  }

  @Test
  void toStop() {
    assertEquals(STOP_A, stopConnection.toStop());
    assertEquals(STOP_D, passThroughStopConnection.toStop());
    assertEquals(STOP_C, transferConnection.toStop());
  }

  @Test
  void durationInSeconds() {
    assertEquals(MINIMUM_WAIT_TIME.toSeconds(), stopConnection.durationInSeconds());
    assertEquals(
      MINIMUM_WAIT_TIME.plusSeconds(TRANSFER.durationInSeconds()).toSeconds(),
      transferConnection.durationInSeconds()
    );
  }

  @Test
  void c1() {
    assertEquals(RaptorConstants.ZERO, stopConnection.c1());
    assertEquals(TX_C1, transferConnection.c1());
  }

  @Test
  void isSameStop() {
    assertTrue(stopConnection.isSameStop());
    assertFalse(transferConnection.isSameStop());
  }

  @Test
  void isTransfer() {
    assertFalse(stopConnection.isTransfer());
    assertTrue(transferConnection.isTransfer());
  }

  static List<Arguments> isBetterThanTestCases() {
    // Subject is: STOP_A, STOP_B, MIN_DURATION, C1
    return List.of(
      Arguments.of(STOP_A, STOP_B, TX_DURATION, TX_C1, true, "Same"),
      Arguments.of(STOP_C, STOP_B, TX_DURATION, TX_C1, false, "toStop differ"),
      Arguments.of(STOP_A, STOP_C, TX_DURATION, TX_C1, false, "fromStop differ"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION + 1, TX_C1, true, "Wait time is better"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION - 1, TX_C1, false, "Wait time is worse"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION, TX_C1 + 1, true, "C1 is better"),
      Arguments.of(STOP_A, STOP_B, TX_DURATION, TX_C1 - 1, false, "C1 is worse")
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
    var subject = RaptorViaLocation.via("Subject")
      .addViaTransfer(STOP_A, new TestTransfer(STOP_B, TX_DURATION, TX_C1))
      .build()
      .connections()
      .getFirst();

    var candidate = RaptorViaLocation.via("Candidate")
      .addViaTransfer(fromStop, new TestTransfer(toStop, minWaitTime, c1))
      .build()
      .connections()
      .getFirst();

    assertEquals(subject.isBetterOrEqual(candidate), expected, description);
  }

  @Test
  void asBitSet() {
    var subject = RaptorViaLocation.passThrough(VIA_LABEL)
      .addPassThroughStop(2)
      .addPassThroughStop(7)
      .addPassThroughStop(13)
      .build();

    var bitSet = subject.asBitSet();

    // Sample some all set values as well as some not set values
    assertFalse(bitSet.get(0));
    assertTrue(bitSet.get(2));
    assertFalse(bitSet.get(3));
    assertFalse(bitSet.get(6));
    assertTrue(bitSet.get(7));
    assertTrue(bitSet.get(13));
    assertFalse(bitSet.get(15000000));
  }

  @Test
  void testEqualsAndHAshCode() {
    var viaTxConnection = RaptorViaLocation.via("SameAsVia", MINIMUM_WAIT_TIME)
      .addViaTransfer(STOP_B, TRANSFER)
      .build();
    var viaStopConnections = RaptorViaLocation.via("SameAsVia", MINIMUM_WAIT_TIME)
      .addViaStop(STOP_A)
      .build();

    var sameTransferConnection = viaTxConnection.connections().get(0);
    var sameStopConnection = viaStopConnections.connections().get(0);

    // Equals
    assertEquals(sameTransferConnection, transferConnection);
    assertEquals(sameStopConnection, stopConnection);
    assertNotEquals(sameStopConnection, transferConnection);

    // Hash code
    assertEquals(sameTransferConnection.hashCode(), transferConnection.hashCode());
    assertEquals(sameStopConnection.hashCode(), stopConnection.hashCode());
    assertNotEquals(sameStopConnection.hashCode(), transferConnection.hashCode());
  }
}
