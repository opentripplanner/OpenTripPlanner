package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;

class EmptyBoardOrAlightEventTest {

  @Test
  void testEmptyBoardOrAlightEvent() {
    var subject = new EmptyBoardOrAlightEvent<>(300);

    assertTrue(subject.empty());
    assertEquals(300, subject.earliestBoardTime());
    assertEquals(RaptorTransferConstraint.REGULAR_TRANSFER, subject.transferConstraint());
    assertEquals("EmptyBoardOrAlightEvent(00:05:00)", subject.toString());
  }
}
