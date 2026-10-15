package org.opentripplanner.raptor.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransfer;

class RaptorViaLocationTest implements RaptorTestConstants {

  private static final Duration MINIMUM_WAIT_TIME = Duration.ofSeconds(23);
  private static final String VIA_LABEL = "Via";
  private static final String PASS_THROUGH_LABEL = "PassThrough";
  private static final int TX_C1 = 3000;
  private static final int TX_DURATION = D30_s;
  private static final TestTransfer TRANSFER = TestTransfer.transfer(STOP_C, TX_DURATION, TX_C1);

  private final RaptorViaLocation subject = RaptorViaLocation.viaVisit(VIA_LABEL, MINIMUM_WAIT_TIME)
    .addTransfer(STOP_B, TRANSFER)
    .addStop(STOP_A)
    .build();

  private final RaptorViaLocation subjectPassThrough = RaptorViaLocation.passThrough(
    PASS_THROUGH_LABEL
  )
    .addStop(STOP_D)
    .build();

  private final RaptorTransferViaConnection transferConnection = subject
    .connections()
    .stream()
    .filter(it -> it instanceof RaptorTransferViaConnection)
    .findFirst()
    .map(it -> (RaptorTransferViaConnection) it)
    .orElseThrow();

  private final RaptorVisitStopViaConnection stopConnection = subject
    .connections()
    .stream()
    .filter(it -> it instanceof RaptorVisitStopViaConnection)
    .findFirst()
    .map(it -> (RaptorVisitStopViaConnection) it)
    .orElseThrow();

  @Test
  void connections() {
    var connections = subject.connections();
    assertTrue(connections.contains(transferConnection), connections.toString());
    assertTrue(connections.contains(stopConnection), connections.toString());
  }

  @Test
  void atLeastOneConnectionMustExist() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      RaptorViaLocation.viaVisit(VIA_LABEL, MINIMUM_WAIT_TIME).build()
    );
    assertEquals("At least one connection must exist!", ex.getMessage());
  }

  @Test
  void validateDuplicateConnections() {
    var passThroughLocation = RaptorViaLocation.passThrough("subject")
      .addStop(STOP_A, STOP_B, STOP_A)
      .build();

    var ex1 = assertThrows(IllegalArgumentException.class, () ->
      passThroughLocation.validateDuplicateConnections()
    );
    assertEquals("All connection need to be pareto-optimal: (stop 1) ≡ (stop 1)", ex1.getMessage());

    var viaVisitLocation = RaptorViaLocation.viaVisit("subject", MINIMUM_WAIT_TIME)
      .addTransfer(STOP_A, TestTransfer.transfer(STOP_B, 30))
      .addTransfer(STOP_A, TestTransfer.transfer(STOP_B, 20))
      .addStop(STOP_A)
      .build();

    var ex2 = assertThrows(IllegalArgumentException.class, () ->
      viaVisitLocation.validateDuplicateConnections()
    );
    assertEquals(
      "All connection need to be pareto-optimal: (transfer 1 ~ 2 [53s C₁60]) ≻ (transfer 1 ~ 2 [43s C₁40])",
      ex2.getMessage()
    );
  }

  @Test
  void testToString() {
    assertEquals(
      "RaptorViaLocation{via-visit Via : [(transfer 2 ~ 3 [53s C₁30]), (stop 1 [23s])]}",
      subject.toString()
    );
    assertEquals(
      "RaptorViaLocation{pass-through PassThrough : [(stop 4)]}",
      subjectPassThrough.toString()
    );

    assertEquals(
      "RaptorViaLocation{via-visit Via : [(transfer B ~ C [53s C₁30]), (stop A [23s])]}",
      subject.toString(RaptorTestConstants::stopIndexToName)
    );
  }

  @Test
  void label() {
    assertEquals(VIA_LABEL, subject.label());
    assertEquals(PASS_THROUGH_LABEL, subjectPassThrough.label());
  }

  @Test
  void testMinimumWaitTimePropagateFromLocationToConnection() {
    assertEquals(MINIMUM_WAIT_TIME.toSeconds(), stopConnection.minimumWaitTime());
  }

  @Test
  void asBitSet() {
    var subject = RaptorViaLocation.passThrough(VIA_LABEL)
      .addStop(2)
      .addStop(7)
      .addStop(13)
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
  void testEquals() {
    var ex = assertThrows(UnsupportedOperationException.class, () ->
      subject.equals("<Any object>")
    );
    assertEquals(
      "No need to compare class org.opentripplanner.raptor.api.request.via.RaptorViaLocation",
      ex.getMessage()
    );
    ex = assertThrows(UnsupportedOperationException.class, () ->
      subjectPassThrough.equals("<Any object>")
    );
    assertEquals(
      "No need to compare class org.opentripplanner.raptor.api.request.via.RaptorViaLocation",
      ex.getMessage()
    );
  }

  @Test
  void testHashCode() {
    var ex = assertThrows(UnsupportedOperationException.class, () -> subject.hashCode());
    assertEquals(
      "No need for hashCode of class org.opentripplanner.raptor.api.request.via.RaptorViaLocation",
      ex.getMessage()
    );
    ex = assertThrows(UnsupportedOperationException.class, () -> subjectPassThrough.hashCode());
    assertEquals(
      "No need for hashCode of class org.opentripplanner.raptor.api.request.via.RaptorViaLocation",
      ex.getMessage()
    );
  }

  @Test
  void testMixingConnectionEqualsAndHashCode() {
    var viaTxConnection = RaptorViaLocation.viaVisit("SameAsVia", MINIMUM_WAIT_TIME)
      .addTransfer(STOP_B, TRANSFER)
      .build();
    var viaStopConnections = RaptorViaLocation.viaVisit("SameAsVia", MINIMUM_WAIT_TIME)
      .addStop(STOP_A)
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
