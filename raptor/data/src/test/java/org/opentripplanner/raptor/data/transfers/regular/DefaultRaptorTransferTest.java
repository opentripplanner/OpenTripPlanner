package org.opentripplanner.raptor.data.transfers.regular;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class DefaultRaptorTransferTest {

  private final DefaultRaptorTransfer subject = new DefaultRaptorTransfer(7, 12_345, 90);

  @Test
  void materializeReturnsTheImmutableTransferItself() {
    assertSame(subject, subject.materialize());
  }

  @Test
  void testToString() {
    assertEquals("Transfer 7 ~ [1m30s, $123.45]", subject.toString());
  }
}
