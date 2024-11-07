package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnknownPathTest {

  @Test
  void forwardDirectPath() {
    var subject = new UnknownPath<>(300, 1200, 3);

    assertEquals(300, subject.startTime());
    assertEquals(1200, subject.endTime());
    assertEquals(3, subject.numberOfTransfers());
    assertEquals(900, subject.durationInSeconds());
    assertEquals("[0:05 0:20 15m Tₓ3]", subject.toString());
  }

  @Test
  void reversDirectPath() {
    var subject = new UnknownPath<>(1200, 300, 2);

    assertEquals(300, subject.startTime());
    assertEquals(1200, subject.endTime());
    assertEquals(2, subject.numberOfTransfers());
    assertEquals(900, subject.durationInSeconds());
    assertEquals("[0:05 0:20 15m Tₓ2]", subject.toString());
  }
}
