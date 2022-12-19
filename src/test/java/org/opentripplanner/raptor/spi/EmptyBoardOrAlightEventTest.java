package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmptyBoardOrAlightEventTest {

  @Test
  void testEmptyBoardOrAlightEvent() {
    var subject = new EmptyBoardOrAlightEvent<>(300);

    assertTrue(subject.empty());
    assertEquals(300, subject.getEarliestBoardTime());
    assertEquals(RaptorTransferConstraint.REGULAR_TRANSFER, subject.getTransferConstraint());
    assertEquals("EmptyBoardOrAlightEvent[earliestBoardTime=300]", subject.toString());
  }
}
